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
public class LeverJobProvider implements JobProvider {

    private static final Logger log = LoggerFactory.getLogger(LeverJobProvider.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LeverJobProvider(
            JobProviderConfigurationService configService,
            JobProviderConnectionService connectionService,
            RestClient restClient) {
        this.configService = configService;
        this.connectionService = connectionService;
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return "LEVER";
    }

    @Override
    public boolean isEnabled() {
        return configService.getConfig("LEVER")
                .map(JobProviderConfig::isEnabled)
                .orElse(false);
    }

    @Override
    public ConnectionTestResult testConnection() {
        return connectionService.testConnection("LEVER");
    }

    @Override
    public List<DiscoveredJob> search(String query, String location, int limit) {
        List<DiscoveredJob> jobs = new ArrayList<>();
        JobProviderConfig config = configService.getOrCreateConfig("LEVER");
        if (!config.isEnabled()) {
            return jobs;
        }

        List<JobProviderCompany> companies = configService.getEnabledCompanies("LEVER");
        if (companies.isEmpty()) {
            log.info("Lever search skipped: No enabled companies configured");
            return jobs;
        }

        String filterQuery = query != null ? query.toLowerCase().trim() : "";
        String filterLoc = location != null ? location.toLowerCase().trim() : "";

        for (JobProviderCompany company : companies) {
            String site = company.getBoardIdentifier().toLowerCase().trim();
            String url = "https://api.lever.co/v0/postings/" + site + "?mode=json";

            try {
                log.info("Fetching Lever postings for company '{}' (site: '{}')", company.getCompanyName(), site);
                String json = restClient.get()
                        .uri(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                if (json != null) {
                    JsonNode root = objectMapper.readTree(json);
                    if (root.isArray()) {
                        for (JsonNode item : root) {
                            String title = item.path("text").asText("");
                            String locName = item.path("categories").path("location").asText("Remote");
                            String hostedUrl = item.path("hostedUrl").asText("");
                            String desc = item.path("descriptionPlain").asText("");

                            boolean matchesQuery = filterQuery.isEmpty() || title.toLowerCase().contains(filterQuery) || desc.toLowerCase().contains(filterQuery);
                            boolean matchesLoc = filterLoc.isEmpty() || locName.toLowerCase().contains(filterLoc) || locName.equalsIgnoreCase("Remote");

                            if (matchesQuery && matchesLoc && !title.isBlank()) {
                                jobs.add(new DiscoveredJob(title, company.getCompanyName(), locName, hostedUrl, desc, "LEVER"));
                                if (limit > 0 && jobs.size() >= limit) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Lever jobs for company '{}': {}", company.getCompanyName(), e.getMessage());
            }
        }
        log.info("Lever returned total {} matching jobs", jobs.size());
        return jobs;
    }
}
