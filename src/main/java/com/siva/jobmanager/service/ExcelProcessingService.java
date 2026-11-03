package com.siva.jobmanager.service;

import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.repository.JobApplicationRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ExcelProcessingService {
    private final JobApplicationRepository repository;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public ExcelProcessingService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public ImportResult importExcel(Path file) throws Exception {
        return importFile(file);
    }

    @Transactional
    public ImportResult importFile(Path file) throws Exception {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".csv")) {
            return importCsv(file);
        }
        return processWorkbook(file);
    }

    private ImportResult processWorkbook(Path file) throws Exception {
        int imported = 0, duplicates = 0, invalid = 0;
        List<String> errors = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel must contain a header and at least one data row.");
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("Header row is missing in Excel sheet.");
            }
            Map<String, Integer> columns = new HashMap<>();
            for (Cell c : header) {
                columns.put(normalize(value(c)), c.getColumnIndex());
            }
            int companyCol = col(columns, "company", "company name", "organization", "employer");
            int roleCol = col(columns, "job role", "role", "position", "job title", "title");
            int emailCol = col(columns, "email", "recruiter email", "contact email", "email address", "email id");
            int locationCol = optionalCol(columns, "location", "city", "place");
            int urlCol = optionalCol(columns, "job url", "url", "link", "job link");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String company = cell(row, companyCol);
                String role = cell(row, roleCol);
                String email = cell(row, emailCol).toLowerCase(Locale.ROOT);
                String location = locationCol < 0 ? null : cell(row, locationCol);
                String url = urlCol < 0 ? null : cell(row, urlCol);

                if (company.isBlank() && role.isBlank() && email.isBlank()) {
                    continue; // Skip empty row
                }
                if (company.isBlank() || role.isBlank() || !EMAIL.matcher(email).matches()) {
                    invalid++;
                    errors.add("Row " + (i + 1) + ": company, role or email is invalid.");
                    continue;
                }
                if (repository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(company, role, email)) {
                    duplicates++;
                    continue;
                }
                JobApplication job = new JobApplication();
                job.setCompanyName(company);
                job.setJobRole(role);
                job.setRecruiterEmail(email);
                job.setLocation(location == null || location.isBlank() ? null : location);
                job.setJobUrl(url == null || url.isBlank() ? null : url);
                repository.save(job);
                imported++;
            }
        }
        return new ImportResult(imported, duplicates, invalid, errors);
    }

    private ImportResult importCsv(Path file) throws Exception {
        int imported = 0, duplicates = 0, invalid = 0;
        List<String> errors = new ArrayList<>();
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        if (raw.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }
        if (raw.contains("\\n") && !raw.contains("\n") && !raw.contains("\r")) {
            raw = raw.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
        }
        String[] lineArr = raw.split("\\r?\\n");
        List<List<String>> rows = new ArrayList<>();
        for (String line : lineArr) {
            if (line.trim().isEmpty()) continue;
            rows.add(parseCsvLine(line));
        }
        if (rows.size() < 2) {
            throw new IllegalArgumentException("CSV must contain a header and at least one data row.");
        }
        List<String> header = rows.get(0);
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            columns.put(normalize(header.get(i)), i);
        }

        int companyCol = col(columns, "company", "company name", "organization", "employer");
        int roleCol = col(columns, "job role", "role", "position", "job title", "title");
        int emailCol = col(columns, "email", "recruiter email", "contact email", "email address", "email id");
        int locationCol = optionalCol(columns, "location", "city", "place");
        int urlCol = optionalCol(columns, "job url", "url", "link", "job link");

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String company = csvCell(row, companyCol);
            String role = csvCell(row, roleCol);
            String email = csvCell(row, emailCol).toLowerCase(Locale.ROOT);
            String location = locationCol < 0 ? null : csvCell(row, locationCol);
            String url = urlCol < 0 ? null : csvCell(row, urlCol);

            if (company.isBlank() && role.isBlank() && email.isBlank()) {
                continue;
            }
            if (company.isBlank() || role.isBlank() || !EMAIL.matcher(email).matches()) {
                invalid++;
                errors.add("Row " + (i + 1) + ": company, role or email is invalid.");
                continue;
            }
            if (repository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(company, role, email)) {
                duplicates++;
                continue;
            }
            JobApplication job = new JobApplication();
            job.setCompanyName(company);
            job.setJobRole(role);
            job.setRecruiterEmail(email);
            job.setLocation(location == null || location.isBlank() ? null : location);
            job.setJobUrl(url == null || url.isBlank() ? null : url);
            repository.save(job);
            imported++;
        }
        return new ImportResult(imported, duplicates, invalid, errors);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    private static String csvCell(List<String> row, int i) {
        return (i >= 0 && i < row.size()) ? row.get(i).trim() : "";
    }

    private static String normalize(String str) {
        if (str == null) return "";
        return str.trim().toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]+", " ");
    }

    private static int col(Map<String, Integer> m, String... names) {
        int v = optionalCol(m, names);
        if (v < 0) throw new IllegalArgumentException("Missing required column: " + String.join(" / ", names));
        return v;
    }

    private static int optionalCol(Map<String, Integer> m, String... names) {
        for (String n : names) {
            String norm = normalize(n);
            if (m.containsKey(norm)) return m.get(norm);
            for (Map.Entry<String, Integer> entry : m.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(norm) ||
                    entry.getKey().replace(" ", "").equalsIgnoreCase(norm.replace(" ", ""))) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }

    private static String cell(Row row, int i) {
        return i < 0 ? "" : value(row.getCell(i)).trim();
    }

    private static String value(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) {
            return DateUtil.isCellDateFormatted(c) ? c.getLocalDateTimeCellValue().toString() : String.valueOf((long) c.getNumericCellValue());
        }
        if (c.getCellType() == CellType.FORMULA) {
            try {
                return c.getStringCellValue();
            } catch (Exception e) {
                return String.valueOf(c.getNumericCellValue());
            }
        }
        return c.toString();
    }

    public record ImportResult(int imported, int duplicates, int invalid, List<String> errors) {}
}
