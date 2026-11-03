package com.siva.jobmanager;

import com.siva.jobmanager.config.SpringContext;
import com.siva.jobmanager.ui.DesktopApplication;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class JobApplicationManagerApplication {
    public static void main(String[] args) {
        loadEnvironment();
        ConfigurableApplicationContext context = SpringApplication.run(JobApplicationManagerApplication.class, args);
        System.setProperty("spring.context.id", context.getId());
        Application.launch(DesktopApplication.class, args);
    }

    public static void loadEnvironment() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            envPath = Paths.get(".env.example");
        }
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String val = line.substring(idx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if ("GMAIL_APP_PASSWORD".equalsIgnoreCase(key)) {
                            // Strip spaces from Google app passwords
                            val = val.replaceAll("\\s+", "");
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not load env file: " + e.getMessage());
            }
        }
        // Also sanitize GMAIL_APP_PASSWORD from environment if it contains spaces
        String envPass = System.getenv("GMAIL_APP_PASSWORD");
        if (envPass != null && envPass.contains(" ")) {
            System.setProperty("GMAIL_APP_PASSWORD", envPass.replaceAll("\\s+", ""));
        }
    }
}
