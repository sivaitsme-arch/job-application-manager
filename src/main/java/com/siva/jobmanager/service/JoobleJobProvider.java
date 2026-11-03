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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JoobleJobProvider implements JobProvider {

    private static final Logger log = LoggerFactory.getLogger(JoobleJobProvider.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderCredentialService credentialService;
    private final JobProviderConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JoobleJobProvider(
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
        return "JOOBLE";
    }

    @Override
    public boolean isEnabled() {
        return configService.getConfig("JOOBLE")
                .map(JobProviderConfig::isEnabled)
                .orElse(false);
    }

    @Override
    public ConnectionTestResult testConnection() {
        return connectionService.testConnection("JOOBLE");
    }

    @Override
    public List<DiscoveredJob> search(String query, String location, int limit) {
        List<DiscoveredJob> jobs = new ArrayList<>();
        JobProviderConfig config = configService.getOrCreateConfig("JOOBLE");
        if (!config.isEnabled()) {
            return jobs;
        }

        Map<String, String> creds = credentialService.getAllDecryptedCredentials(config.getId());
        String apiKey = creds.get("API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Jooble search skipped: API key not configured");
            return jobs;
        }

        int pageSize = limit > 0 ? limit : config.getResultsPerSearch();
        String loc = (location != null && !location.isBlank()) ? location : config.getLocation();
        String kw = (query != null && !query.isBlank()) ? query : "software developer";

        String url = "https://jooble.org/api/" + apiKey.trim();
        String requestJson = String.format("{\"keywords\":\"%s\",\"location\":\"%s\",\"page\":1,\"resultonpage\":%d}",
                kw.replace("\"", "\\\""), loc.replace("\"", "\\\""), pageSize);

        try {
            log.info("Searching Jooble for keywords: '{}' in location: '{}'", kw, loc);
            String json = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode jobsArray = root.path("jobs");
                if (jobsArray.isArray()) {
                    for (JsonNode item : jobsArray) {
                        String title = item.path("title").asText("");
                        String company = item.path("company").asText("");
                        String itemLoc = item.path("location").asText(loc);
                        String link = item.path("link").asText("");
                        String snippet = item.path("snippet").asText("");

                        if (!title.isBlank() && !company.isBlank()) {
                            jobs.add(new DiscoveredJob(title, company, itemLoc, link, snippet, "JOOBLE"));
                        }
                    }
                }
            }
            log.info("Jooble returned {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("Jooble job search failed: {}", e.getMessage());
        }
        return jobs;
    }
}

