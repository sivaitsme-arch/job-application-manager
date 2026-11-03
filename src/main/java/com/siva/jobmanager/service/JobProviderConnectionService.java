package com.siva.jobmanager.service;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class JobProviderConnectionService {

    private static final Logger log = LoggerFactory.getLogger(JobProviderConnectionService.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderCredentialService credentialService;
    private final RestClient restClient;

    public JobProviderConnectionService(
            JobProviderConfigurationService configService,
            JobProviderCredentialService credentialService,
            RestClient restClient) {
        this.configService = configService;
        this.credentialService = credentialService;
        this.restClient = restClient;
    }

    public JobProviderConnectionService(
            JobProviderConfigurationService configService,
            JobProviderCredentialService credentialService) {
        this(configService, credentialService, RestClient.builder().build());
    }

    /**
     * Tests connectivity for a named provider using its saved configuration and credentials.
     */
    public ConnectionTestResult testConnection(String providerName) {
        String name = providerName.trim().toUpperCase();
        log.info("{} connection test started", name);

        JobProviderConfig config = configService.getOrCreateConfig(name);
        Map<String, String> creds = credentialService.getAllDecryptedCredentials(config.getId());

        try {
            return switch (name) {
                case "ADZUNA" -> testAdzuna(config, creds);
                case "JOOBLE" -> testJooble(config, creds);
                case "GREENHOUSE" -> testGreenhouse(config);
                case "LEVER" -> testLever(config);
                default -> ConnectionTestResult.error(name, "Unknown provider name: " + name, null);
            };
        } catch (Exception ex) {
            log.error("Job provider connection test threw unhandled exception for: {}", name, ex);
            return ConnectionTestResult.error(name, "Unexpected test error: " + ex.getMessage(), null);
        }
    }

    /**
     * Adzuna connection test.
     * Validates App ID and App Key by issuing a minimal search request for India.
     */
    public ConnectionTestResult testAdzunaWithCredentials(String appId, String appKey, String countryCode) {
        if (appId == null || appId.isBlank()) {
            return ConnectionTestResult.failure("ADZUNA", null, "Application ID is missing", "Please enter your Adzuna Application ID.");
        }
        if (appKey == null || appKey.isBlank()) {
            return ConnectionTestResult.failure("ADZUNA", null, "Application Key is missing", "Please enter your Adzuna Application Key.");
        }
        String country = (countryCode != null && !countryCode.isBlank()) ? countryCode.trim().toLowerCase() : "in";
        String url = String.format("https://api.adzuna.com/v1/api/jobs/%s/search/1?app_id=%s&app_key=%s&results_per_page=1&what=developer",
                country, appId.trim(), appKey.trim());

        try {
            var response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            int code = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Adzuna connection successful (HTTP {})", code);
                return ConnectionTestResult.success("ADZUNA", code, "Successfully connected to Adzuna API (Country: " + country.toUpperCase() + ")");
            } else {
                log.warn("Adzuna connection failed with status: {}", code);
                return ConnectionTestResult.failure("ADZUNA", code, "Adzuna returned HTTP " + code, response.getBody());
            }
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            String msg = (code == 400 || code == 401 || code == 403)
                    ? "Invalid Application ID or Application Key"
                    : "Adzuna HTTP client error: " + ex.getStatusText();
            log.warn("Adzuna connection failed: HTTP {}", code);
            return ConnectionTestResult.failure("ADZUNA", code, msg, ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.warn("Adzuna server error: HTTP {}", ex.getStatusCode().value());
            return ConnectionTestResult.failure("ADZUNA", ex.getStatusCode().value(), "Adzuna server is temporarily unavailable", null);
        } catch (ResourceAccessException ex) {
            log.warn("Adzuna network timeout/unreachable");
            return ConnectionTestResult.error("ADZUNA", "Connection timed out. Check your internet connection.", ex.getMessage());
        }
    }

    private ConnectionTestResult testAdzuna(JobProviderConfig config, Map<String, String> creds) {
        String appId = creds.get("APP_ID");
        String appKey = creds.get("APP_KEY");
        return testAdzunaWithCredentials(appId, appKey, config.getCountryCode());
    }

    /**
     * Jooble connection test.
     * Validates API Key by querying Jooble API.
     */
    public ConnectionTestResult testJoobleWithKey(String apiKey, String location) {
        if (apiKey == null || apiKey.isBlank()) {
            return ConnectionTestResult.failure("JOOBLE", null, "API Key is missing", "Please enter your Jooble API Key.");
        }
        String loc = (location != null && !location.isBlank()) ? location.trim() : "India";
        String url = "https://jooble.org/api/" + apiKey.trim();
        String body = String.format("{\"keywords\":\"developer\",\"location\":\"%s\",\"page\":1,\"resultonpage\":1}", loc);

        try {
            var response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            int code = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Jooble connection successful (HTTP {})", code);
                return ConnectionTestResult.success("JOOBLE", code, "Successfully connected to Jooble API (Location: " + loc + ")");
            } else {
                log.warn("Jooble connection failed: HTTP {}", code);
                return ConnectionTestResult.failure("JOOBLE", code, "Jooble returned HTTP " + code, response.getBody());
            }
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            String msg = (code == 401 || code == 403)
                    ? "Invalid Jooble API Key"
                    : "Jooble HTTP error: " + ex.getStatusText();
            log.warn("Jooble connection failed: HTTP {}", code);
            return ConnectionTestResult.failure("JOOBLE", code, msg, ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.warn("Jooble server error: HTTP {}", ex.getStatusCode().value());
            return ConnectionTestResult.failure("JOOBLE", ex.getStatusCode().value(), "Jooble server is temporarily unavailable", null);
        } catch (ResourceAccessException ex) {
            log.warn("Jooble network timeout/unreachable");
            return ConnectionTestResult.error("JOOBLE", "Connection timed out. Check your internet connection.", ex.getMessage());
        }
    }

    private ConnectionTestResult testJooble(JobProviderConfig config, Map<String, String> creds) {
        String apiKey = creds.get("API_KEY");
        return testJoobleWithKey(apiKey, config.getLocation());
    }

    /**
     * Greenhouse connection test for a specific company board token.
     * Public endpoint: https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs
     */
    public ConnectionTestResult testGreenhouseBoard(String boardToken, String companyName) {
        if (boardToken == null || boardToken.isBlank()) {
            return ConnectionTestResult.failure("GREENHOUSE", null, "Board token is missing", null);
        }
        String token = boardToken.trim().toLowerCase();
        String url = "https://boards-api.greenhouse.io/v1/boards/" + token + "/jobs";

        try {
            var response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            int code = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful()) {
                String cName = (companyName != null && !companyName.isBlank()) ? companyName : token;
                log.info("Greenhouse board '{}' connection successful", token);
                return ConnectionTestResult.success("GREENHOUSE", code, "Connected to Greenhouse job board for '" + cName + "'");
            } else {
                return ConnectionTestResult.failure("GREENHOUSE", code, "Greenhouse returned HTTP " + code, null);
            }
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            String msg = (code == 404)
                    ? "Job board not found. Verify the board token '" + token + "'."
                    : "Greenhouse error: " + ex.getStatusText();
            log.warn("Greenhouse board '{}' failed: HTTP {}", token, code);
            return ConnectionTestResult.failure("GREENHOUSE", code, msg, null);
        } catch (ResourceAccessException ex) {
            return ConnectionTestResult.error("GREENHOUSE", "Network timeout connecting to Greenhouse.", ex.getMessage());
        }
    }

    private ConnectionTestResult testGreenhouse(JobProviderConfig config) {
        List<JobProviderCompany> companies = configService.getEnabledCompanies("GREENHOUSE");
        if (companies.isEmpty()) {
            return ConnectionTestResult.failure("GREENHOUSE", null, "No companies configured", "Please add at least one company with a Greenhouse board token.");
        }
        int successful = 0;
        StringBuilder details = new StringBuilder();
        for (JobProviderCompany company : companies) {
            ConnectionTestResult r = testGreenhouseBoard(company.getBoardIdentifier(), company.getCompanyName());
            details.append(company.getCompanyName()).append(": ").append(r.getMessage()).append("\n");
            if (r.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                successful++;
            }
        }
        if (successful == companies.size()) {
            return ConnectionTestResult.success("GREENHOUSE", 200, "All " + successful + " Greenhouse boards validated successfully!\n" + details);
        } else if (successful > 0) {
            return ConnectionTestResult.failure("GREENHOUSE", 207, successful + "/" + companies.size() + " Greenhouse boards responded successfully.\n" + details, null);
        } else {
            return ConnectionTestResult.failure("GREENHOUSE", 400, "None of the configured Greenhouse boards could be reached.\n" + details, null);
        }
    }

    /**
     * Lever connection test for a specific company site identifier.
     * Public endpoint: https://api.lever.co/v0/postings/{site_identifier}?mode=json&limit=1
     */
    public ConnectionTestResult testLeverSite(String siteIdentifier, String companyName) {
        if (siteIdentifier == null || siteIdentifier.isBlank()) {
            return ConnectionTestResult.failure("LEVER", null, "Site identifier is missing", null);
        }
        String site = siteIdentifier.trim().toLowerCase();
        String url = "https://api.lever.co/v0/postings/" + site + "?mode=json&limit=1";

        try {
            var response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);

            int code = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful()) {
                String cName = (companyName != null && !companyName.isBlank()) ? companyName : site;
                log.info("Lever site '{}' connection successful", site);
                return ConnectionTestResult.success("LEVER", code, "Connected to Lever postings for '" + cName + "'");
            } else {
                return ConnectionTestResult.failure("LEVER", code, "Lever returned HTTP " + code, null);
            }
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            String msg = (code == 404)
                    ? "Lever site not found. Verify site identifier '" + site + "'."
                    : "Lever error: " + ex.getStatusText();
            log.warn("Lever site '{}' failed: HTTP {}", site, code);
            return ConnectionTestResult.failure("LEVER", code, msg, null);
        } catch (ResourceAccessException ex) {
            return ConnectionTestResult.error("LEVER", "Network timeout connecting to Lever.", ex.getMessage());
        }
    }

    private ConnectionTestResult testLever(JobProviderConfig config) {
        List<JobProviderCompany> companies = configService.getEnabledCompanies("LEVER");
        if (companies.isEmpty()) {
            return ConnectionTestResult.failure("LEVER", null, "No companies configured", "Please add at least one company with a Lever site identifier.");
        }
        int successful = 0;
        StringBuilder details = new StringBuilder();
        for (JobProviderCompany company : companies) {
            ConnectionTestResult r = testLeverSite(company.getBoardIdentifier(), company.getCompanyName());
            details.append(company.getCompanyName()).append(": ").append(r.getMessage()).append("\n");
            if (r.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                successful++;
            }
        }
        if (successful == companies.size()) {
            return ConnectionTestResult.success("LEVER", 200, "All " + successful + " Lever company sites validated successfully!\n" + details);
        } else if (successful > 0) {
            return ConnectionTestResult.failure("LEVER", 207, successful + "/" + companies.size() + " Lever sites responded successfully.\n" + details, null);
        } else {
            return ConnectionTestResult.failure("LEVER", 400, "None of the configured Lever sites could be reached.\n" + details, null);
        }
    }
}

