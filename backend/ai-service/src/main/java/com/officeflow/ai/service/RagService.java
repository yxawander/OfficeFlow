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
        return query(question, null, null);
    }

    /**
     * RAG 多轮问答：在基础 RAG 上叠加对话历史上下文
     */
    public Map<String, Object> query(String question, String conversationId,
                                      com.officeflow.ai.service.ConversationService conversationService) {
        long start = System.currentTimeMillis();

        // 1. 将问题向量化
        float[] queryEmbedding = embeddingModel.embed(question);

        // 2. 检索最相似的文档片段
        List<Map<String, Object>> relevantDocs = vectorStoreRepository.similaritySearch(queryEmbedding, TOP_K);

        // 3. 拼装 RAG 上下文
        StringBuilder context = new StringBuilder();
        List<String> sources = new ArrayList<>();
        for (Map<String, Object> doc : relevantDocs) {
            context.append(doc.get("content")).append("\n\n");
            double score = ((Number) doc.get("score")).doubleValue();
            sources.add(String.format("score=%.4f", score));
        }

        // 4. 构造带上下文和历史记录的 Prompt
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个智能问答助手。请根据以下提供的参考资料来回答用户的问题。\n");
        systemPrompt.append("如果参考资料中没有相关信息，请如实告知用户你不确定，不要编造答案。\n\n");
        systemPrompt.append("参考资料：\n").append(context);

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt.toString()));

        // 5. 注入对话历史
        if (conversationId != null && conversationService != null) {
            List<Map<String, Object>> history = conversationService.getHistoryForPrompt(conversationId);
            for (Map<String, Object> msg : history) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new org.springframework.ai.chat.messages.AssistantMessage(content));
                }
            }
        }

        messages.add(new UserMessage(question));

        ChatResponse response = chatModel.call(new Prompt(messages));
        String answer = response.getResult().getOutput().getText();

        // 6. 保存消息到对话
        if (conversationId != null && conversationService != null) {
            conversationService.saveUserMessage(conversationId, question);
            conversationService.saveAiMessage(conversationId, answer);
        }

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
     * 清空知识库（包括向量数据和加载记录）
     */
    public void clearKnowledge() {
        vectorStoreRepository.deleteAll();
        // 也清空 loaded_documents 表
        vectorStoreRepository.clearLoadedDocuments();
    }

    /**
     * 查询知识库状态
     */
    public long getDocumentCount() {
        return vectorStoreRepository.count();
    }

    /**
     * 列出已加载的文档
     */
    public List<Map<String, Object>> listLoadedDocuments() {
        return vectorStoreRepository.listLoadedDocuments();
    }

    /**
     * 删除指定来源的文档及其向量数据
     */
    public void deleteDocument(String source) {
        vectorStoreRepository.deleteBySource(source);
    }

    /**
     * 标记文档已加载到 loaded_documents 表
     */
    public void markDocumentLoaded(String source, String fileHash, int chunkCount) {
        vectorStoreRepository.markSourceLoaded(source, fileHash, chunkCount);
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
