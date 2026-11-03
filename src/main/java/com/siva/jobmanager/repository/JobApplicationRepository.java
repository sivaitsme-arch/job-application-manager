package com.siva.jobmanager.repository;

import com.siva.jobmanager.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(String company,String role,String email);
    List<JobApplication> findAllByOrderByCreatedAtDesc();
    long countByStatus(ApplicationStatus status);
}
