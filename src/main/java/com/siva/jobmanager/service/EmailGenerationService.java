package com.siva.jobmanager.service;

import com.siva.jobmanager.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailGenerationService {
    @Value("${app.candidate.name:Your Name}") private String candidateName;
    @Value("${app.candidate.email:}") private String candidateEmail;

    public void generate(JobApplication job) {
        job.setEmailSubject("Application for " + job.getJobRole() + " - " + candidateName);
        StringBuilder sb = new StringBuilder();
        sb.append("Dear Hiring Team,\n\n");
        sb.append("I am writing to express my interest in the ").append(job.getJobRole())
          .append(" position at ").append(job.getCompanyName()).append(".\n\n");
        sb.append("I have approximately 4 years of experience in software development and would appreciate ")
          .append("the opportunity to discuss how my experience could contribute to your team.\n\n");
        sb.append("Please find my resume attached for your consideration. I would be happy to provide any additional information.\n\n");
        sb.append("Thank you for your time.\n\n");
        sb.append("Regards,\n").append(candidateName);
        if (candidateEmail != null && !candidateEmail.isBlank()) {
            sb.append("\n").append(candidateEmail);
        }
        job.setEmailBody(sb.toString());
        job.setStatus(ApplicationStatus.READY);
    }
}
