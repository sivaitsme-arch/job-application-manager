package com.siva.jobmanager.service;

import com.siva.jobmanager.model.*;
import com.siva.jobmanager.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class JobApplicationService {
    private final JobApplicationRepository repository;
    private final EmailGenerationService generator;

    public JobApplicationService(JobApplicationRepository repository, EmailGenerationService generator) {
        this.repository = repository;
        this.generator = generator;
    }

    public List<JobApplication> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public JobApplication generate(Long id) {
        JobApplication job = repository.findById(id).orElseThrow(() -> 
            new IllegalArgumentException("Job application not found with ID: " + id));
        if (job.getStatus() == ApplicationStatus.SENT) {
            throw new IllegalStateException("This application was already sent.");
        }
        generator.generate(job);
        return repository.save(job);
    }

    @Transactional
    public JobApplication updateDraft(Long id, String subject, String body) {
        JobApplication job = repository.findById(id).orElseThrow(() -> 
            new IllegalArgumentException("Job application not found with ID: " + id));
        if (job.getStatus() == ApplicationStatus.SENT) {
            throw new IllegalStateException("This application was already sent.");
        }
        job.setEmailSubject(subject);
        job.setEmailBody(body);
        job.setStatus(ApplicationStatus.READY);
        return repository.save(job);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void markFailed(Long id) {
        JobApplication job = repository.findById(id).orElse(null);
        if (job != null && job.getStatus() != ApplicationStatus.SENT) {
            job.setStatus(ApplicationStatus.FAILED);
            repository.save(job);
        }
    }

    public long count(ApplicationStatus status) {
        return repository.countByStatus(status);
    }
}
