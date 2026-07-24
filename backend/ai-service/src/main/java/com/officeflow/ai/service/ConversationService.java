package com.officeflow.ai.service;

import com.officeflow.ai.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    private static final int MAX_HISTORY_TURNS = 10;

    public Map<String, Object> createConversation(String title) {
        return createConversation(title, null);
    }

    public Map<String, Object> createConversation(String title, Long userId) {
        UUID id = conversationRepository.create(
                title != null ? title : "新对话", userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id.toString());
        result.put("title", title != null ? title : "新对话");
        return result;
    }

    public List<Map<String, Object>> listConversations() {
        return conversationRepository.listAll();
    }

    public List<Map<String, Object>> listConversations(Long userId) {
        if (userId != null) {
            return conversationRepository.listByUserId(userId);
        }
        return conversationRepository.listAll();
    }

    public List<Map<String, Object>> getMessages(String conversationId) {
        return conversationRepository.getMessages(UUID.fromString(conversationId), 100);
    }

    public void deleteConversation(String conversationId) {
        conversationRepository.delete(UUID.fromString(conversationId));
    }

    public boolean deleteByUser(String conversationId, Long userId) {
        return conversationRepository.deleteByUser(UUID.fromString(conversationId), userId) > 0;
    }

    public void saveUserMessage(String conversationId, String content) {
        UUID id = UUID.fromString(conversationId);
        conversationRepository.addMessage(id, "user", content);
        conversationRepository.touch(id);

        int count = conversationRepository.getMessageCount(id);
        if (count == 1) {
            String title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
            conversationRepository.updateTitle(id, title);
        }
    }

    public void saveAiMessage(String conversationId, String content) {
        conversationRepository.addMessage(UUID.fromString(conversationId), "assistant", content);
    }

    /**
     * 获取最近 N 轮对话历史（按时间正序返回，方便拼入 prompt）
     */
    public List<Map<String, Object>> getHistoryForPrompt(String conversationId) {
        List<Map<String, Object>> recent = conversationRepository.getMessages(
                UUID.fromString(conversationId), MAX_HISTORY_TURNS * 2);
        Collections.reverse(recent);
        return recent;
    }
}
