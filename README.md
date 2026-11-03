# Job Application Manager

A local JavaFX desktop application backed by Spring Boot, MySQL, Apache POI and Gmail SMTP.
It imports job rows from Excel/CSV, creates editable email drafts, and sends one approved email at a time with a PDF resume attachment.

## 1. Requirements
- Windows 10/11 (also works on macOS/Linux)
- JDK 17 or JDK 21
- Maven 3.9+
- MySQL 8.x running locally
- Gmail account with 2-Step Verification enabled and an App Password

## 2. Database setup
Run `database/schema.sql` in MySQL Workbench or execute:
```sql
CREATE DATABASE IF NOT EXISTS job_application_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
The application uses Hibernate `ddl-auto: update` to create/update tables automatically.

## 3. Configuration (.env)
The application automatically reads configuration from `.env` in the project root on startup.
You can copy `.env.example` to `.env`:
```env
DB_USERNAME=root
DB_PASSWORD=root
GMAIL_USERNAME=yourname@gmail.com
GMAIL_APP_PASSWORD=your16characterapppassword
CANDIDATE_NAME=Your Full Name
RESUME_DIRECTORY=./data/resumes
```
> [!NOTE]
> Google App Passwords often contain spaces (e.g., `xxxx xxxx xxxx xxxx`). The application automatically sanitizes and removes spaces before authenticating with Gmail SMTP.

You can also set these as standard environment variables in PowerShell if preferred:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:GMAIL_USERNAME="yourname@gmail.com"
$env:GMAIL_APP_PASSWORD="your16characterapppassword"
$env:CANDIDATE_NAME="Your Full Name"
```

## 4. Excel & CSV Templates
Both Excel (`.xlsx`, `.xls`) and CSV (`.csv`) files are supported.
The file must have a header row. Required columns (case-insensitive and tolerant to spaces/underscores):
- `Company` (or `Company Name`)
- `Job Role` (or `Role`, `Position`, `Job Title`)
- `Email` (or `Recruiter Email`, `Contact Email`)

Optional columns: `Location`, `Job URL`.

Example:
| Company | Job Role | Email | Location | Job URL |
|---|---|---|---|---|
| Example Technologies | Java Developer | recruiter@example.com | Hyderabad | https://example.com/jobs/123 |

## 5. Resumes
Place your PDF resume in `./data/resumes/` to have it auto-detected on startup, or click **Select Resume PDF** in the application to browse for any PDF resume.

## 6. Run
From the project directory:
```powershell
mvn clean javafx:run
```
The JavaFX desktop window will open.
1. Click **Import Jobs (Excel/CSV)** to import jobs from a spreadsheet or CSV.
2. Select a row and click **Generate / Regenerate Draft** to prepare the email.
3. Click **Preview / Edit Email** to inspect and customize the subject and body, then click **Save Draft**.
4. Confirm your resume is selected.
5. Click **Approve & Send** to send the email via Gmail SMTP.

## 7. Workflow and Safety
- Duplicates are automatically skipped using company + job role + recruiter email.
- Emails are never sent automatically on import or draft generation; every email requires explicit approval.
- Background sending ensures the desktop UI remains responsive and does not freeze.
- `SENT` applications cannot be resent.
- If sending fails, the status is marked as `FAILED` and full error details are displayed.
- The **Delete** button allows removing unwanted records.

## 8. Troubleshooting
- `Access denied for user`: check `DB_USERNAME` and `DB_PASSWORD` in `.env`.
- `Communications link failure`: ensure MySQL service (e.g., `MySQL84`) is running locally on port 3306.
- `AuthenticationFailedException`: verify `GMAIL_USERNAME` and generate a fresh 16-character Google App Password under Google Account -> Security -> 2-Step Verification -> App Passwords.
