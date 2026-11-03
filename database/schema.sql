CREATE DATABASE IF NOT EXISTS job_application_manager
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE job_application_manager;

CREATE TABLE IF NOT EXISTS job_application (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 company_name VARCHAR(180) NOT NULL,
 job_role VARCHAR(180) NOT NULL,
 recruiter_email VARCHAR(254) NOT NULL,
 location VARCHAR(180),
 job_url VARCHAR(1000),
 status VARCHAR(30) NOT NULL DEFAULT 'IMPORTED',
 email_subject VARCHAR(300),
 email_body TEXT,
 resume_path VARCHAR(1000),
 sent_at DATETIME,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_company_role_email (company_name, job_role, recruiter_email)
);

-- 1. Provider Configuration Table
CREATE TABLE IF NOT EXISTS job_provider_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_name VARCHAR(50) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    country_code VARCHAR(10) DEFAULT 'in',
    location VARCHAR(100) DEFAULT 'India',
    results_per_search INT NOT NULL DEFAULT 20,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Encrypted Credentials Table (AES-256-GCM encrypted)
CREATE TABLE IF NOT EXISTS job_provider_credential (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_config_id BIGINT NOT NULL,
    credential_key VARCHAR(50) NOT NULL,
    encrypted_credential_value TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_key (provider_config_id, credential_key),
    CONSTRAINT fk_credential_provider FOREIGN KEY (provider_config_id) REFERENCES job_provider_config (id) ON DELETE CASCADE
);

-- 3. Configured Provider Companies (Greenhouse & Lever)
CREATE TABLE IF NOT EXISTS job_provider_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_name VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    board_identifier VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_company_board (provider_name, board_identifier)
);

-- Seed default provider configurations
INSERT IGNORE INTO job_provider_config (provider_name, enabled, country_code, location, results_per_search)
VALUES 
('ADZUNA', FALSE, 'in', 'India', 30),
('JOOBLE', FALSE, 'in', 'India', 20),
('GREENHOUSE', FALSE, 'in', 'Global/Remote', 25),
('LEVER', FALSE, 'in', 'Global/Remote', 25);
