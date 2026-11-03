package com.siva.jobmanager.repository;

import com.siva.jobmanager.model.JobProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobProviderCredentialRepository extends JpaRepository<JobProviderCredential, Long> {
    List<JobProviderCredential> findByProviderConfigId(Long providerConfigId);
    Optional<JobProviderCredential> findByProviderConfigIdAndCredentialKeyIgnoreCase(Long providerConfigId, String credentialKey);
    void deleteByProviderConfigId(Long providerConfigId);
}

