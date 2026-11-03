package com.siva.jobmanager.service;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.DiscoveredJob;
import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobDiscoveryServiceTest {

    private JobProvider adzunaProvider;
    private JobProvider joobleProvider;
    private JobApplicationRepository applicationRepository;
    private JobDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        adzunaProvider = Mockito.mock(JobProvider.class);
        joobleProvider = Mockito.mock(JobProvider.class);
        applicationRepository = Mockito.mock(JobApplicationRepository.class);

        when(adzunaProvider.getProviderName()).thenReturn("ADZUNA");
        when(joobleProvider.getProviderName()).thenReturn("JOOBLE");

        discoveryService = new JobDiscoveryService(List.of(adzunaProvider, joobleProvider), applicationRepository);
    }

    @Test
    void testDynamicProviderSelectionSkipsDisabled() {
        when(adzunaProvider.isEnabled()).thenReturn(true);
        when(joobleProvider.isEnabled()).thenReturn(false);

        DiscoveredJob job = new DiscoveredJob("Java Developer", "Oracle", "Bangalore", "https://oracle.com", "Spring Boot required", "ADZUNA");
        when(adzunaProvider.search(any(), any(), anyInt())).thenReturn(List.of(job));

        when(applicationRepository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(any(), any(), any()))
                .thenReturn(false);

        JobDiscoveryService.DiscoveryResult result = discoveryService.discoverJobs("Java", "India");

        assertEquals(1, result.matchedJobs().size());
        assertEquals("Disabled", result.providerStatuses().get("JOOBLE"));
        assertTrue(result.providerStatuses().get("ADZUNA").contains("Success"));
        verify(joobleProvider, never()).search(any(), any(), anyInt());
        verify(adzunaProvider, times(1)).search(any(), any(), anyInt());
    }

    @Test
    void testProviderFailureIsolation() {
        when(adzunaProvider.isEnabled()).thenReturn(true);
        when(joobleProvider.isEnabled()).thenReturn(true);

        when(adzunaProvider.search(any(), any(), anyInt())).thenThrow(new RuntimeException("Adzuna network timeout"));
        DiscoveredJob joobleJob = new DiscoveredJob("Backend Engineer", "Amazon", "Hyderabad", "https://amazon.jobs", "Java developer with 4 years experience", "JOOBLE");
        when(joobleProvider.search(any(), any(), anyInt())).thenReturn(List.of(joobleJob));

        when(applicationRepository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(any(), any(), any()))
                .thenReturn(false);

        JobDiscoveryService.DiscoveryResult result = discoveryService.discoverJobs("Java", "India");

        // Despite Adzuna throwing an exception, Jooble's job is retrieved and returned!
        assertEquals(1, result.matchedJobs().size());
        assertEquals("Backend Engineer", result.matchedJobs().get(0).getTitle());
        assertTrue(result.providerStatuses().get("ADZUNA").startsWith("Error"));
        assertTrue(result.providerStatuses().get("JOOBLE").startsWith("Success"));
    }

    @Test
    void testMatchScoringAndSkillDetection() {
        DiscoveredJob job = new DiscoveredJob(
                "Senior Java Engineer",
                "Google",
                "Hyderabad",
                "https://careers.google.com",
                "Requires 4 years experience in Java, Spring Boot, MySQL, REST, and Docker.",
                "GREENHOUSE"
        );

        int score = discoveryService.calculateMatch(job, Set.of("Java", "Spring Boot", "MySQL", "Docker"), 3, 5);

        assertTrue(score >= 70, "Score should be high due to matching skills, title and experience");
        assertTrue(job.getMatchedSkills().contains("Java"));
        assertTrue(job.getMatchedSkills().contains("Spring Boot"));
    }

    @Test
    void testImportToApplications() {
        DiscoveredJob job = new DiscoveredJob("Java Developer", "Netflix", "Remote", "https://netflix.jobs/1", "Spring Boot", "LEVER");
        job.setContactEmail("jobs@netflix.com");

        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        JobApplication app = discoveryService.importToApplications(job);
        assertNotNull(app);
        assertEquals("Netflix", app.getCompanyName());
        assertEquals("Java Developer", app.getJobRole());
        assertEquals("jobs@netflix.com", app.getRecruiterEmail());
        verify(applicationRepository, times(1)).save(any(JobApplication.class));
    }
}

