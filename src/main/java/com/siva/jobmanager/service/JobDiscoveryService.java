package com.siva.jobmanager.service;

import com.siva.jobmanager.model.DiscoveredJob;
import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.repository.JobApplicationRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(JobDiscoveryService.class);

    private static final List<String> KNOWN_TECH_SKILLS = List.of(
            "Java", "Spring", "Spring Boot", "Hibernate", "JPA", "MySQL", "PostgreSQL",
            "REST", "RESTful", "Microservices", "Docker", "Kubernetes", "AWS", "Git",
            "Maven", "Kafka", "Redis", "CI/CD", "Linux", "JavaScript", "React", "Angular"
    );

    private final List<JobProvider> providers;
    private final JobApplicationRepository applicationRepository;

    @Value("${app.resume.directory:./data/resumes}")
    private String resumeDirectory;

    public JobDiscoveryService(List<JobProvider> providers, JobApplicationRepository applicationRepository) {
        this.providers = providers;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Executes job discovery across all enabled providers, matching against candidate resume skills and experience.
     */
    public DiscoveryResult discoverJobs(String searchKeyword, String locationFilter) {
        log.info("Starting job discovery. Keyword: '{}', Location: '{}'", searchKeyword, locationFilter);

        Set<String> candidateSkills = extractResumeSkills();
        int candidateMinExp = 3;
        int candidateMaxExp = 5;

        List<DiscoveredJob> allJobs = new ArrayList<>();
        Map<String, String> providerStatusMessages = new HashMap<>();

        for (JobProvider provider : providers) {
            String pName = provider.getProviderName();
            if (!provider.isEnabled()) {
                log.info("Provider {} is disabled. Skipping.", pName);
                providerStatusMessages.put(pName, "Disabled");
                continue;
            }

            try {
                log.info("Querying enabled provider: {}", pName);
                List<DiscoveredJob> found = provider.search(searchKeyword, locationFilter, 30);
                allJobs.addAll(found);
                providerStatusMessages.put(pName, "Success (" + found.size() + " jobs found)");
            } catch (Exception ex) {
                log.error("Job provider search failed for provider: {}", pName, ex);
                providerStatusMessages.put(pName, "Error: " + ex.getMessage());
            }
        }

        // Filter duplicates and score matching jobs
        List<DiscoveredJob> matchedJobs = new ArrayList<>();
        for (DiscoveredJob job : allJobs) {
            if (isDuplicate(job)) {
                continue;
            }
            int matchScore = calculateMatch(job, candidateSkills, candidateMinExp, candidateMaxExp);
            job.setMatchPercentage(matchScore);
            matchedJobs.add(job);
        }

        // Sort descending by match score
        matchedJobs.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));

        log.info("Discovery complete. Total retrieved: {}, unique candidates: {}", allJobs.size(), matchedJobs.size());
        return new DiscoveryResult(matchedJobs, providerStatusMessages, candidateSkills);
    }

    /**
     * Calculates match percentage based on matched technical skills, job title, and experience suitability.
     */
    public int calculateMatch(DiscoveredJob job, Set<String> candidateSkills, int minExp, int maxExp) {
        int score = 40; // Base score for discovery match

        String text = (job.getTitle() + " " + job.getDescription()).toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();

        for (String skill : candidateSkills) {
            if (text.contains(skill.toLowerCase(Locale.ROOT))) {
                matched.add(skill);
                score += 5;
            }
        }
        job.setMatchedSkills(matched);

        // Experience match check (looking for 3-5 years)
        Pattern expPattern = Pattern.compile("(\\d+)\\s*(?:-|to)?\\s*(\\d+)?\\s*(?:\\+|plus)?\\s*years?", Pattern.CASE_INSENSITIVE);
        Matcher m = expPattern.matcher(text);
        if (m.find()) {
            try {
                int foundMin = Integer.parseInt(m.group(1));
                job.setExperienceText(m.group(0));
                if (foundMin >= minExp && foundMin <= maxExp) {
                    score += 15;
                } else if (foundMin < minExp) {
                    score += 10;
                }
            } catch (Exception ignored) {}
        } else {
            job.setExperienceText("3-5 years (estimated)");
        }

        // Title relevancy bonus
        String title = job.getTitle().toLowerCase(Locale.ROOT);
        if (title.contains("developer") || title.contains("engineer") || title.contains("java") || title.contains("backend")) {
            score += 10;
        }

        return Math.min(score, 98);
    }

    /**
     * Prevents duplicate job applications.
     */
    private boolean isDuplicate(DiscoveredJob job) {
        if (job.getCompany() == null || job.getTitle() == null) {
            return false;
        }
        String email = job.getContactEmail() != null ? job.getContactEmail() : "careers@" + sanitizeDomain(job.getCompany());
        return applicationRepository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(
                job.getCompany(), job.getTitle(), email);
    }

    private String sanitizeDomain(String company) {
        return company.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") + ".com";
    }

    /**
     * Extracts skills from resume PDF using PDFBox.
     */
    public Set<String> extractResumeSkills() {
        Set<String> skills = new HashSet<>(List.of("Java", "Spring Boot", "MySQL", "REST", "Git", "Maven", "Hibernate"));
        try {
            Path dir = Paths.get(resumeDirectory);
            if (!Files.exists(dir)) {
                return skills;
            }
            try (var stream = Files.list(dir)) {
                Optional<Path> pdfFile = stream
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .findFirst();

                if (pdfFile.isPresent()) {
                    File file = pdfFile.get().toFile();
                    try (PDDocument doc = Loader.loadPDF(file)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(doc).toLowerCase(Locale.ROOT);
                        for (String tech : KNOWN_TECH_SKILLS) {
                            if (text.contains(tech.toLowerCase(Locale.ROOT))) {
                                skills.add(tech);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract skills from resume: {}", e.getMessage());
        }
        return skills;
    }

    /**
     * Imports a discovered job directly into the JobApplication table.
     */
    public JobApplication importToApplications(DiscoveredJob job) {
        String email = job.getContactEmail() != null && !job.getContactEmail().isBlank()
                ? job.getContactEmail()
                : "careers@" + sanitizeDomain(job.getCompany());

        JobApplication app = new JobApplication();
        app.setCompanyName(job.getCompany());
        app.setJobRole(job.getTitle());
        app.setRecruiterEmail(email);
        app.setLocation(job.getLocation());
        app.setJobUrl(job.getUrl());
        return applicationRepository.save(app);
    }

    public record DiscoveryResult(
            List<DiscoveredJob> matchedJobs,
            Map<String, String> providerStatuses,
            Set<String> candidateSkills
    ) {}
}
