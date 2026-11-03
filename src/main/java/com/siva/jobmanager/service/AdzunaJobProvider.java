package com.siva.jobmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.DiscoveredJob;
import com.siva.jobmanager.model.JobProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AdzunaJobProvider implements JobProvider {

    private static final Logger log = LoggerFactory.getLogger(AdzunaJobProvider.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderCredentialService credentialService;
    private final JobProviderConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdzunaJobProvider(
            JobProviderConfigurationService configService,
            JobProviderCredentialService credentialService,
            JobProviderConnectionService connectionService,
            RestClient restClient) {
        this.configService = configService;
        this.credentialService = credentialService;
        this.connectionService = connectionService;
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "ADZUNA";
    }

    @Override
    public boolean isEnabled() {
        return configService.getConfig("ADZUNA")
                .map(JobProviderConfig::isEnabled)
                .orElse(false);
    }

    @Override
    public ConnectionTestResult testConnection() {
        return connectionService.testConnection("ADZUNA");
    }

    @Override
    public List<DiscoveredJob> search(String query, String location, int limit) {
        List<DiscoveredJob> jobs = new ArrayList<>();
        JobProviderConfig config = configService.getOrCreateConfig("ADZUNA");
        if (!config.isEnabled()) {
            return jobs;
        }

        Map<String, String> creds = credentialService.getAllDecryptedCredentials(config.getId());
        String appId = creds.get("APP_ID");
        String appKey = creds.get("APP_KEY");

        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            log.warn("Adzuna search skipped: credentials not configured");
            return jobs;
        }

        int pageSize = limit > 0 ? limit : config.getResultsPerSearch();
        String country = config.getCountryCode() != null ? config.getCountryCode().trim().toLowerCase() : "in";
        String encodedQuery = URLEncoder.encode(query != null ? query : "software developer", StandardCharsets.UTF_8);
        String encodedLoc = URLEncoder.encode(location != null ? location : "India", StandardCharsets.UTF_8);

        String url = String.format("https://api.adzuna.com/v1/api/jobs/%s/search/1?app_id=%s&app_key=%s&results_per_page=%d&what=%s&where=%s",
                country, appId.trim(), appKey.trim(), pageSize, encodedQuery, encodedLoc);

        try {
            log.info("Searching Adzuna for query: '{}' in country: '{}'", query, country);
            String json = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode item : results) {
                        String title = item.path("title").asText("");
                        String company = item.path("company").path("display_name").asText("");
                        String loc = item.path("location").path("display_name").asText(location);
                        String redirectUrl = item.path("redirect_url").asText("");
                        String description = item.path("description").asText("");

                        if (!title.isBlank() && !company.isBlank()) {
                            jobs.add(new DiscoveredJob(title, company, loc, redirectUrl, description, "ADZUNA"));
                        }
                    }
                }
            }
            log.info("Adzuna returned {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("Adzuna job search failed: {}", e.getMessage());
        }
        return jobs;
    }
}

