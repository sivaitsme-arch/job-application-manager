package com.siva.jobmanager.service;

import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import com.siva.jobmanager.repository.JobProviderCompanyRepository;
import com.siva.jobmanager.repository.JobProviderConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobProviderConfigurationServiceTest {

    private JobProviderConfigRepository configRepository;
    private JobProviderCompanyRepository companyRepository;
    private JobProviderCredentialService credentialService;
    private JobProviderConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configRepository = Mockito.mock(JobProviderConfigRepository.class);
        companyRepository = Mockito.mock(JobProviderCompanyRepository.class);
        credentialService = Mockito.mock(JobProviderCredentialService.class);
        configurationService = new JobProviderConfigurationService(configRepository, companyRepository, credentialService);
    }

    @Test
    void testSaveAndGetConfig() {
        JobProviderConfig existing = new JobProviderConfig("ADZUNA", false, "in", "India", 30);
        when(configRepository.findByProviderNameIgnoreCase("ADZUNA")).thenReturn(Optional.of(existing));
        when(configRepository.save(any(JobProviderConfig.class))).thenAnswer(i -> i.getArgument(0));

        JobProviderConfig saved = configurationService.saveConfig("ADZUNA", true, "in", "India", 50);

        assertTrue(saved.isEnabled());
        assertEquals(50, saved.getResultsPerSearch());
        verify(configRepository, atLeastOnce()).save(any(JobProviderConfig.class));
    }

    @Test
    void testToggleProvider() {
        JobProviderConfig config = new JobProviderConfig("JOOBLE", false, "in", "India", 20);
        when(configRepository.findByProviderNameIgnoreCase("JOOBLE")).thenReturn(Optional.of(config));
        when(configRepository.save(any(JobProviderConfig.class))).thenAnswer(i -> i.getArgument(0));

        JobProviderConfig toggled = configurationService.toggleProvider("JOOBLE", true);
        assertTrue(toggled.isEnabled());
    }

    @Test
    void testAddCompanyAndDuplicatePrevention() {
        when(companyRepository.existsByProviderNameIgnoreCaseAndBoardIdentifierIgnoreCase("GREENHOUSE", "stripe"))
                .thenReturn(false);
        when(companyRepository.save(any(JobProviderCompany.class))).thenAnswer(i -> i.getArgument(0));

        JobProviderCompany comp = configurationService.addCompany("GREENHOUSE", "Stripe", "stripe");
        assertEquals("Stripe", comp.getCompanyName());
        assertEquals("stripe", comp.getBoardIdentifier());

        // Duplicate addition attempt
        when(companyRepository.existsByProviderNameIgnoreCaseAndBoardIdentifierIgnoreCase("GREENHOUSE", "stripe"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                configurationService.addCompany("GREENHOUSE", "Stripe Inc", "stripe")
        );
    }

    @Test
    void testRemoveCompany() {
        configurationService.removeCompany(42L);
        verify(companyRepository, times(1)).deleteById(42L);
    }
}

