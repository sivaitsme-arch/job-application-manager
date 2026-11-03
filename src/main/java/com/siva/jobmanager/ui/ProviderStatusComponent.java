package com.siva.jobmanager.ui;

import com.siva.jobmanager.model.ConnectionTestResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class ProviderStatusComponent extends VBox {

    private final Label nameLabel;
    private final Label enabledBadge;
    private final Label connectionBadge;
    private final Label detailsLabel;

    public ProviderStatusComponent(String providerName) {
        setSpacing(6);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");

        nameLabel = new Label(providerName);
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        enabledBadge = new Label("Disabled");
        enabledBadge.setStyle("-fx-background-color: #475569; -fx-text-fill: #cbd5e1; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        connectionBadge = new Label("Not Tested");
        connectionBadge.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        HBox badges = new HBox(8, nameLabel, enabledBadge, connectionBadge);
        badges.setAlignment(Pos.CENTER_LEFT);

        detailsLabel = new Label("Configure and test this provider below.");
        detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        detailsLabel.setWrapText(true);

        getChildren().addAll(badges, detailsLabel);
    }

    public void updateStatus(boolean enabled, ConnectionTestResult testResult) {
        if (enabled) {
            enabledBadge.setText("Active");
            enabledBadge.setStyle("-fx-background-color: #166534; -fx-text-fill: #bbf7d0; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            enabledBadge.setText("Disabled");
            enabledBadge.setStyle("-fx-background-color: #475569; -fx-text-fill: #cbd5e1; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        }

        if (testResult != null) {
            String time = testResult.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (testResult.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                connectionBadge.setText("Connected (" + time + ")");
                connectionBadge.setStyle("-fx-background-color: #065f46; -fx-text-fill: #a7f3d0; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                detailsLabel.setStyle("-fx-text-fill: #86efac; -fx-font-size: 12px;");
            } else {
                connectionBadge.setText("Failed (" + time + ")");
                connectionBadge.setStyle("-fx-background-color: #991b1b; -fx-text-fill: #fecaca; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                detailsLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");
            }
            detailsLabel.setText(testResult.getMessage());
        }
    }

    public void setTesting(String message) {
        connectionBadge.setText("Testing...");
        connectionBadge.setStyle("-fx-background-color: #1e40af; -fx-text-fill: #bfdbfe; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        detailsLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");
        detailsLabel.setText(message);
    }
}

