package com.siva.jobmanager.service;

import com.siva.jobmanager.model.JobProviderCredential;
import com.siva.jobmanager.repository.JobProviderCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JobProviderCredentialService {

    private static final Logger log = LoggerFactory.getLogger(JobProviderCredentialService.class);

    private final JobProviderCredentialRepository credentialRepository;
    private final JobProviderEncryptionService encryptionService;

    public JobProviderCredentialService(
            JobProviderCredentialRepository credentialRepository,
            JobProviderEncryptionService encryptionService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Saves or updates an encrypted credential for a given provider config.
     */
    @Transactional
    public void saveCredential(Long providerConfigId, String credentialKey, String plainValue) {
        if (providerConfigId == null || credentialKey == null || plainValue == null || plainValue.isBlank()) {
            return;
        }
        String encrypted = encryptionService.encrypt(plainValue.trim());
        Optional<JobProviderCredential> existing = credentialRepository
                .findByProviderConfigIdAndCredentialKeyIgnoreCase(providerConfigId, credentialKey.trim());

        if (existing.isPresent()) {
            JobProviderCredential cred = existing.get();
            cred.setEncryptedCredentialValue(encrypted);
            credentialRepository.save(cred);
            log.info("Updated encrypted credential for config id: {}, key: {}", providerConfigId, credentialKey);
        } else {
            JobProviderCredential cred = new JobProviderCredential(providerConfigId, credentialKey.trim().toUpperCase(), encrypted);
            credentialRepository.save(cred);
            log.info("Saved new encrypted credential for config id: {}, key: {}", providerConfigId, credentialKey);
        }
    }

    /**
     * Retrieves and decrypts a specific credential for a provider.
     */
    @Transactional(readOnly = true)
    public Optional<String> getDecryptedCredential(Long providerConfigId, String credentialKey) {
        if (providerConfigId == null || credentialKey == null) {
            return Optional.empty();
        }
        return credentialRepository.findByProviderConfigIdAndCredentialKeyIgnoreCase(providerConfigId, credentialKey.trim())
                .map(cred -> encryptionService.decrypt(cred.getEncryptedCredentialValue()));
    }

    /**
     * Retrieves all decrypted credentials for a provider as a Key-Value map.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllDecryptedCredentials(Long providerConfigId) {
        Map<String, String> result = new HashMap<>();
        if (providerConfigId == null) {
            return result;
        }
        List<JobProviderCredential> creds = credentialRepository.findByProviderConfigId(providerConfigId);
        for (JobProviderCredential cred : creds) {
            try {
                String plain = encryptionService.decrypt(cred.getEncryptedCredentialValue());
                result.put(cred.getCredentialKey().toUpperCase(), plain);
            } catch (Exception e) {
                log.error("Failed to decrypt credential key {} for config id {}", cred.getCredentialKey(), providerConfigId);
            }
        }
        return result;
    }

    /**
     * Checks if a credential exists for the provider.
     */
    @Transactional(readOnly = true)
    public boolean hasCredential(Long providerConfigId, String credentialKey) {
        return credentialRepository.findByProviderConfigIdAndCredentialKeyIgnoreCase(providerConfigId, credentialKey).isPresent();
    }

    /**
     * Deletes credentials for a provider.
     */
    @Transactional
    public void deleteCredentials(Long providerConfigId) {
        credentialRepository.deleteByProviderConfigId(providerConfigId);
        log.info("Deleted credentials for config id: {}", providerConfigId);
    }
}
