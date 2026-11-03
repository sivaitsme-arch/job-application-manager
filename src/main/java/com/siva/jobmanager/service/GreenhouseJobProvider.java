package com.siva.jobmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.DiscoveredJob;
import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GreenhouseJobProvider implements JobProvider {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseJobProvider.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GreenhouseJobProvider(
            JobProviderConfigurationService configService,
            JobProviderConnectionService connectionService,
            RestClient restClient) {
        this.configService = configService;
        this.connectionService = connectionService;
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "GREENHOUSE";
    }

    @Override
    public boolean isEnabled() {
        return configService.getConfig("GREENHOUSE")
                .map(JobProviderConfig::isEnabled)
                .orElse(false);
    }

    @Override
    public ConnectionTestResult testConnection() {
        return connectionService.testConnection("GREENHOUSE");
    }

    @Override
    public List<DiscoveredJob> search(String query, String location, int limit) {
        List<DiscoveredJob> jobs = new ArrayList<>();
        JobProviderConfig config = configService.getOrCreateConfig("GREENHOUSE");
        if (!config.isEnabled()) {
            return jobs;
        }

        List<JobProviderCompany> companies = configService.getEnabledCompanies("GREENHOUSE");
        if (companies.isEmpty()) {
            log.info("Greenhouse search skipped: No enabled companies configured");
            return jobs;
        }

        String filterQuery = query != null ? query.toLowerCase().trim() : "";
        String filterLoc = location != null ? location.toLowerCase().trim() : "";

        for (JobProviderCompany company : companies) {
            String token = company.getBoardIdentifier().toLowerCase().trim();
            String url = "https://boards-api.greenhouse.io/v1/boards/" + token + "/jobs?content=true";

            try {
                log.info("Fetching Greenhouse jobs for company '{}' (board: '{}')", company.getCompanyName(), token);
                String json = restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                if (json != null) {
                    JsonNode root = objectMapper.readTree(json);
                    JsonNode jobsArray = root.path("jobs");
                    if (jobsArray.isArray()) {
                        for (JsonNode item : jobsArray) {
                            String title = item.path("title").asText("");
                            String locName = item.path("location").path("name").asText("Remote");
                            String absUrl = item.path("absolute_url").asText("");
                            String content = item.path("content").asText("");

                            // Filter by query/location if specified
                            boolean matchesQuery = filterQuery.isEmpty() || title.toLowerCase().contains(filterQuery) || content.toLowerCase().contains(filterQuery);
                            boolean matchesLoc = filterLoc.isEmpty() || locName.toLowerCase().contains(filterLoc) || locName.equalsIgnoreCase("Remote");

                            if (matchesQuery && matchesLoc && !title.isBlank()) {
                                jobs.add(new DiscoveredJob(title, company.getCompanyName(), locName, absUrl, content, "GREENHOUSE"));
                                if (limit > 0 && jobs.size() >= limit) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Greenhouse jobs for company '{}': {}", company.getCompanyName(), e.getMessage());
            }
        }
        log.info("Greenhouse returned total {} matching jobs", jobs.size());
        return jobs;
    }
}
