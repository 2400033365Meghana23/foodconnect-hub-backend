package com.foodwaste.platform.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SettingsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS settings_entries (
              section VARCHAR(40) PRIMARY KEY,
              payload JSON NOT NULL,
              updated_at VARCHAR(40) NOT NULL
            )
            """);
    }

    public Map<String, Map<String, Object>> listAll() {
        Map<String, Map<String, Object>> settings = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT section, payload FROM settings_entries", rs -> {
            settings.put(rs.getString("section"), readJson(rs.getString("payload")));
        });
        return settings;
    }

    public void replaceAll(Map<String, Map<String, Object>> settings) {
        jdbcTemplate.update("DELETE FROM settings_entries");
        for (Map.Entry<String, Map<String, Object>> entry : settings.entrySet()) {
            jdbcTemplate.update(
                "INSERT INTO settings_entries (section, payload, updated_at) VALUES (?, CAST(? AS JSON), ?)",
                entry.getKey(),
                writeJson(entry.getValue()),
                Instant.now().toString()
            );
        }
    }

    public void seedIfEmpty(Map<String, Map<String, Object>> settings) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM settings_entries", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        replaceAll(settings);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize settings payload", ex);
        }
    }

    private Map<String, Object> readJson(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize settings payload", ex);
        }
    }
}
