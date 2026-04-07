package com.foodwaste.platform.repository;

import com.foodwaste.platform.model.RequestRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RequestRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RequestRecord> mapper = this::mapRow;

    public RequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS requests (
              id VARCHAR(36) PRIMARY KEY,
              donation_id VARCHAR(36) NOT NULL,
              recipient_id VARCHAR(36) NOT NULL,
              requested_by VARCHAR(120) NULL,
              status VARCHAR(30) NOT NULL,
              request_date VARCHAR(20) NOT NULL,
              estimated_delivery VARCHAR(120) NULL,
              delivery_date VARCHAR(20) NULL,
              created_at VARCHAR(40) NOT NULL,
              updated_at VARCHAR(40) NOT NULL,
              INDEX idx_requests_donation (donation_id),
              INDEX idx_requests_recipient (recipient_id),
              INDEX idx_requests_status (status)
            )
            """);
    }

    public List<RequestRecord> listAll() {
        return jdbcTemplate.query("SELECT * FROM requests ORDER BY created_at DESC", mapper);
    }

    public void replaceAll(List<RequestRecord> requests) {
        jdbcTemplate.update("DELETE FROM requests");
        for (RequestRecord request : requests) {
            jdbcTemplate.update("""
                INSERT INTO requests (
                  id, donation_id, recipient_id, requested_by, status, request_date,
                  estimated_delivery, delivery_date, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.id,
                request.donationId,
                request.recipientId,
                blankToNull(request.requestedBy),
                request.status,
                request.requestDate,
                blankToNull(request.estimatedDelivery),
                blankToNull(request.deliveryDate),
                request.createdAt,
                request.updatedAt
            );
        }
    }

    public void seedIfEmpty(List<RequestRecord> requests) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM requests", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        replaceAll(new ArrayList<>(requests));
    }

    private RequestRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        RequestRecord request = new RequestRecord();
        request.id = rs.getString("id");
        request.donationId = rs.getString("donation_id");
        request.recipientId = rs.getString("recipient_id");
        request.requestedBy = defaultString(rs.getString("requested_by"));
        request.status = rs.getString("status");
        request.requestDate = rs.getString("request_date");
        request.estimatedDelivery = defaultString(rs.getString("estimated_delivery"));
        request.deliveryDate = defaultString(rs.getString("delivery_date"));
        request.createdAt = rs.getString("created_at");
        request.updatedAt = rs.getString("updated_at");
        return request;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
