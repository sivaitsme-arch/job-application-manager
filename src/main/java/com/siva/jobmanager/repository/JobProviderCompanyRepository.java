package com.siva.jobmanager.repository;

import com.siva.jobmanager.model.JobProviderCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobProviderCompanyRepository extends JpaRepository<JobProviderCompany, Long> {
    List<JobProviderCompany> findByProviderNameIgnoreCaseOrderByCompanyNameAsc(String providerName);
    List<JobProviderCompany> findByProviderNameIgnoreCaseAndEnabledTrue(String providerName);
    boolean existsByProviderNameIgnoreCaseAndBoardIdentifierIgnoreCase(String providerName, String boardIdentifier);
    boolean existsByProviderNameIgnoreCaseAndCompanyNameIgnoreCase(String providerName, String companyName);
    Optional<JobProviderCompany> findByProviderNameIgnoreCaseAndBoardIdentifierIgnoreCase(String providerName, String boardIdentifier);
}
