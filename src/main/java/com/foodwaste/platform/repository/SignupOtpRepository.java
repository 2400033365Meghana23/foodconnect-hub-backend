package com.foodwaste.platform.repository;

import com.foodwaste.platform.model.SignupOtpEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SignupOtpRepository {

    private final JdbcTemplate jdbcTemplate;

    public SignupOtpRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SignupOtpEntity> mapper = this::mapRow;

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS signup_otps (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              email VARCHAR(255) NOT NULL UNIQUE,
              otp_code VARCHAR(6) NOT NULL,
              expires_at DATETIME NOT NULL,
              attempts INT NOT NULL DEFAULT 0,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              INDEX idx_signup_otps_expires_at (expires_at)
            )
            """);
    }

    public void save(String email, String otpCode, Instant expiresAt) {
        jdbcTemplate.update("""
            INSERT INTO signup_otps (email, otp_code, expires_at, attempts)
            VALUES (?, ?, ?, 0)
            ON DUPLICATE KEY UPDATE
              otp_code = VALUES(otp_code),
              expires_at = VALUES(expires_at),
              attempts = 0
            """, email, otpCode, Timestamp.from(expiresAt));
    }

    public SignupOtpEntity findByEmail(String email) {
        List<SignupOtpEntity> rows = jdbcTemplate.query(
            "SELECT * FROM signup_otps WHERE LOWER(email) = LOWER(?) LIMIT 1",
            mapper,
            email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void incrementAttempts(String email) {
        jdbcTemplate.update("UPDATE signup_otps SET attempts = attempts + 1 WHERE LOWER(email) = LOWER(?)", email);
    }

    public void deleteByEmail(String email) {
        jdbcTemplate.update("DELETE FROM signup_otps WHERE LOWER(email) = LOWER(?)", email);
    }

    private SignupOtpEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        SignupOtpEntity entity = new SignupOtpEntity();
        entity.id = rs.getLong("id");
        entity.email = rs.getString("email");
        entity.otpCode = rs.getString("otp_code");
        entity.expiresAt = rs.getTimestamp("expires_at").toInstant();
        entity.attempts = rs.getInt("attempts");
        return entity;
    }
}
