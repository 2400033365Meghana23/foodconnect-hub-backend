package com.foodwaste.platform.repository;

import com.foodwaste.platform.model.Donation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DonationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Donation> mapper = this::mapRow;

    public DonationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS donations (
              id VARCHAR(36) PRIMARY KEY,
              item VARCHAR(120) NOT NULL,
              category VARCHAR(40) NOT NULL,
              quantity DOUBLE NOT NULL,
              unit VARCHAR(20) NOT NULL,
              status VARCHAR(30) NOT NULL,
              donation_date VARCHAR(20) NOT NULL,
              expiry_date VARCHAR(40) NULL,
              expiry_hours INT NULL,
              location VARCHAR(160) NOT NULL,
              description TEXT NULL,
              recipient_org VARCHAR(120) NULL,
              donor_id VARCHAR(36) NOT NULL,
              assigned_request_id VARCHAR(36) NULL,
              created_at VARCHAR(40) NOT NULL,
              updated_at VARCHAR(40) NOT NULL,
              INDEX idx_donations_donor (donor_id),
              INDEX idx_donations_status (status),
              INDEX idx_donations_category (category)
            )
            """);
    }

    public List<Donation> listAll() {
        return jdbcTemplate.query("SELECT * FROM donations ORDER BY created_at DESC", mapper);
    }

    public void replaceAll(List<Donation> donations) {
        jdbcTemplate.update("DELETE FROM donations");
        for (Donation donation : donations) {
            jdbcTemplate.update("""
                INSERT INTO donations (
                  id, item, category, quantity, unit, status, donation_date, expiry_date, expiry_hours,
                  location, description, recipient_org, donor_id, assigned_request_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                donation.id,
                donation.item,
                donation.category,
                donation.quantity == null ? 0d : donation.quantity,
                donation.unit,
                donation.status,
                donation.date,
                blankToNull(donation.expiryDate),
                donation.expiryHours,
                donation.location,
                blankToNull(donation.description),
                blankToNull(donation.recipientOrg),
                donation.donorId,
                blankToNull(donation.assignedRequestId),
                donation.createdAt,
                donation.updatedAt
            );
        }
    }

    public void seedIfEmpty(List<Donation> donations) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM donations", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        replaceAll(new ArrayList<>(donations));
    }

    private Donation mapRow(ResultSet rs, int rowNum) throws SQLException {
        Donation donation = new Donation();
        donation.id = rs.getString("id");
        donation.item = rs.getString("item");
        donation.category = rs.getString("category");
        donation.quantity = rs.getDouble("quantity");
        donation.unit = rs.getString("unit");
        donation.status = rs.getString("status");
        donation.date = rs.getString("donation_date");
        donation.expiryDate = defaultString(rs.getString("expiry_date"));
        Integer expiryHours = rs.getObject("expiry_hours", Integer.class);
        donation.expiryHours = expiryHours;
        donation.location = rs.getString("location");
        donation.description = defaultString(rs.getString("description"));
        donation.recipientOrg = defaultString(rs.getString("recipient_org"));
        donation.donorId = rs.getString("donor_id");
        donation.assignedRequestId = defaultString(rs.getString("assigned_request_id"));
        donation.createdAt = rs.getString("created_at");
        donation.updatedAt = rs.getString("updated_at");
        return donation;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
