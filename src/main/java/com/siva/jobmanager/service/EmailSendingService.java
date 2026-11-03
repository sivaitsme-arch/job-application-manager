package com.siva.jobmanager.service;

import com.siva.jobmanager.model.*;
import com.siva.jobmanager.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.*;
import java.time.LocalDateTime;

@Service
public class EmailSendingService {
    private final JavaMailSender mailSender;
    private final JobApplicationRepository repository;
    @Value("${app.mail.delay-millis:3000}") private long delayMillis;
    @Value("${app.candidate.email:}") private String senderEmail;

    public EmailSendingService(JavaMailSender mailSender, JobApplicationRepository repository) {
        this.mailSender = mailSender;
        this.repository = repository;
    }

    public void sendApproved(Long id, Path resume) throws Exception {
        JobApplication job = repository.findById(id).orElseThrow(() -> 
            new IllegalArgumentException("Job application not found with ID: " + id));

        if (job.getStatus() == ApplicationStatus.SENT) {
            throw new IllegalStateException("This application was already sent.");
        }
        if (job.getEmailSubject() == null || job.getEmailBody() == null || job.getEmailBody().isBlank()) {
            throw new IllegalStateException("Generate and review the email draft first.");
        }
        if (resume == null || !Files.isRegularFile(resume)) {
            throw new IllegalArgumentException("Please select a valid resume PDF file.");
        }
        if (!resume.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Resume attachment must be a PDF file.");
        }
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException("Sender email is not configured. Please check GMAIL_USERNAME in .env or environment variables.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(job.getRecruiterEmail());
            helper.setSubject(job.getEmailSubject());
            helper.setText(job.getEmailBody(), false);
            helper.addAttachment(resume.getFileName().toString(), new FileSystemResource(resume));

            mailSender.send(message);

            recordSuccess(id, resume);
        } catch (Exception ex) {
            recordFailure(id);
            throw ex;
        }

        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Transactional
    public void recordSuccess(Long id, Path resume) {
        JobApplication job = repository.findById(id).orElse(null);
        if (job != null) {
            job.setResumePath(resume.toAbsolutePath().toString());
            job.setStatus(ApplicationStatus.SENT);
            job.setSentAt(LocalDateTime.now());
            repository.save(job);
        }
    }

    @Transactional
    public void recordFailure(Long id) {
        JobApplication job = repository.findById(id).orElse(null);
        if (job != null && job.getStatus() != ApplicationStatus.SENT) {
            job.setStatus(ApplicationStatus.FAILED);
            repository.save(job);
        }
    }
}
