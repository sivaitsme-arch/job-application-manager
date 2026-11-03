# Job Application Manager Enterprise

A modern desktop application built with **JavaFX 21**, **Spring Boot 3.5.x**, **MySQL 8**, **Apache POI**, **Apache PDFBox**, and **Gmail SMTP**.
It supports automated job discovery from multiple providers, resume-based skill and experience matching, interactive job application queueing, editable email drafts, and single-click approved email dispatching with PDF attachments.

---

## 1. Requirements
- Windows 10/11, macOS, or Linux
- JDK 17 or JDK 21
- Maven 3.9+
- MySQL 8.x running locally
- Gmail account with 2-Step Verification and an App Password

---

## 2. Desktop Navigation Structure

The application features a clean, professional dark navy desktop interface with dedicated navigation sections:

```text
Job Application Manager
│
├── Dashboard
│   └── High-level metrics: Ingested, Ready, Sent, Failed, and Active Providers
│
├── Resume Profile
│   └── Resume viewer and automated skill extraction using Apache PDFBox
│
├── Job Discovery
│   └── Multi-provider search, India 3–5 yr filtering, and matching percentage calculation
│
├── Job Discovery Settings (Configuration Mode)
│   ├── Adzuna (App ID, App Key, Country, Results Limit)
│   ├── Jooble (API Key, Search Location, Results Limit)
│   ├── Greenhouse (Board Tokens, Multiple Company Management, Public API Testing)
│   └── Lever (Site Identifiers, Multiple Company Management, Postings Testing)
│
├── Excel Management
│   └── Import & manage spreadsheet/CSV rows, duplicate skipping, validation
│
└── Email Applications
    └── Email draft generation, rich preview/edit, and one-by-one approved sending
```

---

## 3. Job Discovery Configuration Mode

Instead of configuring provider credentials through environment variables, you can configure, test, and manage all four job providers directly through the **Job Discovery Settings** UI:

### Supported Providers
1. **Adzuna**:
   - Fields: Enable Provider, Application ID, Application Key (masked), Country Code (default `in`), Results Limit.
   - Actions: Save Configuration, Test Connection (issues a live India search request to verify credentials), Reset.
2. **Jooble**:
   - Fields: Enable Provider, API Key (masked), Search Location (default `India`), Results Limit.
   - Actions: Save Configuration, Test Connection, Reset.
3. **Greenhouse**:
   - Public job boards API integration.
   - Add multiple company board tokens (e.g., `stripe`, `gitlab`, `airbnb`).
   - Actions: Add Company, Remove Company, Toggle Active, Test Board Connection, Test All Configured Boards.
4. **Lever**:
   - Public postings API integration.
   - Add multiple company site identifiers (e.g., `netflix`, `figma`).
   - Actions: Add Company, Remove Company, Toggle Active, Test Site Connection, Test All Configured Sites.

### Credential Security
- Credentials are encrypted at rest using **AES-256-GCM** with a unique 12-byte `SecureRandom` IV and a 128-bit authentication tag.
- Credentials are never logged or stored as plain text.
- External key can be configured via environment variable `APP_ENCRYPTION_KEY` or `JOB_MANAGER_ENCRYPTION_KEY`.
- If no key is set, the application automatically initializes a secure 256-bit persistent key in `data/.master_key`.
- In the UI, credentials are automatically masked (e.g., `••••••••`).

---

## 4. Database Setup & Migration

Execute `database/schema.sql` (or `database/job_provider_schema.sql` if upgrading an existing database) in MySQL:

```powershell
Get-Content database/schema.sql -Raw | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -proot
```

Tables created:
- `job_application`: Applications, statuses (`IMPORTED`, `READY`, `SENT`, `FAILED`), email content, sent timestamps.
- `job_provider_config`: Provider configurations, enabled flags, limits, and locations.
- `job_provider_credential`: AES-256-GCM encrypted provider credentials.
- `job_provider_company`: Configured company boards and site identifiers for Greenhouse and Lever.

---

## 5. Configuration (.env)

The application automatically loads `.env` on startup. Copy `.env.example` to `.env`:

```env
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
GMAIL_USERNAME=yourname@gmail.com
GMAIL_APP_PASSWORD=your16characterapppassword
CANDIDATE_NAME=Your Full Name
RESUME_DIRECTORY=./data/resumes
```

> [!NOTE]
> Google App Passwords copied with spaces (e.g. `xxxx xxxx xxxx xxxx`) are automatically sanitized and stripped of whitespace before connecting to Gmail SMTP.

---

## 6. How to Run

From the project root:

```powershell
mvn clean javafx:run
```

1. Navigate to **Job Discovery Settings** to configure and enable your desired providers (Adzuna, Jooble, Greenhouse, Lever).
2. Click **Test Connection** to verify that credentials and company board endpoints respond successfully.
3. Open **Job Discovery** to discover jobs, view match percentages calculated against your resume, and click **Add to Applications** to queue jobs.
4. Under **Email Applications**, select a row, click **Generate / Regenerate Draft**, inspect with **Preview / Edit Email**, and click **Approve & Send**.

---

## 7. Running Unit Tests

To run the complete test suite:

```powershell
mvn test
```

Unit tests cover:
- AES-256-GCM encryption/decryption, random IV differentiation, and credential masking.
- Provider configuration CRUD, enable/disable toggling, and duplicate company prevention.
- Connection testing, credential validation, and timeout handling.
- Dynamic provider execution and provider failure isolation.
- Resume-based skill extraction and match percentage scoring.
- Excel and CSV parsing and import regression.
