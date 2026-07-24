package com.officeflow.ai.controller;

import com.officeflow.common.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final ChatModel chatModel;

    /**
     * 健康检查：验证 DashScope 连接
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "ai-service");
        result.put("chatModel", "OK - " + chatModel.getClass().getSimpleName());
        return result;
    }

    /**
     * 测试对话：向通义千问发送问题
     */
    @RateLimit(key = "ai:chat-test", maxRequests = 10, windowSeconds = 60,
            message = "AI 对话请求过于频繁，请稍后再试")
    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam(defaultValue = "你好，请用一句话介绍你自己") String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            ChatResponse response = chatModel.call(new Prompt(question));
            String answer = response.getResult().getOutput().getText();
            result.put("status", "OK");
            result.put("question", question);
            result.put("answer", answer);
            result.put("costMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            result.put("status", "FAIL");
            result.put("error", e.getMessage());
            result.put("costMs", System.currentTimeMillis() - start);
        }
        return result;
    }
}
