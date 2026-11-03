package com.siva.jobmanager.model;

import java.util.ArrayList;
import java.util.List;

public class DiscoveredJob {
    private String title;
    private String company;
    private String location;
    private String url;
    private String description;
    private String providerName;
    private int matchPercentage;
    private List<String> matchedSkills = new ArrayList<>();
    private String experienceText;
    private String contactEmail;

    public DiscoveredJob() {}

    public DiscoveredJob(String title, String company, String location, String url, String description, String providerName) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.description = description;
        this.providerName = providerName;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { this.matchPercentage = matchPercentage; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public String getExperienceText() { return experienceText; }
    public void setExperienceText(String experienceText) { this.experienceText = experienceText; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}

