package com.siva.jobmanager.service;

import com.siva.jobmanager.model.ApplicationStatus;
import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.repository.JobApplicationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailSendingServiceTest {

    private JavaMailSender mailSender;
    private JobApplicationRepository repository;
    private EmailSendingService sendingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        repository = Mockito.mock(JobApplicationRepository.class);
        sendingService = new EmailSendingService(mailSender, repository);
        ReflectionTestUtils.setField(sendingService, "delayMillis", 0L);
        ReflectionTestUtils.setField(sendingService, "senderEmail", "sender@example.com");

        Session session = Session.getInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
    }

    @Test
    void testSendApprovedSuccess() throws Exception {
        JobApplication job = new JobApplication();
        job.setCompanyName("Acme Corp");
        job.setJobRole("Developer");
        job.setRecruiterEmail("recruiter@acme.com");
        job.setEmailSubject("Application for Developer");
        job.setEmailBody("Hello, here is my application.");
        job.setStatus(ApplicationStatus.READY);

        when(repository.findById(1L)).thenReturn(Optional.of(job));

        Path resumePdf = tempDir.resolve("resume.pdf");
        Files.writeString(resumePdf, "%PDF-1.4 dummy pdf content");

        sendingService.sendApproved(1L, resumePdf);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertEquals(ApplicationStatus.SENT, job.getStatus());
        assertNotNull(job.getSentAt());
        assertEquals(resumePdf.toAbsolutePath().toString(), job.getResumePath());
    }

    @Test
    void testSendApprovedFailureUpdatesStatusToFailed() throws Exception {
        JobApplication job = new JobApplication();
        job.setCompanyName("Acme Corp");
        job.setJobRole("Developer");
        job.setRecruiterEmail("recruiter@acme.com");
        job.setEmailSubject("Application for Developer");
        job.setEmailBody("Hello, here is my application.");
        job.setStatus(ApplicationStatus.READY);

        when(repository.findById(1L)).thenReturn(Optional.of(job));
        doThrow(new RuntimeException("SMTP connection failed")).when(mailSender).send(any(MimeMessage.class));

        Path resumePdf = tempDir.resolve("resume.pdf");
        Files.writeString(resumePdf, "%PDF-1.4 dummy pdf content");

        Exception ex = assertThrows(RuntimeException.class, () -> sendingService.sendApproved(1L, resumePdf));
        assertEquals("SMTP connection failed", ex.getMessage());
        assertEquals(ApplicationStatus.FAILED, job.getStatus());
    }
}

