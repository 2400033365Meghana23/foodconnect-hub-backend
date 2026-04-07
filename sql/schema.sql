CREATE DATABASE IF NOT EXISTS food_waste_platform;
USE food_waste_platform;

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
);

CREATE TABLE IF NOT EXISTS signup_otps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  otp_code VARCHAR(6) NOT NULL,
  expires_at DATETIME NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_signup_otps_expires_at (expires_at)
);

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
);

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
);

CREATE TABLE IF NOT EXISTS content_items (
  id VARCHAR(36) PRIMARY KEY,
  type VARCHAR(30) NOT NULL,
  payload JSON NOT NULL,
  created_at VARCHAR(40) NOT NULL,
  updated_at VARCHAR(40) NOT NULL,
  INDEX idx_content_type (type)
);

CREATE TABLE IF NOT EXISTS settings_entries (
  section VARCHAR(40) PRIMARY KEY,
  payload JSON NOT NULL,
  updated_at VARCHAR(40) NOT NULL
);
