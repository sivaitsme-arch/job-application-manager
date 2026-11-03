package com.siva.jobmanager.service;

import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import com.siva.jobmanager.repository.JobProviderCompanyRepository;
import com.siva.jobmanager.repository.JobProviderConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class JobProviderConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(JobProviderConfigurationService.class);

    private final JobProviderConfigRepository configRepository;
    private final JobProviderCompanyRepository companyRepository;
    private final JobProviderCredentialService credentialService;

    public JobProviderConfigurationService(
            JobProviderConfigRepository configRepository,
            JobProviderCompanyRepository companyRepository,
            JobProviderCredentialService credentialService) {
        this.configRepository = configRepository;
        this.companyRepository = companyRepository;
        this.credentialService = credentialService;
    }

    /**
     * Retrieves or creates default configuration for a given provider.
     */
    @Transactional
    public JobProviderConfig getOrCreateConfig(String providerName) {
        String name = providerName.trim().toUpperCase();
        return configRepository.findByProviderNameIgnoreCase(name)
                .orElseGet(() -> {
                    JobProviderConfig newConfig = new JobProviderConfig(
                            name,
                            false,
                            "in",
                            name.equals("GREENHOUSE") || name.equals("LEVER") ? "Global/Remote" : "India",
                            name.equals("ADZUNA") ? 30 : (name.equals("JOOBLE") ? 20 : 25)
                    );
                    log.info("Created initial configuration for provider: {}", name);
                    return configRepository.save(newConfig);
                });
    }

    @Transactional(readOnly = true)
    public Optional<JobProviderConfig> getConfig(String providerName) {
        return configRepository.findByProviderNameIgnoreCase(providerName.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<JobProviderConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JobProviderConfig> getEnabledConfigs() {
        return configRepository.findByEnabledTrue();
    }

    /**
     * Saves or updates configuration parameters for a provider.
     */
    @Transactional
    public JobProviderConfig saveConfig(String providerName, boolean enabled, String countryCode, String location, int resultsPerSearch) {
        JobProviderConfig config = getOrCreateConfig(providerName);
        config.setEnabled(enabled);
        if (countryCode != null && !countryCode.isBlank()) {
            config.setCountryCode(countryCode.trim().toLowerCase());
        }
        if (location != null && !location.isBlank()) {
            config.setLocation(location.trim());
        }
        if (resultsPerSearch > 0) {
            config.setResultsPerSearch(resultsPerSearch);
        }
        JobProviderConfig saved = configRepository.save(config);
        log.info("Saved configuration for provider {}: enabled={}, country={}, location={}, resultsLimit={}",
                saved.getProviderName(), saved.isEnabled(), saved.getCountryCode(), saved.getLocation(), saved.getResultsPerSearch());
        return saved;
    }

    /**
     * Toggles provider enabled status.
     */
    @Transactional
    public JobProviderConfig toggleProvider(String providerName, boolean enabled) {
        JobProviderConfig config = getOrCreateConfig(providerName);
        config.setEnabled(enabled);
        JobProviderConfig saved = configRepository.save(config);
        log.info("Provider {} enabled state set to: {}", providerName, enabled);
        return saved;
    }

    // ==================== Company Board Management (Greenhouse / Lever) ====================

    @Transactional(readOnly = true)
    public List<JobProviderCompany> getCompanies(String providerName) {
        return companyRepository.findByProviderNameIgnoreCaseOrderByCompanyNameAsc(providerName.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<JobProviderCompany> getEnabledCompanies(String providerName) {
        return companyRepository.findByProviderNameIgnoreCaseAndEnabledTrue(providerName.trim().toUpperCase());
    }

    @Transactional
    public JobProviderCompany addCompany(String providerName, String companyName, String boardIdentifier) {
        String pName = providerName.trim().toUpperCase();
        String cName = companyName.trim();
        String bId = boardIdentifier.trim().toLowerCase();

        if (companyRepository.existsByProviderNameIgnoreCaseAndBoardIdentifierIgnoreCase(pName, bId)) {
            throw new IllegalArgumentException("A company with board identifier '" + bId + "' already exists for " + pName);
        }

        JobProviderCompany company = new JobProviderCompany(pName, cName, bId, true);
        JobProviderCompany saved = companyRepository.save(company);
        log.info("Added company board for {}: {} ({})", pName, cName, bId);
        return saved;
    }

    @Transactional
    public void removeCompany(Long companyId) {
        companyRepository.deleteById(companyId);
        log.info("Removed company board with ID: {}", companyId);
    }

    @Transactional
    public JobProviderCompany toggleCompany(Long companyId, boolean enabled) {
        JobProviderCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + companyId));
        company.setEnabled(enabled);
        JobProviderCompany saved = companyRepository.save(company);
        log.info("Toggled company {} enabled to: {}", company.getCompanyName(), enabled);
        return saved;
    }
}
