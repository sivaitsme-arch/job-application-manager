package com.siva.jobmanager.ui;

import com.siva.jobmanager.model.ConnectionTestResult;
import com.siva.jobmanager.model.JobProviderCompany;
import com.siva.jobmanager.model.JobProviderConfig;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

public class JobDiscoverySettingsView extends VBox {

    private final ProviderConfigurationController controller;

    private final ProviderStatusComponent adzunaStatus = new ProviderStatusComponent("Adzuna API");
    private final ProviderStatusComponent joobleStatus = new ProviderStatusComponent("Jooble API");
    private final ProviderStatusComponent greenhouseStatus = new ProviderStatusComponent("Greenhouse Boards");
    private final ProviderStatusComponent leverStatus = new ProviderStatusComponent("Lever Postings");

    public JobDiscoverySettingsView(ProviderConfigurationController controller) {
        this.controller = controller;

        setSpacing(14);
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #0f172a;");

        // Header
        Label title = new Label("Job Discovery Settings");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subtitle = new Label("Configure, validate, and test credentials and company board endpoints for job discovery providers.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(0, 0, 8, 0));

        // TabPane for Providers
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab adzunaTab = new Tab("Adzuna", createAdzunaCard());
        Tab joobleTab = new Tab("Jooble", createJoobleCard());
        Tab greenhouseTab = new Tab("Greenhouse", createGreenhouseCard());
        Tab leverTab = new Tab("Lever", createLeverCard());

        tabPane.getTabs().addAll(adzunaTab, joobleTab, greenhouseTab, leverTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().addAll(header, tabPane);
    }

    public void selectProviderTab(String providerName) {
        // Can be called to deep-link to a specific tab if needed
    }

    // ==================== Adzuna Configuration Card ====================
    private ScrollPane createAdzunaCard() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        JobProviderConfig cfg = controller.getConfig("ADZUNA");
        String currentAppId = controller.getCredentials("ADZUNA").getOrDefault("APP_ID", "");
        String currentMaskedKey = controller.getMaskedCredential("ADZUNA", "APP_KEY");

        CheckBox enableCheck = new CheckBox("Enable Adzuna Provider");
        enableCheck.setSelected(cfg.isEnabled());
        enableCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField appIdField = createStyledTextField(currentAppId, "Enter Adzuna App ID");
        PasswordField appKeyField = createStyledPasswordField("Enter Adzuna App Key");
        if (!currentMaskedKey.isEmpty()) {
            appKeyField.setPromptText("Saved: " + currentMaskedKey + " (Leave blank to keep)");
        }

        TextField countryField = createStyledTextField(cfg.getCountryCode() != null ? cfg.getCountryCode() : "in", "Country Code (default: in)");
        TextField limitField = createStyledTextField(String.valueOf(cfg.getResultsPerSearch() > 0 ? cfg.getResultsPerSearch() : 30), "30");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, createFieldLabel("Application ID:"), appIdField);
        grid.addRow(1, createFieldLabel("Application Key:"), appKeyField);
        grid.addRow(2, createFieldLabel("Country Code:"), countryField);
        grid.addRow(3, createFieldLabel("Results Limit:"), limitField);

        Button saveBtn = createAccentButton("Save Configuration");
        Button testBtn = createSecondaryButton("Test Connection");
        Button resetBtn = createOutlineButton("Reset");

        adzunaStatus.updateStatus(cfg.isEnabled(), null);

        saveBtn.setOnAction(e -> {
            try {
                int limit = Integer.parseInt(limitField.getText().trim());
                controller.saveAdzunaConfig(enableCheck.isSelected(), appIdField.getText(), appKeyField.getText(), countryField.getText(), limit);
                adzunaStatus.updateStatus(enableCheck.isSelected(), null);
                showInfoAlert("Adzuna Saved", "Adzuna configuration and encrypted credentials saved successfully.");
            } catch (Exception ex) {
                showErrorAlert("Save Failed", ex.getMessage());
            }
        });

        testBtn.setOnAction(e -> {
            adzunaStatus.setTesting("Executing test query for India jobs on Adzuna API...");
            testBtn.setDisable(true);
            controller.testConnectionAsync("ADZUNA", result -> {
                testBtn.setDisable(false);
                adzunaStatus.updateStatus(enableCheck.isSelected(), result);
                if (result.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                    showInfoAlert("Adzuna Connection Succeeded", result.getMessage());
                } else {
                    showErrorAlert("Adzuna Connection Failed", result.getMessage() + (result.getDetails() != null ? "\n\nDetails: " + result.getDetails() : ""));
                }
            }, error -> {
                testBtn.setDisable(false);
                adzunaStatus.updateStatus(enableCheck.isSelected(), ConnectionTestResult.error("ADZUNA", error.getMessage(), null));
                showErrorAlert("Test Error", error.getMessage());
            });
        });

        resetBtn.setOnAction(e -> {
            JobProviderConfig fresh = controller.getConfig("ADZUNA");
            enableCheck.setSelected(fresh.isEnabled());
            countryField.setText(fresh.getCountryCode());
            limitField.setText(String.valueOf(fresh.getResultsPerSearch()));
            appIdField.setText(controller.getCredentials("ADZUNA").getOrDefault("APP_ID", ""));
            appKeyField.clear();
        });

        HBox actions = new HBox(10, saveBtn, testBtn, resetBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(adzunaStatus, enableCheck, grid, actions);
        return wrapInScrollPane(box);
    }

    // ==================== Jooble Configuration Card ====================
    private ScrollPane createJoobleCard() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        JobProviderConfig cfg = controller.getConfig("JOOBLE");
        String currentMaskedKey = controller.getMaskedCredential("JOOBLE", "API_KEY");

        CheckBox enableCheck = new CheckBox("Enable Jooble Provider");
        enableCheck.setSelected(cfg.isEnabled());
        enableCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        PasswordField apiKeyField = createStyledPasswordField("Enter Jooble API Key");
        if (!currentMaskedKey.isEmpty()) {
            apiKeyField.setPromptText("Saved: " + currentMaskedKey + " (Leave blank to keep)");
        }

        TextField locField = createStyledTextField(cfg.getLocation() != null ? cfg.getLocation() : "India", "Location (default: India)");
        TextField limitField = createStyledTextField(String.valueOf(cfg.getResultsPerSearch() > 0 ? cfg.getResultsPerSearch() : 20), "20");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, createFieldLabel("API Key:"), apiKeyField);
        grid.addRow(1, createFieldLabel("Search Location:"), locField);
        grid.addRow(2, createFieldLabel("Results Limit:"), limitField);

        Button saveBtn = createAccentButton("Save Configuration");
        Button testBtn = createSecondaryButton("Test Connection");
        Button resetBtn = createOutlineButton("Reset");

        joobleStatus.updateStatus(cfg.isEnabled(), null);

        saveBtn.setOnAction(e -> {
            try {
                int limit = Integer.parseInt(limitField.getText().trim());
                controller.saveJoobleConfig(enableCheck.isSelected(), apiKeyField.getText(), locField.getText(), limit);
                joobleStatus.updateStatus(enableCheck.isSelected(), null);
                showInfoAlert("Jooble Saved", "Jooble configuration and encrypted credentials saved successfully.");
            } catch (Exception ex) {
                showErrorAlert("Save Failed", ex.getMessage());
            }
        });

        testBtn.setOnAction(e -> {
            joobleStatus.setTesting("Executing test query on Jooble API...");
            testBtn.setDisable(true);
            controller.testConnectionAsync("JOOBLE", result -> {
                testBtn.setDisable(false);
                joobleStatus.updateStatus(enableCheck.isSelected(), result);
                if (result.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                    showInfoAlert("Jooble Connection Succeeded", result.getMessage());
                } else {
                    showErrorAlert("Jooble Connection Failed", result.getMessage() + (result.getDetails() != null ? "\n\nDetails: " + result.getDetails() : ""));
                }
            }, error -> {
                testBtn.setDisable(false);
                joobleStatus.updateStatus(enableCheck.isSelected(), ConnectionTestResult.error("JOOBLE", error.getMessage(), null));
                showErrorAlert("Test Error", error.getMessage());
            });
        });

        resetBtn.setOnAction(e -> {
            JobProviderConfig fresh = controller.getConfig("JOOBLE");
            enableCheck.setSelected(fresh.isEnabled());
            locField.setText(fresh.getLocation());
            limitField.setText(String.valueOf(fresh.getResultsPerSearch()));
            apiKeyField.clear();
        });

        HBox actions = new HBox(10, saveBtn, testBtn, resetBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(joobleStatus, enableCheck, grid, actions);
        return wrapInScrollPane(box);
    }

    // ==================== Greenhouse Configuration Card ====================
    private ScrollPane createGreenhouseCard() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        JobProviderConfig cfg = controller.getConfig("GREENHOUSE");

        CheckBox enableCheck = new CheckBox("Enable Greenhouse Provider");
        enableCheck.setSelected(cfg.isEnabled());
        enableCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField limitField = createStyledTextField(String.valueOf(cfg.getResultsPerSearch() > 0 ? cfg.getResultsPerSearch() : 25), "25");
        HBox limitBox = new HBox(10, createFieldLabel("Results Limit per Board:"), limitField);
        limitBox.setAlignment(Pos.CENTER_LEFT);

        greenhouseStatus.updateStatus(cfg.isEnabled(), null);

        // Configured Companies Table
        ObservableList<JobProviderCompany> companies = FXCollections.observableArrayList(controller.getCompanies("GREENHOUSE"));
        TableView<JobProviderCompany> table = createCompanyTableView("GREENHOUSE", companies);

        // Add Company Row
        TextField newCompanyField = createStyledTextField("", "e.g., Stripe, GitLab, Airbnb");
        TextField newBoardField = createStyledTextField("", "Board Token (e.g., stripe)");
        Button addCompanyBtn = createSecondaryButton("Add Company Board");

        addCompanyBtn.setOnAction(e -> {
            String cName = newCompanyField.getText().trim();
            String bToken = newBoardField.getText().trim();
            if (cName.isEmpty() || bToken.isEmpty()) {
                showErrorAlert("Validation Error", "Please provide both Company Name and Board Token.");
                return;
            }
            try {
                JobProviderCompany created = controller.addCompany("GREENHOUSE", cName, bToken);
                companies.add(created);
                newCompanyField.clear();
                newBoardField.clear();
            } catch (Exception ex) {
                showErrorAlert("Could Not Add Company", ex.getMessage());
            }
        });

        HBox addBox = new HBox(10, newCompanyField, newBoardField, addCompanyBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = createAccentButton("Save Configuration");
        Button testAllBtn = createSecondaryButton("Test All Configured Boards");

        saveBtn.setOnAction(e -> {
            try {
                int limit = Integer.parseInt(limitField.getText().trim());
                controller.saveGreenhouseConfig(enableCheck.isSelected(), limit);
                greenhouseStatus.updateStatus(enableCheck.isSelected(), null);
                showInfoAlert("Greenhouse Saved", "Greenhouse configuration saved successfully.");
            } catch (Exception ex) {
                showErrorAlert("Save Failed", ex.getMessage());
            }
        });

        testAllBtn.setOnAction(e -> {
            greenhouseStatus.setTesting("Testing public API endpoints for all configured Greenhouse boards...");
            testAllBtn.setDisable(true);
            controller.testConnectionAsync("GREENHOUSE", result -> {
                testAllBtn.setDisable(false);
                greenhouseStatus.updateStatus(enableCheck.isSelected(), result);
                if (result.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                    showInfoAlert("Greenhouse Boards Verified", result.getMessage());
                } else {
                    showErrorAlert("Greenhouse Test Results", result.getMessage());
                }
            }, error -> {
                testAllBtn.setDisable(false);
                greenhouseStatus.updateStatus(enableCheck.isSelected(), ConnectionTestResult.error("GREENHOUSE", error.getMessage(), null));
                showErrorAlert("Test Error", error.getMessage());
            });
        });

        HBox actions = new HBox(10, saveBtn, testAllBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("Configured Greenhouse Company Boards (Public APIs):");
        tableTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        box.getChildren().addAll(greenhouseStatus, enableCheck, limitBox, new Separator(), tableTitle, addBox, table, actions);
        return wrapInScrollPane(box);
    }

    // ==================== Lever Configuration Card ====================
    private ScrollPane createLeverCard() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");

        JobProviderConfig cfg = controller.getConfig("LEVER");

        CheckBox enableCheck = new CheckBox("Enable Lever Provider");
        enableCheck.setSelected(cfg.isEnabled());
        enableCheck.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField limitField = createStyledTextField(String.valueOf(cfg.getResultsPerSearch() > 0 ? cfg.getResultsPerSearch() : 25), "25");
        HBox limitBox = new HBox(10, createFieldLabel("Results Limit per Company:"), limitField);
        limitBox.setAlignment(Pos.CENTER_LEFT);

        leverStatus.updateStatus(cfg.isEnabled(), null);

        // Configured Companies Table
        ObservableList<JobProviderCompany> companies = FXCollections.observableArrayList(controller.getCompanies("LEVER"));
        TableView<JobProviderCompany> table = createCompanyTableView("LEVER", companies);

        // Add Company Row
        TextField newCompanyField = createStyledTextField("", "e.g., Netflix, Figma, Spotify");
        TextField newSiteField = createStyledTextField("", "Site Identifier (e.g., netflix)");
        Button addCompanyBtn = createSecondaryButton("Add Company Site");

        addCompanyBtn.setOnAction(e -> {
            String cName = newCompanyField.getText().trim();
            String sId = newSiteField.getText().trim();
            if (cName.isEmpty() || sId.isEmpty()) {
                showErrorAlert("Validation Error", "Please provide both Company Name and Site Identifier.");
                return;
            }
            try {
                JobProviderCompany created = controller.addCompany("LEVER", cName, sId);
                companies.add(created);
                newCompanyField.clear();
                newSiteField.clear();
            } catch (Exception ex) {
                showErrorAlert("Could Not Add Company", ex.getMessage());
            }
        });

        HBox addBox = new HBox(10, newCompanyField, newSiteField, addCompanyBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = createAccentButton("Save Configuration");
        Button testAllBtn = createSecondaryButton("Test All Configured Sites");

        saveBtn.setOnAction(e -> {
            try {
                int limit = Integer.parseInt(limitField.getText().trim());
                controller.saveLeverConfig(enableCheck.isSelected(), limit);
                leverStatus.updateStatus(enableCheck.isSelected(), null);
                showInfoAlert("Lever Saved", "Lever configuration saved successfully.");
            } catch (Exception ex) {
                showErrorAlert("Save Failed", ex.getMessage());
            }
        });

        testAllBtn.setOnAction(e -> {
            leverStatus.setTesting("Testing public postings endpoints for all configured Lever company sites...");
            testAllBtn.setDisable(true);
            controller.testConnectionAsync("LEVER", result -> {
                testAllBtn.setDisable(false);
                leverStatus.updateStatus(enableCheck.isSelected(), result);
                if (result.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                    showInfoAlert("Lever Sites Verified", result.getMessage());
                } else {
                    showErrorAlert("Lever Test Results", result.getMessage());
                }
            }, error -> {
                testAllBtn.setDisable(false);
                leverStatus.updateStatus(enableCheck.isSelected(), ConnectionTestResult.error("LEVER", error.getMessage(), null));
                showErrorAlert("Test Error", error.getMessage());
            });
        });

        HBox actions = new HBox(10, saveBtn, testAllBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("Configured Lever Company Sites (Public APIs):");
        tableTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        box.getChildren().addAll(leverStatus, enableCheck, limitBox, new Separator(), tableTitle, addBox, table, actions);
        return wrapInScrollPane(box);
    }

    // ==================== Reusable Company TableView ====================
    private TableView<JobProviderCompany> createCompanyTableView(String providerName, ObservableList<JobProviderCompany> items) {
        TableView<JobProviderCompany> table = new TableView<>(items);
        table.setPrefHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc;");

        TableColumn<JobProviderCompany, String> nameCol = new TableColumn<>("Company Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCompanyName()));

        TableColumn<JobProviderCompany, String> identCol = new TableColumn<>(
                "GREENHOUSE".equalsIgnoreCase(providerName) ? "Board Token" : "Site Identifier");
        identCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBoardIdentifier()));

        TableColumn<JobProviderCompany, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isEnabled()));
        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(index -> {
            JobProviderCompany comp = items.get(index);
            SimpleBooleanProperty prop = new SimpleBooleanProperty(comp.isEnabled());
            prop.addListener((obs, oldVal, newVal) -> {
                controller.toggleCompany(comp.getId(), newVal);
                comp.setEnabled(newVal);
            });
            return prop;
        }));
        activeCol.setMaxWidth(80);

        TableColumn<JobProviderCompany, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button testBtn = new Button("Test");
            private final Button deleteBtn = new Button("Remove");
            private final HBox pane = new HBox(6, testBtn, deleteBtn);
            {
                testBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-size: 11px;");
                deleteBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-size: 11px;");
                pane.setAlignment(Pos.CENTER);

                testBtn.setOnAction(e -> {
                    JobProviderCompany item = getTableView().getItems().get(getIndex());
                    testBtn.setDisable(true);
                    controller.testCompanyBoardAsync(providerName, item.getBoardIdentifier(), item.getCompanyName(),
                            res -> {
                                testBtn.setDisable(false);
                                if (res.getStatus() == ConnectionTestResult.Status.SUCCESS) {
                                    showInfoAlert("Connection Successful", res.getMessage());
                                } else {
                                    showErrorAlert("Connection Failed", res.getMessage());
                                }
                            },
                            err -> {
                                testBtn.setDisable(false);
                                showErrorAlert("Test Error", err.getMessage());
                            });
                });

                deleteBtn.setOnAction(e -> {
                    JobProviderCompany item = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Remove '" + item.getCompanyName() + "' (" + item.getBoardIdentifier() + ") from " + providerName + "?",
                            ButtonType.CANCEL, ButtonType.OK);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText(null);
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        controller.removeCompany(item.getId());
                        items.remove(item);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(nameCol, identCol, activeCol, actionCol);
        return table;
    }

    // ==================== UI Helpers & Styling ====================
    private static Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold; -fx-min-width: 140px;");
        return lbl;
    }

    private static TextField createStyledTextField(String initialValue, String prompt) {
        TextField tf = new TextField(initialValue);
        tf.setPromptText(prompt);
        tf.setPrefWidth(320);
        tf.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc; -fx-border-color: #334155; -fx-border-radius: 4; -fx-padding: 6 10;");
        return tf;
    }

    private static PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefWidth(320);
        pf.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc; -fx-border-color: #334155; -fx-border-radius: 4; -fx-padding: 6 10;");
        return pf;
    }

    private static Button createAccentButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        return btn;
    }

    private static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-padding: 8 16; -fx-background-radius: 6;");
        return btn;
    }

    private static Button createOutlineButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-text-fill: #cbd5e1; -fx-padding: 7 14; -fx-border-radius: 6;");
        return btn;
    }

    private static ScrollPane wrapInScrollPane(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

