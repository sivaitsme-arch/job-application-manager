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
