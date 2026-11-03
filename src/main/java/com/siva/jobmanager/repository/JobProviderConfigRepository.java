package com.siva.jobmanager.repository;

import com.siva.jobmanager.model.JobProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobProviderConfigRepository extends JpaRepository<JobProviderConfig, Long> {
    Optional<JobProviderConfig> findByProviderNameIgnoreCase(String providerName);
    List<JobProviderConfig> findByEnabledTrue();
    boolean existsByProviderNameIgnoreCase(String providerName);
}
