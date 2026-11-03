package com.siva.jobmanager.service;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.DiscoveredJob;

import java.util.List;

public interface JobProvider {
    String getProviderName();
    boolean isEnabled();
    ConnectionTestResult testConnection();
    List<DiscoveredJob> search(String query, String location, int limit);
}
