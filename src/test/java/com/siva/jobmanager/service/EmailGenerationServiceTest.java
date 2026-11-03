package com.siva.jobmanager.service;

import com.siva.jobmanager.JobApplicationManagerApplication;
import com.siva.jobmanager.model.ApplicationStatus;
import com.siva.jobmanager.model.JobApplication;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EmailGenerationServiceTest {

    @Test
    void testEmailGeneration() {
        EmailGenerationService service = new EmailGenerationService();
        ReflectionTestUtils.setField(service, "candidateName", "Kette ShivaKumar");
        ReflectionTestUtils.setField(service, "candidateEmail", "sivaitsme01@gmail.com");

        JobApplication job = new JobApplication();
        job.setCompanyName("Google");
        job.setJobRole("Software Engineer");
        job.setRecruiterEmail("recruiter@google.com");

        service.generate(job);

        assertEquals("Application for Software Engineer - Kette ShivaKumar", job.getEmailSubject());
        assertTrue(job.getEmailBody().contains("Software Engineer position at Google"));
        assertTrue(job.getEmailBody().contains("Kette ShivaKumar\nsivaitsme01@gmail.com"));
        assertEquals(ApplicationStatus.READY, job.getStatus());
    }

    @Test
    void testLoadEnvironment() {
        JobApplicationManagerApplication.loadEnvironment();
        // Since .env exists with GMAIL_APP_PASSWORD having spaces, check that it is stripped
        String pass = System.getProperty("GMAIL_APP_PASSWORD");
        assertNotNull(pass);
        assertFalse(pass.contains(" "), "GMAIL_APP_PASSWORD should have spaces stripped");
        assertEquals("tjjuoqlbteuhvsyx", pass);
    }
}

