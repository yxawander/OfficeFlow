package com.officeflow.ai.service;

import com.officeflow.ai.repository.VectorStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final VectorStoreRepository vectorStoreRepository;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int TOP_K = 3;

    /**
     * 上传文档：分块 → embedding → 存入 pgvector
     */
    public Map<String, Object> ingestDocument(String content, String source) {
        List<String> chunks = splitIntoChunks(content, CHUNK_SIZE, CHUNK_OVERLAP);
        log.info("Document split into {} chunks (source: {})", chunks.size(), source);

        List<Map<String, Object>> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingModel.embed(chunk);
            String metadata = String.format(
                    "{\"source\":\"%s\",\"chunk\":%d,\"total\":%d}", source, i, chunks.size()
            );
            Map<String, Object> doc = new HashMap<>();
            doc.put("content", chunk);
            doc.put("metadata", metadata);
            doc.put("embedding", embedding);
            documents.add(doc);
        }

        vectorStoreRepository.saveBatch(documents);
        long total = vectorStoreRepository.count();

        Map<String, Object> result = new HashMap<>();
        result.put("source", source);
        result.put("chunks", chunks.size());
        result.put("totalDocuments", total);
        return result;
    }

    /**
     * RAG 问答：检索相关上下文 → 拼装 prompt → 调用 LLM
     */
    public Map<String, Object> query(String question) {
        long start = System.currentTimeMillis();

        // 1. 将问题向量化
        float[] queryEmbedding = embeddingModel.embed(question);

        // 2. 检索最相似的文档片段
        List<Map<String, Object>> relevantDocs = vectorStoreRepository.similaritySearch(queryEmbedding, TOP_K);

        // 3. 拼装上下文
        StringBuilder context = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (Map<String, Object> doc : relevantDocs) {
            context.append(doc.get("content")).append("\n\n");
            double score = ((Number) doc.get("score")).doubleValue();
            sources.add(String.format("score=%.4f", score));
        }

        // 4. 构造带上下文的 Prompt
        String systemPrompt = "你是一个智能问答助手。请根据以下提供的参考资料来回答用户的问题。\n" +
                "如果参考资料中没有相关信息，请如实告知用户你不确定，不要编造答案。\n\n" +
                "参考资料：\n" + context.toString();

        ChatResponse response = chatModel.call(new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(question)
        )));

        String answer = response.getResult().getOutput().getText();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("question", question);
        result.put("answer", answer);
        result.put("relevantChunks", relevantDocs.size());
        result.put("sources", sources);
        result.put("costMs", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 清空知识库
     */
    public void clearKnowledge() {
        vectorStoreRepository.deleteAll();
    }

    /**
     * 查询知识库状态
     */
    public long getDocumentCount() {
        return vectorStoreRepository.count();
    }

    /**
     * 将文本按固定大小分块，带重叠
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
