package com.siva.jobmanager.ui;

import com.siva.jobmanager.config.SpringContext;
import com.siva.jobmanager.model.ApplicationStatus;
import com.siva.jobmanager.model.DiscoveredJob;
import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.model.JobProviderConfig;
import com.siva.jobmanager.repository.JobApplicationRepository;
import com.siva.jobmanager.service.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DesktopApplication extends Application {

    // Data lists
    private final ObservableList<JobApplication> applicationRows = FXCollections.observableArrayList();
    private final ObservableList<DiscoveredJob> discoveredJobRows = FXCollections.observableArrayList();

    // Services
    private JobApplicationService jobs;
    private ExcelProcessingService excel;
    private EmailSendingService sender;
    private JobApplicationRepository repository;
    private JobProviderConfigurationService configService;
    private JobProviderCredentialService credentialService;
    private JobProviderConnectionService connectionService;
    private JobDiscoveryService discoveryService;
    private ProviderConfigurationController providerController;

    // UI Structure
    private BorderPane rootLayout;
    private StackPane contentArea;
    private Label globalStatus;
    private ProgressIndicator globalProgress;

    // Active Resume
    private Path resume;
    private Label resumeLabel;

    // View caches
    private Node dashboardView;
    private Node resumeProfileView;
    private Node discoveryView;
    private JobDiscoverySettingsView settingsView;
    private Node excelManagementView;
    private Node emailApplicationsView;

    @Override
    public void start(Stage stage) {
        initializeServices();

        stage.setTitle("Job Application Manager");

        // Top / Bottom global elements
        globalProgress = new ProgressIndicator();
        globalProgress.setPrefSize(18, 18);
        globalProgress.setVisible(false);

        globalStatus = new Label("Ready");
        globalStatus.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        HBox statusBar = new HBox(10, globalProgress, globalStatus);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6, 16, 6, 16));
        statusBar.setStyle("-fx-background-color: #0b132b; -fx-border-color: #1e293b; -fx-border-width: 1 0 0 0;");

        // Auto-detect resume
        autoDetectResume();

        // Main Layout
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #0f172a;");

        // Sidebar Navigation
        VBox sidebar = buildSidebar(stage);
        rootLayout.setLeft(sidebar);

        // Content Area
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f172a;");
        rootLayout.setCenter(contentArea);
        rootLayout.setBottom(statusBar);

        // Initialize Views
        dashboardView = buildDashboardView();
        resumeProfileView = buildResumeProfileView(stage);
        discoveryView = buildJobDiscoveryView();
        settingsView = new JobDiscoverySettingsView(providerController);
        excelManagementView = buildExcelManagementView(stage);
        emailApplicationsView = buildEmailApplicationsView(stage);

        // Show Dashboard initially
        showView(dashboardView);

        Scene scene = new Scene(rootLayout, 1260, 740);
        stage.setScene(scene);
        stage.show();

        refreshApplications();
    }

    private void initializeServices() {
        jobs = SpringContext.getBean(JobApplicationService.class);
        excel = SpringContext.getBean(ExcelProcessingService.class);
        sender = SpringContext.getBean(EmailSendingService.class);
        repository = SpringContext.getBean(JobApplicationRepository.class);
        configService = SpringContext.getBean(JobProviderConfigurationService.class);
        credentialService = SpringContext.getBean(JobProviderCredentialService.class);
        connectionService = SpringContext.getBean(JobProviderConnectionService.class);
        discoveryService = SpringContext.getBean(JobDiscoveryService.class);
        providerController = new ProviderConfigurationController(configService, credentialService, connectionService);
    }

    @Override
    public void stop() {
        if (SpringContext.getContext() instanceof ConfigurableApplicationContext cac) {
            cac.close();
        }
    }

    // ==================== Navigation Sidebar ====================
    private VBox buildSidebar(Stage stage) {
        VBox sidebar = new VBox(6);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(16, 10, 16, 10));
        sidebar.setStyle("-fx-background-color: #0b132b; -fx-border-color: #1e293b; -fx-border-width: 0 1 0 0;");

        // Title Header
        Label appTitle = new Label("Job Application");
        appTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label appSub = new Label("Manager Enterprise");
        appSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        VBox titleBox = new VBox(2, appTitle, appSub);
        titleBox.setPadding(new Insets(4, 8, 16, 8));

        sidebar.getChildren().add(titleBox);

        // Navigation Items
        addNavSectionHeader(sidebar, "OVERVIEW");
        Button dashBtn = createNavButton("Dashboard");
        Button profileBtn = createNavButton("Resume Profile");

        addNavSectionHeader(sidebar, "JOB DISCOVERY");
        Button discBtn = createNavButton("Discover Jobs");

        addNavSectionHeader(sidebar, "CONFIGURATION");
        Button settingsBtn = createNavButton("Job Discovery Settings");

        addNavSectionHeader(sidebar, "APPLICATIONS");
        Button excelBtn = createNavButton("Excel Management");
        Button emailBtn = createNavButton("Email Applications");

        dashBtn.setOnAction(e -> {
            dashboardView = buildDashboardView();
            showView(dashboardView);
        });
        profileBtn.setOnAction(e -> showView(resumeProfileView));
        discBtn.setOnAction(e -> showView(discoveryView));
        settingsBtn.setOnAction(e -> showView(settingsView));
        excelBtn.setOnAction(e -> showView(excelManagementView));
        emailBtn.setOnAction(e -> showView(emailApplicationsView));

        sidebar.getChildren().addAll(dashBtn, profileBtn, discBtn, settingsBtn, excelBtn, emailBtn);

        // Bottom Resume Pill
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        resumeLabel = new Label(resume != null ? resume.getFileName().toString() : "No resume selected");
        resumeLabel.setStyle("-fx-text-fill: #7dd3fc; -fx-font-size: 11px;");
        resumeLabel.setWrapText(true);

        Button changeResumeBtn = new Button("Change Resume");
        changeResumeBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-padding: 4 8;");
        changeResumeBtn.setOnAction(e -> selectResume(stage));

        VBox resumeBox = new VBox(4, new Label("Active Resume:"), resumeLabel, changeResumeBtn);
        resumeBox.setStyle("-fx-background-color: #162032; -fx-padding: 8; -fx-background-radius: 6;");

        sidebar.getChildren().addAll(spacer, resumeBox);
        return sidebar;
    }

    private void addNavSectionHeader(VBox sidebar, String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 10 8 4 8;");
        sidebar.getChildren().add(lbl);
    }

    private Button createNavButton(String title) {
        Button btn = new Button(title);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 6;"));
        return btn;
    }

    private void showView(Node view) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
    }

    // ==================== View: Dashboard ====================
    private Node buildDashboardView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Dashboard Overview");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        long totalCount = repository.count();
        long readyCount = repository.countByStatus(ApplicationStatus.READY);
        long sentCount = repository.countByStatus(ApplicationStatus.SENT);
        long failedCount = repository.countByStatus(ApplicationStatus.FAILED);
        List<JobProviderConfig> enabledConfigs = configService.getEnabledConfigs();

        HBox statsBox = new HBox(14);
        statsBox.getChildren().addAll(
                createStatCard("Total Applications", String.valueOf(totalCount), "#38bdf8"),
                createStatCard("Ready to Send", String.valueOf(readyCount), "#60a5fa"),
                createStatCard("Successfully Sent", String.valueOf(sentCount), "#4ade80"),
                createStatCard("Failed Applications", String.valueOf(failedCount), "#f87171"),
                createStatCard("Active Providers", enabledConfigs.size() + " / 4", "#c084fc")
        );

        Label quickTitle = new Label("Quick Actions");
        quickTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-padding: 10 0 4 0;");

        Button goDiscovery = new Button("Search & Discover Jobs");
        goDiscovery.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 18; -fx-background-radius: 6;");
        goDiscovery.setOnAction(e -> showView(discoveryView));

        Button goSettings = new Button("Configure Job Providers");
        goSettings.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-padding: 10 18; -fx-background-radius: 6;");
        goSettings.setOnAction(e -> showView(settingsView));

        Button goEmail = new Button("Review & Send Applications");
        goEmail.setStyle("-fx-background-color: #166534; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 18; -fx-background-radius: 6;");
        goEmail.setOnAction(e -> showView(emailApplicationsView));

        HBox actionRow = new HBox(12, goDiscovery, goSettings, goEmail);

        box.getChildren().addAll(title, statsBox, quickTitle, actionRow);
        return box;
    }

    private VBox createStatCard(String label, String value, String colorHex) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");

        Label vLbl = new Label(value);
        vLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        Label lLbl = new Label(label);
        lLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(vLbl, lLbl);
        return card;
    }

    // ==================== View: Resume Profile ====================
    private Node buildResumeProfileView(Stage stage) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Resume Profile & Skill Extraction");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label sub = new Label("Extracted candidate profile and technical keywords used for matching job discovery results.");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        Button selectBtn = new Button("Select Different Resume PDF");
        selectBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 16;");
        selectBtn.setOnAction(e -> {
            selectResume(stage);
            showView(buildResumeProfileView(stage));
        });

        Set<String> skills = discoveryService.extractResumeSkills();
        FlowPane chips = new FlowPane(8, 8);
        for (String skill : skills) {
            Label chip = new Label(skill);
            chip.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-padding: 6 12; -fx-background-radius: 16; -fx-border-color: #0284c7; -fx-border-radius: 16;");
            chips.getChildren().add(chip);
        }

        VBox card = new VBox(10, new Label("Detected Technical Skills:"), chips);
        card.setStyle("-fx-background-color: #1e293b; -fx-padding: 16; -fx-background-radius: 8;");

        box.getChildren().addAll(title, sub, selectBtn, card);
        return box;
    }

    // ==================== View: Job Discovery ====================
    private Node buildJobDiscoveryView() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Job Discovery");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        TextField kwField = new TextField("Java Developer");
        kwField.setPromptText("Job Keyword / Role (e.g., Java Developer)");
        kwField.setPrefWidth(260);
        kwField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-border-color: #334155; -fx-padding: 8;");

        TextField locField = new TextField("India");
        locField.setPromptText("Location (e.g., India or Remote)");
        locField.setPrefWidth(180);
        locField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-border-color: #334155; -fx-padding: 8;");

        Button searchBtn = new Button("Discover Matching Jobs");
        searchBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        HBox searchRow = new HBox(10, kwField, locField, searchBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        // Discovered Jobs Table
        TableView<DiscoveredJob> table = new TableView<>(discoveredJobRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc;");

        TableColumn<DiscoveredJob, String> compCol = new TableColumn<>("Company");
        compCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCompany()));

        TableColumn<DiscoveredJob, String> roleCol = new TableColumn<>("Role / Title");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<DiscoveredJob, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocation()));

        TableColumn<DiscoveredJob, String> provCol = new TableColumn<>("Provider");
        provCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProviderName()));
        provCol.setMaxWidth(100);

        TableColumn<DiscoveredJob, String> matchCol = new TableColumn<>("Match %");
        matchCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMatchPercentage() + "%"));
        matchCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                }
            }
        });
        matchCol.setMaxWidth(90);

        TableColumn<DiscoveredJob, Void> actCol = new TableColumn<>("Action");
        actCol.setCellFactory(c -> new TableCell<>() {
            private final Button addBtn = new Button("Add to Applications");
            {
                addBtn.setStyle("-fx-background-color: #166534; -fx-text-fill: white; -fx-font-size: 11px;");
                addBtn.setOnAction(e -> {
                    DiscoveredJob item = getTableView().getItems().get(getIndex());
                    try {
                        discoveryService.importToApplications(item);
                        refreshApplications();
                        addBtn.setText("Added!");
                        addBtn.setDisable(true);
                        info("Job at " + item.getCompany() + " queued for email application!");
                    } catch (Exception ex) {
                        error("Could not add job", ex);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : addBtn);
            }
        });
        actCol.setMaxWidth(160);

        table.getColumns().addAll(compCol, roleCol, locCol, provCol, matchCol, actCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        searchBtn.setOnAction(e -> {
            String kw = kwField.getText().trim();
            String loc = locField.getText().trim();
            searchBtn.setDisable(true);
            setGlobalBusy(true, "Searching across enabled providers...");

            Task<JobDiscoveryService.DiscoveryResult> task = new Task<>() {
                @Override
                protected JobDiscoveryService.DiscoveryResult call() {
                    return discoveryService.discoverJobs(kw, loc);
                }
            };
            task.setOnSucceeded(ev -> {
                searchBtn.setDisable(false);
                setGlobalBusy(false, "Search complete.");
                JobDiscoveryService.DiscoveryResult res = task.getValue();
                discoveredJobRows.setAll(res.matchedJobs());
                info("Discovered " + res.matchedJobs().size() + " matching jobs from active providers.");
            });
            task.setOnFailed(ev -> {
                searchBtn.setDisable(false);
                setGlobalBusy(false, "Search failed.");
                error("Job discovery failed", task.getException());
            });
            new Thread(task, "JobDiscoveryThread").start();
        });

        box.getChildren().addAll(title, searchRow, table);
        return box;
    }

    // ==================== View: Excel Management ====================
    private Node buildExcelManagementView(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Excel & CSV File Management");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Button importBtn = new Button("Import Jobs (Excel/CSV)");
        importBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        importBtn.setOnAction(e -> importJobs(stage));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-padding: 8 16;");
        refreshBtn.setOnAction(e -> refreshApplications());

        HBox actions = new HBox(10, importBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        TableView<JobApplication> table = createApplicationsTableView();
        VBox.setVgrow(table, Priority.ALWAYS);

        box.getChildren().addAll(title, actions, table);
        return box;
    }

    // ==================== View: Email Applications Workflow ====================
    private Node buildEmailApplicationsView(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #0f172a;");

        Label title = new Label("Email Applications Workflow");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        TableView<JobApplication> table = createApplicationsTableView();
        VBox.setVgrow(table, Priority.ALWAYS);

        Button resumeBtn = new Button("Select Resume PDF");
        resumeBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-padding: 8 14;");
        resumeBtn.setOnAction(e -> selectResume(stage));

        Button generateBtn = new Button("Generate / Regenerate Draft");
        generateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 14;");
        generateBtn.setOnAction(e -> generateSelected(table));

        Button previewBtn = new Button("Preview / Edit Email");
        previewBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 14;");
        previewBtn.setOnAction(e -> previewSelected(table));

        Button sendBtn = new Button("Approve & Send");
        sendBtn.setStyle("-fx-background-color: #166534; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18;");
        sendBtn.setOnAction(e -> sendSelected(table));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-padding: 8 14;");
        deleteBtn.setOnAction(e -> deleteSelected(table));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-padding: 8 14;");
        refreshBtn.setOnAction(e -> refreshApplications());

        HBox actions = new HBox(10, resumeBtn, generateBtn, previewBtn, sendBtn, deleteBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(title, actions, table);
        return box;
    }

    // ==================== Shared TableView Helper ====================
    private TableView<JobApplication> createApplicationsTableView() {
        TableView<JobApplication> table = new TableView<>(applicationRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc;");

        addAppColumn(table, "Company", JobApplication::getCompanyName);
        addAppColumn(table, "Job Role", JobApplication::getJobRole);
        addAppColumn(table, "Recruiter Email", JobApplication::getRecruiterEmail);
        addAppColumn(table, "Location", JobApplication::getLocation);
        addAppColumn(table, "Job URL", JobApplication::getJobUrl);

        TableColumn<JobApplication, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "SENT" -> setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                        case "FAILED" -> setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                        case "READY" -> setStyle("-fx-text-fill: #60a5fa; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #94a3b8;");
                    }
                }
            }
        });
        table.getColumns().add(statusCol);

        addAppColumn(table, "Sent At", j -> j.getSentAt() != null ? j.getSentAt().toString().replace("T", " ") : "");
        return table;
    }

    private void addAppColumn(TableView<JobApplication> table, String title, Function<JobApplication, String> fn) {
        TableColumn<JobApplication, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> {
            String val = fn.apply(d.getValue());
            return new SimpleStringProperty(val == null ? "" : val);
        });
        table.getColumns().add(c);
    }

    private void refreshApplications() {
        applicationRows.setAll(jobs.all());
    }

    private void autoDetectResume() {
        try {
            Path resumeDir = Paths.get("./data/resumes");
            if (!Files.exists(resumeDir)) {
                Files.createDirectories(resumeDir);
            }
            try (var stream = Files.list(resumeDir)) {
                Optional<Path> found = stream
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .findFirst();
                if (found.isPresent()) {
                    resume = found.get();
                    if (resumeLabel != null) {
                        resumeLabel.setText(resume.getFileName().toString());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void selectResume(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Resume PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));

        File resumeDir = new File("./data/resumes");
        if (resumeDir.exists() && resumeDir.isDirectory()) {
            fc.setInitialDirectory(resumeDir);
        }

        File f = fc.showOpenDialog(stage);
        if (f != null) {
            resume = f.toPath();
            resumeLabel.setText(f.getName());
            setGlobalStatus("Selected resume: " + f.getName());
        }
    }

    private void importJobs(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Job File (Excel or CSV)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Files (*.xlsx, *.xls, *.csv)", "*.xlsx", "*.xls", "*.csv"),
                new FileChooser.ExtensionFilter("Excel files (*.xlsx, *.xls)", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv")
        );
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        try {
            var r = excel.importFile(f.toPath());
            refreshApplications();
            setGlobalStatus("Imported: " + r.imported() + " | Duplicates: " + r.duplicates() + " | Invalid: " + r.invalid());
            if (!r.errors().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Import Warnings");
                alert.setHeaderText("Import finished with warnings:");
                alert.setContentText(String.join("\n", r.errors()));
                alert.showAndWait();
            }
        } catch (Exception ex) {
            error("Import failed", ex);
        }
    }

    private void generateSelected(TableView<JobApplication> table) {
        JobApplication j = table.getSelectionModel().getSelectedItem();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        if (j.getStatus() == ApplicationStatus.SENT) {
            info("This application has already been sent. Draft cannot be regenerated.");
            return;
        }
        try {
            jobs.generate(j.getId());
            refreshApplications();
            setGlobalStatus("Draft generated for " + j.getCompanyName());
        } catch (Exception ex) {
            error("Could not generate email", ex);
        }
    }

    private void previewSelected(TableView<JobApplication> table) {
        JobApplication j = table.getSelectionModel().getSelectedItem();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        try {
            if (j.getEmailBody() == null || j.getEmailBody().isBlank()) {
                j = jobs.generate(j.getId());
                refreshApplications();
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Review Email - " + j.getCompanyName());

            TextField subject = new TextField(j.getEmailSubject() != null ? j.getEmailSubject() : "");
            subject.setPrefWidth(650);

            TextArea body = new TextArea(j.getEmailBody() != null ? j.getEmailBody() : "");
            body.setPrefRowCount(16);
            body.setWrapText(true);

            VBox box = new VBox(8, new Label("To: " + j.getRecruiterEmail()), new Label("Subject:"), subject, new Label("Message:"), body);
            box.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(box);

            boolean isAlreadySent = j.getStatus() == ApplicationStatus.SENT;
            if (isAlreadySent) {
                subject.setEditable(false);
                body.setEditable(false);
                dialog.setHeaderText("Sent Application (Read Only)");
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.showAndWait();
            } else {
                ButtonType saveBtn = new ButtonType("Save Draft", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveBtn);
                Optional<ButtonType> res = dialog.showAndWait();
                if (res.isPresent() && res.get() == saveBtn) {
                    jobs.updateDraft(j.getId(), subject.getText(), body.getText());
                    refreshApplications();
                    setGlobalStatus("Draft saved for " + j.getCompanyName());
                }
            }
        } catch (Exception ex) {
            error("Preview failed", ex);
        }
    }

    private void sendSelected(TableView<JobApplication> table) {
        JobApplication j = table.getSelectionModel().getSelectedItem();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        if (j.getStatus() == ApplicationStatus.SENT) {
            info("This application has already been sent on " + j.getSentAt());
            return;
        }
        if (resume == null || !Files.exists(resume)) {
            info("Please select your resume PDF first before sending.");
            return;
        }
        if (j.getEmailBody() == null || j.getEmailBody().isBlank()) {
            info("Please generate and review the draft email first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Send Job Application");
        confirm.setHeaderText("Send email to " + j.getRecruiterEmail() + "?");
        confirm.setContentText("Company: " + j.getCompanyName() + "\nRole: " + j.getJobRole() + "\nAttachment: " + resume.getFileName());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        final Path resumeToSend = resume;
        final Long jobId = j.getId();
        final String company = j.getCompanyName();

        setGlobalBusy(true, "Sending email to " + j.getRecruiterEmail() + " via Gmail SMTP...");

        CompletableFuture.runAsync(() -> {
            try {
                sender.sendApproved(jobId, resumeToSend);
                Platform.runLater(() -> {
                    refreshApplications();
                    setGlobalBusy(false, "Email sent to " + company);
                    info("Application email successfully sent to " + company + "!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    refreshApplications();
                    setGlobalBusy(false, "Failed sending to " + company);
                    error("Email sending failed", ex);
                });
            }
        });
    }

    private void deleteSelected(TableView<JobApplication> table) {
        JobApplication j = table.getSelectionModel().getSelectedItem();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete application for " + j.getCompanyName() + "?", ButtonType.CANCEL, ButtonType.OK);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            jobs.delete(j.getId());
            refreshApplications();
            setGlobalStatus("Deleted " + j.getCompanyName());
        }
    }

    private void setGlobalBusy(boolean busy, String message) {
        globalProgress.setVisible(busy);
        globalStatus.setText(message);
    }

    private void setGlobalStatus(String message) {
        globalStatus.setText(message);
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void error(String title, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);

        String msg = ex.getMessage();
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                msg = cause.getMessage();
            }
            cause = cause.getCause();
        }
        alert.setContentText(msg != null ? msg : ex.getClass().getSimpleName());
        alert.showAndWait();
    }
}
