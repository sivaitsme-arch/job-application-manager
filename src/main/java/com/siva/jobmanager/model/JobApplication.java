package com.siva.jobmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_application",
 uniqueConstraints = @UniqueConstraint(name = "uk_company_role_email",
 columnNames = {"company_name", "job_role", "recruiter_email"}))
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="company_name", nullable=false) private String companyName;
    @Column(name="job_role", nullable=false) private String jobRole;
    @Column(name="recruiter_email", nullable=false) private String recruiterEmail;
    private String location;
    @Column(length=1000) private String jobUrl;
    @Enumerated(EnumType.STRING) private ApplicationStatus status = ApplicationStatus.IMPORTED;
    @Column(length=300) private String emailSubject;
    @Column(columnDefinition="TEXT") private String emailBody;
    @Column(length=1000) private String resumePath;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;}
    public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;}
    public String getJobRole(){return jobRole;} public void setJobRole(String v){jobRole=v;}
    public String getRecruiterEmail(){return recruiterEmail;} public void setRecruiterEmail(String v){recruiterEmail=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getJobUrl(){return jobUrl;} public void setJobUrl(String v){jobUrl=v;}
    public ApplicationStatus getStatus(){return status;} public void setStatus(ApplicationStatus v){status=v;}
    public String getEmailSubject(){return emailSubject;} public void setEmailSubject(String v){emailSubject=v;}
    public String getEmailBody(){return emailBody;} public void setEmailBody(String v){emailBody=v;}
    public String getResumePath(){return resumePath;} public void setResumePath(String v){resumePath=v;}
    public LocalDateTime getSentAt(){return sentAt;} public void setSentAt(LocalDateTime v){sentAt=v;}
}
