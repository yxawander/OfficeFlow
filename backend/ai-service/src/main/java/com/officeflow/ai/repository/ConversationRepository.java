package com.officeflow.ai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public UUID create(String title, Long userId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ai_conversation (id, title, user_id) VALUES (?, ?, ?)",
                id, title, userId);
        return id;
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList(
                "SELECT id, title, created_at, updated_at FROM ai_conversation ORDER BY updated_at DESC");
    }

    public List<Map<String, Object>> listByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id, title, created_at, updated_at FROM ai_conversation WHERE user_id = ? ORDER BY updated_at DESC",
                userId);
    }

    public void updateTitle(UUID id, String title) {
        jdbcTemplate.update(
                "UPDATE ai_conversation SET title = ?, updated_at = NOW() WHERE id = ?",
                title, id);
    }

    public void touch(UUID id) {
        jdbcTemplate.update(
                "UPDATE ai_conversation SET updated_at = NOW() WHERE id = ?", id);
    }

    public void delete(UUID id) {
        jdbcTemplate.update("DELETE FROM ai_conversation WHERE id = ?", id);
    }

    public int deleteByUser(UUID id, Long userId) {
        return jdbcTemplate.update("DELETE FROM ai_conversation WHERE id = ? AND user_id = ?", id, userId);
    }

    public void addMessage(UUID conversationId, String role, String content) {
        jdbcTemplate.update(
                "INSERT INTO ai_conversation_message (conversation_id, role, content) VALUES (?, ?, ?)",
                conversationId, role, content);
    }

    public List<Map<String, Object>> getMessages(UUID conversationId, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT role, content FROM ai_conversation_message " +
                "WHERE conversation_id = ? ORDER BY created_at DESC LIMIT ?",
                conversationId, limit);
    }

    public int getMessageCount(UUID conversationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_conversation_message WHERE conversation_id = ?",
                Integer.class, conversationId);
        return count != null ? count : 0;
    }
}
