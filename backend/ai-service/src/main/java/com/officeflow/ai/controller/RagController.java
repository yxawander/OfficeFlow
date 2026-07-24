package com.officeflow.ai.controller;

import com.officeflow.ai.service.ConversationService;
import com.officeflow.ai.service.PdfLoaderService;
import com.officeflow.ai.service.RagService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final ConversationService conversationService;
    private final PdfLoaderService pdfLoaderService;

    @Value("${app.document.path:docs/pdf}")
    private String documentPath;

    private static final String HEADER_USER_ID = "X-Login-User-Id";
    private static final String HEADER_ROLES = "X-Login-Roles";

    /* ── 文件上传（仅 ADMIN） ── */

    /**
     * 上传文件到知识库（仅管理员可用）
     * 文件保存到 docs/pdf 目录，提取文本后向量化入库，重启时自动跳过未变更文件
     */
    @PostMapping("/upload-file")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) {
        requireAdmin(request);

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".pdf")
                && !filename.toLowerCase().endsWith(".txt"))) {
            return Map.of("status", "FAIL", "error", "仅支持 PDF 和 TXT 文件");
        }

        try {
            // 1. 确保 docs/pdf 目录存在（使用绝对路径）
            Path docDir = Paths.get(documentPath).toAbsolutePath();
            Files.createDirectories(docDir);

            // 2. 永久保存文件到 docs/pdf
            Path destPath = docDir.resolve(filename);
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", destPath);

            // 3. 计算文件哈希
            String currentHash = pdfLoaderService.computeFileHash(destPath.toFile());

            // 4. 提取文本
            String text;
            if (filename.toLowerCase().endsWith(".pdf")) {
                text = pdfLoaderService.extractText(destPath.toFile());
            } else {
                text = Files.readString(destPath);
            }

            if (text == null || text.trim().isEmpty()) {
                Files.deleteIfExists(destPath);
                return Map.of("status", "FAIL", "error", "文件中未提取到文本内容");
            }

            // 5. 向量化入库（先清除旧数据如果有）
            ragService.deleteDocument(filename);
            Map<String, Object> result = ragService.ingestDocument(text, filename);
            int chunks = (int) result.get("chunks");

            // 6. 登记到 loaded_documents 表
            ragService.markDocumentLoaded(filename, currentHash, chunks);

            result.put("status", "OK");
            result.put("filename", filename);
            result.put("savedTo", destPath.toAbsolutePath().toString());
            return result;
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return Map.of("status", "FAIL", "error", "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传纯文本到知识库（仅管理员可用）
     * POST /api/ai/rag/upload
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestBody Map<String, String> body,
                                       HttpServletRequest request) {
        requireAdmin(request);

        String content = body.get("content");
        String source = body.getOrDefault("source", "unknown");

        if (content == null || content.isBlank()) {
            return Map.of("status", "FAIL", "error", "content is required");
        }

        Map<String, Object> result = ragService.ingestDocument(content, source);
        result.put("status", "OK");
        return result;
    }

    /* ── RAG 问答 ── */

    @GetMapping("/query")
    public Map<String, Object> query(@RequestParam String question,
                                      @RequestParam(required = false) String conversationId) {
        if (question == null || question.isBlank()) {
            return Map.of("status", "FAIL", "error", "question is required");
        }
        return ragService.query(question, conversationId, conversationService);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDocuments", ragService.getDocumentCount());
        return result;
    }

    @DeleteMapping("/knowledge")
    public Map<String, Object> clear(HttpServletRequest request) {
        requireAdmin(request);
        ragService.clearKnowledge();
        return Map.of("status", "OK", "message", "knowledge base cleared");
    }

    /* ── 文档管理（仅 ADMIN） ── */

    /**
     * 列出知识库中已加载的文档
     */
    @GetMapping("/documents")
    public List<Map<String, Object>> documents(HttpServletRequest request) {
        requireAdmin(request);
        return ragService.listLoadedDocuments();
    }

    /**
     * 删除指定文档及其向量数据
     */
    @DeleteMapping("/documents/{source}")
    public Map<String, Object> deleteDocument(@PathVariable String source,
                                               HttpServletRequest request) {
        requireAdmin(request);
        ragService.deleteDocument(source);
        // 尝试删除 docs/pdf 中的物理文件
        try {
            Path filePath = Paths.get(documentPath).toAbsolutePath().resolve(source);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete physical file: {}", e.getMessage());
        }
        return Map.of("status", "OK", "message", "document deleted");
    }

    /* ── 对话管理（用户隔离） ── */

    @GetMapping("/conversations")
    public List<Map<String, Object>> conversations(HttpServletRequest request) {
        Long userId = getUserId(request);
        return conversationService.listConversations(userId);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<Map<String, Object>> messages(@PathVariable String id) {
        return conversationService.getMessages(id);
    }

    @PostMapping("/conversations")
    public Map<String, Object> createConversation(@RequestBody(required = false) Map<String, String> body,
                                                    HttpServletRequest request) {
        String title = body != null ? body.get("title") : null;
        Long userId = getUserId(request);
        return conversationService.createConversation(title, userId);
    }

    @DeleteMapping("/conversations/{id}")
    public Map<String, Object> deleteConversation(@PathVariable String id,
                                                    HttpServletRequest request) {
        Long userId = getUserId(request);
        boolean deleted = conversationService.deleteByUser(id, userId);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该对话");
        }
        return Map.of("status", "OK");
    }

    /* ── 工具方法 ── */

    private Long getUserId(HttpServletRequest request) {
        String value = request.getHeader(HEADER_USER_ID);
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void requireAdmin(HttpServletRequest request) {
        String roles = request.getHeader(HEADER_ROLES);
        if (roles == null || !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可执行此操作");
        }
    }
}
