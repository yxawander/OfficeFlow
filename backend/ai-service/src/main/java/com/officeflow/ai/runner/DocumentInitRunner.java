package com.officeflow.ai.runner;

import com.officeflow.ai.config.DocumentPathResolver;
import com.officeflow.ai.repository.VectorStoreRepository;
import com.officeflow.ai.service.PdfLoaderService;
import com.officeflow.ai.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentInitRunner implements ApplicationRunner {

    private final PdfLoaderService pdfLoaderService;
    private final RagService ragService;
    private final VectorStoreRepository vectorStoreRepository;
    private final DocumentPathResolver documentPathResolver;

    @Override
    public void run(ApplicationArguments args) {
        Path resolvedDocumentPath = documentPathResolver.resolveDirectory();
        log.info("=== Document auto-loading started ===");
        log.info("Scanning document directory: {}", resolvedDocumentPath);

        File dir = resolvedDocumentPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Document directory does not exist or is not a directory: {}", resolvedDocumentPath);
            return;
        }

        File[] docFiles = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".md");
        });
        if (docFiles == null || docFiles.length == 0) {
            log.info("No document files (pdf/txt/md) found in {}", resolvedDocumentPath);
            return;
        }

        log.info("Found {} document file(s) to process", docFiles.length);

        int loaded = 0;
        int skipped = 0;
        int updated = 0;

        for (File docFile : docFiles) {
            String source = docFile.getName();
            try {
                String currentHash = pdfLoaderService.computeFileHash(docFile);
                boolean alreadyLoaded = vectorStoreRepository.isSourceLoaded(source);

                if (alreadyLoaded) {
                    String storedHash = vectorStoreRepository.getSourceHash(source);
                    if (currentHash != null && currentHash.equals(storedHash)) {
                        log.info("[SKIP] {} - already loaded, file unchanged", source);
                        skipped++;
                        continue;
                    } else {
                        log.info("[UPDATE] {} - file has changed, re-ingesting...", source);
                        vectorStoreRepository.deleteBySource(source);
                    }
                }

                // Extract text based on file type
                String text;
                if (source.toLowerCase().endsWith(".pdf")) {
                    text = pdfLoaderService.extractText(docFile);
                } else {
                    text = Files.readString(docFile.toPath());
                }
                if (text == null || text.trim().isEmpty()) {
                    log.warn("[WARN] {} - no text extracted, skipping", source);
                    skipped++;
                    continue;
                }

                // Ingest into vector store
                Map<String, Object> result = ragService.ingestDocument(text, source);
                int chunks = (int) result.get("chunks");

                // Mark as loaded with hash
                vectorStoreRepository.markSourceLoaded(source, currentHash, chunks);

                if (alreadyLoaded) {
                    updated++;
                    log.info("[UPDATED] {} - re-ingested {} chunks", source, chunks);
                } else {
                    loaded++;
                    log.info("[LOADED] {} - ingested {} chunks", source, chunks);
                }

            } catch (Exception e) {
                log.error("[ERROR] Failed to process {}: {}", source, e.getMessage(), e);
            }
        }

        long totalDocs = vectorStoreRepository.count();
        log.info("=== Document loading complete: {} new, {} updated, {} skipped, total vectors: {} ===",
                loaded, updated, skipped, totalDocs);
    }
}
