package com.foodwaste.platform.repository;

import com.foodwaste.platform.model.UserEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserEntity> mapper = this::mapRow;

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
              id VARCHAR(36) PRIMARY KEY,
              email VARCHAR(255) NOT NULL UNIQUE,
              password_hash VARCHAR(255) NOT NULL,
              name VARCHAR(120) NOT NULL,
              role ENUM('admin','donor','recipient','analyst') NOT NULL,
              status ENUM('active','inactive') NOT NULL DEFAULT 'active',
              phone VARCHAR(30) NULL,
              location VARCHAR(120) NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              INDEX idx_users_role (role),
              INDEX idx_users_status (status)
            )
            """);
    }

    public List<UserEntity> listUsers() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY created_at DESC", mapper);
    }

    public UserEntity findById(String id) {
        List<UserEntity> users = jdbcTemplate.query("SELECT * FROM users WHERE id = ? LIMIT 1", mapper, id);
        return users.isEmpty() ? null : users.get(0);
    }

    public UserEntity findByEmail(String email) {
        List<UserEntity> users = jdbcTemplate.query("SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1", mapper, email);
        return users.isEmpty() ? null : users.get(0);
    }

    public UserEntity create(UserEntity user) {
        jdbcTemplate.update("""
            INSERT INTO users (id, email, password_hash, name, role, status, phone, location, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            user.id, user.email, user.passwordHash, user.name, user.role, user.status,
            blankToNull(user.phone), blankToNull(user.location),
            Timestamp.from(user.createdAt), Timestamp.from(user.updatedAt == null ? user.createdAt : user.updatedAt));
        return findById(user.id);
    }

    public UserEntity update(String id, UserEntity patch) {
        jdbcTemplate.update("""
            UPDATE users
            SET name = ?, role = ?, status = ?, phone = ?, location = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            patch.name, patch.role, patch.status, blankToNull(patch.phone), blankToNull(patch.location), id);
        return findById(id);
    }

    public boolean deleteById(String id) {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) > 0;
    }

    public void replaceAll(List<UserEntity> users) {
        jdbcTemplate.update("DELETE FROM users");
        for (UserEntity user : users) {
            create(user);
        }
    }

    public void seedUsersIfEmpty(List<UserEntity> users) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        for (UserEntity user : users) {
            create(user);
        }
    }

    private UserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserEntity user = new UserEntity();
        user.id = rs.getString("id");
        user.email = rs.getString("email");
        user.passwordHash = rs.getString("password_hash");
        user.name = rs.getString("name");
        user.role = rs.getString("role");
        user.status = rs.getString("status");
        user.phone = defaultString(rs.getString("phone"));
        user.location = defaultString(rs.getString("location"));
        user.createdAt = rs.getTimestamp("created_at").toInstant();
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        user.updatedAt = updatedAt == null ? null : updatedAt.toInstant();
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
