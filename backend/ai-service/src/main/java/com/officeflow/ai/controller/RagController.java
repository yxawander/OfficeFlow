package com.officeflow.ai.controller;

import com.officeflow.ai.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 上传文档到知识库
     * POST /api/ai/rag/upload
     * Body: { "content": "...", "source": "文件名或来源" }
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String source = body.getOrDefault("source", "unknown");

        if (content == null || content.isBlank()) {
            return Map.of("status", "FAIL", "error", "content is required");
        }

        Map<String, Object> result = ragService.ingestDocument(content, source);
        result.put("status", "OK");
        return result;
    }

    /**
     * RAG 智能问答
     * GET /api/ai/rag/query?question=...
     */
    @GetMapping("/query")
    public Map<String, Object> query(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            return Map.of("status", "FAIL", "error", "question is required");
        }
        return ragService.query(question);
    }

    /**
     * 知识库状态
     * GET /api/ai/rag/status
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDocuments", ragService.getDocumentCount());
        return result;
    }

    /**
     * 清空知识库
     * DELETE /api/ai/rag/knowledge
     */
    @DeleteMapping("/knowledge")
    public Map<String, Object> clear() {
        ragService.clearKnowledge();
        return Map.of("status", "OK", "message", "knowledge base cleared");
    }
}
