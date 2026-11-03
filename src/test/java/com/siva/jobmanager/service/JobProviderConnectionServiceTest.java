package com.siva.jobmanager.service;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.JobProviderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JobProviderConnectionServiceTest {

    private JobProviderConfigurationService configService;
    private JobProviderCredentialService credentialService;
    private JobProviderConnectionService connectionService;

    @BeforeEach
    void setUp() {
        configService = Mockito.mock(JobProviderConfigurationService.class);
        credentialService = Mockito.mock(JobProviderCredentialService.class);
        connectionService = new JobProviderConnectionService(configService, credentialService);
    }

    @Test
    void testAdzunaMissingCredentialsReturnsFailure() {
        JobProviderConfig cfg = new JobProviderConfig("ADZUNA", true, "in", "India", 30);
        cfg.setId(1L);
        when(configService.getOrCreateConfig("ADZUNA")).thenReturn(cfg);
        when(credentialService.getAllDecryptedCredentials(1L)).thenReturn(Collections.emptyMap());

        ConnectionTestResult result = connectionService.testConnection("ADZUNA");
        assertEquals(ConnectionTestResult.Status.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("Application ID is missing") || result.getMessage().contains("missing"));
    }

    @Test
    void testJoobleMissingKeyReturnsFailure() {
        JobProviderConfig cfg = new JobProviderConfig("JOOBLE", true, "in", "India", 20);
        cfg.setId(2L);
        when(configService.getOrCreateConfig("JOOBLE")).thenReturn(cfg);
        when(credentialService.getAllDecryptedCredentials(2L)).thenReturn(Collections.emptyMap());

        ConnectionTestResult result = connectionService.testConnection("JOOBLE");
        assertEquals(ConnectionTestResult.Status.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("API Key is missing"));
    }

    @Test
    void testGreenhouseNoCompaniesReturnsFailure() {
        JobProviderConfig cfg = new JobProviderConfig("GREENHOUSE", true, "in", "Global", 25);
        cfg.setId(3L);
        when(configService.getOrCreateConfig("GREENHOUSE")).thenReturn(cfg);
        when(configService.getEnabledCompanies("GREENHOUSE")).thenReturn(Collections.emptyList());

        ConnectionTestResult result = connectionService.testConnection("GREENHOUSE");
        assertEquals(ConnectionTestResult.Status.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("No companies configured"));
    }

    @Test
    void testLeverNoCompaniesReturnsFailure() {
        JobProviderConfig cfg = new JobProviderConfig("LEVER", true, "in", "Global", 25);
        cfg.setId(4L);
        when(configService.getOrCreateConfig("LEVER")).thenReturn(cfg);
        when(configService.getEnabledCompanies("LEVER")).thenReturn(Collections.emptyList());

        ConnectionTestResult result = connectionService.testConnection("LEVER");
        assertEquals(ConnectionTestResult.Status.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("No companies configured"));
    }
}
