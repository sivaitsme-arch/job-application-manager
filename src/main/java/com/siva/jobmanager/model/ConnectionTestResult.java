package com.siva.jobmanager.model;

import java.time.LocalDateTime;

public class ConnectionTestResult {
    public enum Status { SUCCESS, FAILED, ERROR }

    private final String providerName;
    private final Status status;
    private final Integer httpStatusCode;
    private final String message;
    private final String details;
    private final LocalDateTime timestamp;

    public ConnectionTestResult(String providerName, Status status, Integer httpStatusCode, String message, String details) {
        this.providerName = providerName;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public static ConnectionTestResult success(String providerName, int httpStatusCode, String message) {
        return new ConnectionTestResult(providerName, Status.SUCCESS, httpStatusCode, message, null);
    }

    public static ConnectionTestResult failure(String providerName, Integer httpStatusCode, String message, String details) {
        return new ConnectionTestResult(providerName, Status.FAILED, httpStatusCode, message, details);
    }

    public static ConnectionTestResult error(String providerName, String message, String details) {
        return new ConnectionTestResult(providerName, Status.ERROR, null, message, details);
    }

    public String getProviderName() { return providerName; }
    public Status getStatus() { return status; }
    public Integer getHttpStatusCode() { return httpStatusCode; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + status + "] " + providerName + (httpStatusCode != null ? " (HTTP " + httpStatusCode + ")" : "") + ": " + message;
    }
}
