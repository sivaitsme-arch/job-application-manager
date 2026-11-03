package com.siva.jobmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_provider_credential",
       uniqueConstraints = @UniqueConstraint(name = "uk_provider_key", columnNames = {"provider_config_id", "credential_key"}))
public class JobProviderCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_config_id", nullable = false)
    private Long providerConfigId;

    @Column(name = "credential_key", nullable = false, length = 50)
    private String credentialKey;

    @Column(name = "encrypted_credential_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedCredentialValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public JobProviderCredential() {}

    public JobProviderCredential(Long providerConfigId, String credentialKey, String encryptedCredentialValue) {
        this.providerConfigId = providerConfigId;
        this.credentialKey = credentialKey;
        this.encryptedCredentialValue = encryptedCredentialValue;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }

    public String getCredentialKey() { return credentialKey; }
    public void setCredentialKey(String credentialKey) { this.credentialKey = credentialKey; }

    public String getEncryptedCredentialValue() { return encryptedCredentialValue; }
    public void setEncryptedCredentialValue(String encryptedCredentialValue) { this.encryptedCredentialValue = encryptedCredentialValue; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

