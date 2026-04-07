package com.foodwaste.platform.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ContentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS content_items (
              id VARCHAR(36) PRIMARY KEY,
              type VARCHAR(30) NOT NULL,
              payload JSON NOT NULL,
              created_at VARCHAR(40) NOT NULL,
              updated_at VARCHAR(40) NOT NULL,
              INDEX idx_content_type (type)
            )
            """);
    }

    public Map<String, List<Map<String, Object>>> listAll() {
        Map<String, List<Map<String, Object>>> content = new LinkedHashMap<>();
        content.put("pages", new ArrayList<>());
        content.put("faqs", new ArrayList<>());
        content.put("announcements", new ArrayList<>());

        jdbcTemplate.query("SELECT type, payload FROM content_items ORDER BY created_at DESC", rs -> {
            String type = rs.getString("type");
            content.computeIfAbsent(type, key -> new ArrayList<>()).add(readJson(rs.getString("payload")));
        });
        return content;
    }

    public void replaceAll(Map<String, List<Map<String, Object>>> content) {
        jdbcTemplate.update("DELETE FROM content_items");
        for (Map.Entry<String, List<Map<String, Object>>> entry : content.entrySet()) {
            for (Map<String, Object> item : entry.getValue()) {
                jdbcTemplate.update(
                    "INSERT INTO content_items (id, type, payload, created_at, updated_at) VALUES (?, ?, CAST(? AS JSON), ?, ?)",
                    String.valueOf(item.get("id")),
                    entry.getKey(),
                    writeJson(item),
                    String.valueOf(item.getOrDefault("createdAt", "")),
                    String.valueOf(item.getOrDefault("updatedAt", ""))
                );
            }
        }
    }

    public void seedIfEmpty(Map<String, List<Map<String, Object>>> content) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM content_items", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        replaceAll(content);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize content payload", ex);
        }
    }

    private Map<String, Object> readJson(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize content payload", ex);
        }
    }
}
