package com.siva.jobmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_provider_company",
       uniqueConstraints = @UniqueConstraint(name = "uk_provider_company_board", columnNames = {"provider_name", "board_identifier"}))
public class JobProviderCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "board_identifier", nullable = false, length = 100)
    private String boardIdentifier;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public JobProviderCompany() {}

    public JobProviderCompany(String providerName, String companyName, String boardIdentifier, boolean enabled) {
        this.providerName = providerName;
        this.companyName = companyName;
        this.boardIdentifier = boardIdentifier;
        this.enabled = enabled;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getBoardIdentifier() { return boardIdentifier; }
    public void setBoardIdentifier(String boardIdentifier) { this.boardIdentifier = boardIdentifier; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

