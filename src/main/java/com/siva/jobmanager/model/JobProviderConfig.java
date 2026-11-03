package com.siva.jobmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_provider_config")
public class JobProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, unique = true, length = 50)
    private String providerName;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "country_code", length = 10)
    private String countryCode = "in";

    @Column(length = 100)
    private String location = "India";

    @Column(name = "results_per_search", nullable = false)
    private int resultsPerSearch = 20;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public JobProviderConfig() {}

    public JobProviderConfig(String providerName, boolean enabled, String countryCode, String location, int resultsPerSearch) {
        this.providerName = providerName;
        this.enabled = enabled;
        this.countryCode = countryCode;
        this.location = location;
        this.resultsPerSearch = resultsPerSearch;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getResultsPerSearch() { return resultsPerSearch; }
    public void setResultsPerSearch(int resultsPerSearch) { this.resultsPerSearch = resultsPerSearch; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

