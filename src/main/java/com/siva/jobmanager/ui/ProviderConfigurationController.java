package com.siva.jobmanager.ui;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import com.siva.jobmanager.service.JobProviderConfigurationService;
import com.siva.jobmanager.service.JobProviderConnectionService;
import com.siva.jobmanager.service.JobProviderCredentialService;
import com.siva.jobmanager.service.JobProviderEncryptionService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProviderConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigurationController.class);

    private final JobProviderConfigurationService configService;
    private final JobProviderCredentialService credentialService;
    private final JobProviderConnectionService connectionService;

    public ProviderConfigurationController(
            JobProviderConfigurationService configService,
            JobProviderCredentialService credentialService,
            JobProviderConnectionService connectionService) {
        this.configService = configService;
        this.credentialService = credentialService;
        this.connectionService = connectionService;
    }

    public JobProviderConfig getConfig(String providerName) {
        return configService.getOrCreateConfig(providerName);
    }

    public Map<String, String> getCredentials(String providerName) {
        JobProviderConfig config = configService.getOrCreateConfig(providerName);
        return credentialService.getAllDecryptedCredentials(config.getId());
    }

    public boolean hasCredential(String providerName, String key) {
        JobProviderConfig config = configService.getOrCreateConfig(providerName);
        return credentialService.hasCredential(config.getId(), key);
    }

    public String getMaskedCredential(String providerName, String key) {
        JobProviderConfig config = configService.getOrCreateConfig(providerName);
        return credentialService.getDecryptedCredential(config.getId(), key)
                .map(JobProviderEncryptionService::mask)
                .orElse("");
    }

    public void saveAdzunaConfig(boolean enabled, String appId, String appKey, String countryCode, int resultsLimit) {
        JobProviderConfig config = configService.saveConfig("ADZUNA", enabled, countryCode, "India", resultsLimit);
        if (appId != null && !appId.isBlank()) {
            credentialService.saveCredential(config.getId(), "APP_ID", appId.trim());
        }
        if (appKey != null && !appKey.isBlank() && !appKey.startsWith("••••")) {
            credentialService.saveCredential(config.getId(), "APP_KEY", appKey.trim());
        }
    }

    public void saveJoobleConfig(boolean enabled, String apiKey, String location, int resultsLimit) {
        JobProviderConfig config = configService.saveConfig("JOOBLE", enabled, "in", location, resultsLimit);
        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("••••")) {
            credentialService.saveCredential(config.getId(), "API_KEY", apiKey.trim());
        }
    }

    public void saveGreenhouseConfig(boolean enabled, int resultsLimit) {
        configService.saveConfig("GREENHOUSE", enabled, "in", "Global/Remote", resultsLimit);
    }

    public void saveLeverConfig(boolean enabled, int resultsLimit) {
        configService.saveConfig("LEVER", enabled, "in", "Global/Remote", resultsLimit);
    }

    public List<JobProviderCompany> getCompanies(String providerName) {
        return configService.getCompanies(providerName);
    }

    public JobProviderCompany addCompany(String providerName, String companyName, String boardIdentifier) {
        return configService.addCompany(providerName, companyName, boardIdentifier);
    }

    public void removeCompany(Long companyId) {
        configService.removeCompany(companyId);
    }

    public JobProviderCompany toggleCompany(Long companyId, boolean enabled) {
        return configService.toggleCompany(companyId, enabled);
    }

    /**
     * Executes connection test in background JavaFX Task.
     */
    public void testConnectionAsync(String providerName, Consumer<ConnectionTestResult> onResult, Consumer<Throwable> onError) {
        Task<ConnectionTestResult> task = new Task<>() {
            @Override
            protected ConnectionTestResult call() {
                return connectionService.testConnection(providerName);
            }
        };
        task.setOnSucceeded(e -> onResult.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        new Thread(task, "TestConnection-" + providerName).start();
    }

    /**
     * Executes single company board test in background JavaFX Task.
     */
    public void testCompanyBoardAsync(String providerName, String boardIdentifier, String companyName,
                                      Consumer<ConnectionTestResult> onResult, Consumer<Throwable> onError) {
        Task<ConnectionTestResult> task = new Task<>() {
            @Override
            protected ConnectionTestResult call() {
                if ("GREENHOUSE".equalsIgnoreCase(providerName)) {
                    return connectionService.testGreenhouseBoard(boardIdentifier, companyName);
                } else {
                    return connectionService.testLeverSite(boardIdentifier, companyName);
                }
            }
        };
        task.setOnSucceeded(e -> onResult.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        new Thread(task, "TestBoard-" + providerName + "-" + boardIdentifier).start();
    }
}

