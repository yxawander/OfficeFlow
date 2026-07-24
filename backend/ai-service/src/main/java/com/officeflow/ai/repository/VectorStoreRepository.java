package com.officeflow.ai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 保存文档及其向量到 pgvector
     */
    public UUID save(String content, String metadata, float[] embedding) {
        String vectorStr = toVectorString(embedding);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO vector_store (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector)",
                id, content, metadata, vectorStr
        );
        return id;
    }

    /**
     * 批量保存
     */
    public void saveBatch(List<Map<String, Object>> documents) {
        String sql = "INSERT INTO vector_store (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector)";
        jdbcTemplate.batchUpdate(sql, documents, documents.size(), (ps, doc) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, (String) doc.get("content"));
            ps.setString(3, (String) doc.getOrDefault("metadata", "{}"));
            ps.setString(4, toVectorString((float[]) doc.get("embedding")));
        });
    }

    /**
     * 余弦相似度检索，返回 topK 最相似的文档
     */
    public List<Map<String, Object>> similaritySearch(float[] queryEmbedding, int topK) {
        String vectorStr = toVectorString(queryEmbedding);
        return jdbcTemplate.queryForList(
                "SELECT id, content, metadata, 1 - (embedding <=> ?::vector) AS score " +
                "FROM vector_store ORDER BY embedding <=> ?::vector LIMIT ?",
                vectorStr, vectorStr, topK
        );
    }

    /**
     * 查询文档总数
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Long.class);
        return count != null ? count : 0;
    }

    /**
     * 清空向量存储
     */
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM vector_store");
    }

    /**
     * 将 float[] 转为 pgvector 可识别的字符串格式 [0.1,0.2,...]
     */
    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    // ---- 文档去重相关方法 ----

    /**
     * 检查指定来源的文档是否已加载过
     */
    public boolean isSourceLoaded(String source) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loaded_documents WHERE source = ?",
                Integer.class, source);
        return count != null && count > 0;
    }

    /**
     * 获取已加载文档的文件哈希值（用于检测文件是否变更）
     */
    public String getSourceHash(String source) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT file_hash FROM loaded_documents WHERE source = ?",
                    String.class, source);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 标记文档已成功加载（记录到 loaded_documents 表）
     */
    public void markSourceLoaded(String source, String fileHash, int chunkCount) {
        jdbcTemplate.update(
                "INSERT INTO loaded_documents (source, file_hash, chunk_count) VALUES (?, ?, ?) " +
                "ON CONFLICT (source) DO UPDATE SET file_hash = ?, chunk_count = ?, loaded_at = NOW()",
                source, fileHash, chunkCount, fileHash, chunkCount);
    }

    /**
     * 删除指定来源的所有向量数据（用于重新加载变更的文档）
     */
    public void deleteBySource(String source) {
        jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'source' = ?", source);
        jdbcTemplate.update(
                "DELETE FROM loaded_documents WHERE source = ?", source);
    }

    /**
     * 列出所有已加载的文档
     */
    public List<Map<String, Object>> listLoadedDocuments() {
        return jdbcTemplate.queryForList(
                "SELECT id, source, file_hash, chunk_count, loaded_at FROM loaded_documents ORDER BY loaded_at DESC");
    }

    /**
     * 清空 loaded_documents 表
     */
    public void clearLoadedDocuments() {
        jdbcTemplate.update("DELETE FROM loaded_documents");
    }
}
