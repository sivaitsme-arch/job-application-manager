package com.siva.jobmanager.ui;

import com.siva.jobmanager.config.SpringContext;
import com.siva.jobmanager.model.ApplicationStatus;
import com.siva.jobmanager.model.JobApplication;
import com.siva.jobmanager.repository.JobApplicationRepository;
import com.siva.jobmanager.service.EmailSendingService;
import com.siva.jobmanager.service.ExcelProcessingService;
import com.siva.jobmanager.service.JobApplicationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DesktopApplication extends Application {
    private final ObservableList<JobApplication> rows = FXCollections.observableArrayList();
    private TableView<JobApplication> table;
    private Path resume;
    private Label resumeLabel;
    private Label status;
    private ProgressIndicator progress;

    private Button importBtn;
    private Button resumeBtn;
    private Button generateBtn;
    private Button previewBtn;
    private Button sendBtn;
    private Button deleteBtn;
    private Button refreshBtn;

    private JobApplicationService jobs;
    private ExcelProcessingService excel;
    private EmailSendingService sender;
    private JobApplicationRepository repository;

    @Override
    public void start(Stage stage) {
        jobs = SpringContext.getBean(JobApplicationService.class);
        excel = SpringContext.getBean(ExcelProcessingService.class);
        sender = SpringContext.getBean(EmailSendingService.class);
        repository = SpringContext.getBean(JobApplicationRepository.class);

        stage.setTitle("Job Application Manager");

        table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addColumn("Company", JobApplication::getCompanyName);
        addColumn("Job Role", JobApplication::getJobRole);
        addColumn("Recruiter Email", JobApplication::getRecruiterEmail);
        addColumn("Location", JobApplication::getLocation);
        addColumn("Job URL", JobApplication::getJobUrl);

        TableColumn<JobApplication, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        statusCol.setCellFactory(col -> new TableCell<JobApplication, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "SENT" -> setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        case "FAILED" -> setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                        case "READY" -> setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #616161;");
                    }
                }
            }
        });
        table.getColumns().add(statusCol);

        addColumn("Sent At", j -> j.getSentAt() != null ? j.getSentAt().toString().replace("T", " ") : "");

        importBtn = new Button("Import Jobs (Excel/CSV)");
        importBtn.setOnAction(e -> importJobs(stage));

        resumeBtn = new Button("Select Resume PDF");
        resumeBtn.setOnAction(e -> selectResume(stage));

        resumeLabel = new Label("No resume selected");
        resumeLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");

        // Try to auto-detect existing resume in ./data/resumes
        autoDetectResume();

        generateBtn = new Button("Generate / Regenerate Draft");
        generateBtn.setOnAction(e -> generateSelected());

        previewBtn = new Button("Preview / Edit Email");
        previewBtn.setOnAction(e -> previewSelected());

        sendBtn = new Button("Approve & Send");
        sendBtn.setStyle("-fx-base: #2e7d32; -fx-font-weight: bold;");
        sendBtn.setOnAction(e -> sendSelected());

        deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-base: #c62828;");
        deleteBtn.setOnAction(e -> deleteSelected());

        refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refresh());

        HBox actions = new HBox(10, importBtn, resumeBtn, resumeLabel, generateBtn, previewBtn, sendBtn, deleteBtn, refreshBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10));

        status = new Label("Ready. Import an Excel or CSV file to begin.");
        status.setPadding(new Insets(6));

        progress = new ProgressIndicator();
        progress.setPrefSize(20, 20);
        progress.setVisible(false);

        HBox statusBar = new HBox(10, progress, status);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6));

        VBox root = new VBox(10, actions, table, statusBar);
        root.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        stage.setScene(new Scene(root, 1200, 680));
        stage.show();
        refresh();
    }

    @Override
    public void stop() {
        if (SpringContext.getContext() instanceof ConfigurableApplicationContext cac) {
            cac.close();
        }
    }

    private void addColumn(String title, Function<JobApplication, String> fn) {
        TableColumn<JobApplication, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> {
            String val = fn.apply(d.getValue());
            return new SimpleStringProperty(val == null ? "" : val);
        });
        table.getColumns().add(c);
    }

    private JobApplication selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void refresh() {
        JobApplication sel = selected();
        Long selId = sel != null ? sel.getId() : null;
        rows.setAll(jobs.all());
        if (selId != null) {
            selectRow(selId);
        }
    }

    private void selectRow(Long id) {
        for (JobApplication j : rows) {
            if (j.getId().equals(id)) {
                table.getSelectionModel().select(j);
                table.scrollTo(j);
                break;
            }
        }
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
                    resumeLabel.setText(resume.getFileName().toString());
                    resumeLabel.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
                }
            }
        } catch (Exception ignored) {
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
            refresh();
            status.setText("Imported: " + r.imported() + " | Duplicates skipped: " + r.duplicates() + " | Invalid: " + r.invalid());
            if (!r.errors().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Import Warnings");
                alert.setHeaderText("Import finished with " + r.invalid() + " invalid row(s):");
                alert.setContentText(String.join("\n", r.errors()));
                alert.showAndWait();
            }
        } catch (Exception ex) {
            error("Excel/CSV import failed", ex);
        }
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
            resumeLabel.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
            status.setText("Selected resume: " + f.getName());
        }
    }

    private void generateSelected() {
        JobApplication j = selected();
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
            refresh();
            selectRow(j.getId());
            status.setText("Draft generated for " + j.getCompanyName() + ". Preview it before sending.");
        } catch (Exception ex) {
            error("Could not generate email", ex);
        }
    }

    private void previewSelected() {
        JobApplication j = selected();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        try {
            if (j.getEmailBody() == null || j.getEmailBody().isBlank()) {
                j = jobs.generate(j.getId());
                refresh();
                selectRow(j.getId());
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Review Email - " + j.getCompanyName());

            TextField subject = new TextField(j.getEmailSubject() != null ? j.getEmailSubject() : "");
            subject.setPrefWidth(650);

            TextArea body = new TextArea(j.getEmailBody() != null ? j.getEmailBody() : "");
            body.setPrefRowCount(16);
            body.setWrapText(true);

            Label toLabel = new Label("To: " + j.getRecruiterEmail());
            toLabel.setStyle("-fx-font-weight: bold;");

            VBox box = new VBox(8, toLabel, new Label("Subject:"), subject, new Label("Message Body:"), body);
            box.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(box);

            boolean isAlreadySent = j.getStatus() == ApplicationStatus.SENT;
            if (isAlreadySent) {
                subject.setEditable(false);
                body.setEditable(false);
                dialog.setHeaderText("Sent Application (Read Only) - " + j.getCompanyName());
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.showAndWait();
            } else {
                dialog.setHeaderText("Review / edit draft for " + j.getCompanyName() + " (" + j.getJobRole() + ")");
                ButtonType saveBtnType = new ButtonType("Save Draft", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveBtnType);

                Optional<ButtonType> result = dialog.showAndWait();
                if (result.isPresent() && result.get() == saveBtnType) {
                    jobs.updateDraft(j.getId(), subject.getText(), body.getText());
                    refresh();
                    selectRow(j.getId());
                    status.setText("Draft saved for " + j.getCompanyName() + ". Select 'Approve & Send' when ready.");
                }
            }
        } catch (Exception ex) {
            error("Preview failed", ex);
        }
    }

    private void sendSelected() {
        JobApplication j = selected();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        if (j.getStatus() == ApplicationStatus.SENT) {
            info("This application has already been sent on " +
                    (j.getSentAt() != null ? j.getSentAt().toString().replace("T", " ") : "a previous date") + ".");
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
        confirm.setContentText("Company: " + j.getCompanyName() + "\n"
                + "Role: " + j.getJobRole() + "\n"
                + "Attachment: " + resume.getFileName().toString() + "\n\n"
                + "Are you sure you want to approve and send this application?");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        final Path resumeToSend = resume;
        final Long jobId = j.getId();
        final String company = j.getCompanyName();
        final String email = j.getRecruiterEmail();

        setBusy(true, "Sending email to " + email + " via Gmail SMTP...");

        CompletableFuture.runAsync(() -> {
            try {
                sender.sendApproved(jobId, resumeToSend);
                Platform.runLater(() -> {
                    refresh();
                    selectRow(jobId);
                    setBusy(false, "Email successfully sent to " + company + " (" + email + ")!");
                    info("Application email successfully sent to " + company + " (" + email + ")!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    refresh();
                    selectRow(jobId);
                    setBusy(false, "Failed to send email to " + company);
                    error("Email sending failed", ex);
                });
            }
        });
    }

    private void deleteSelected() {
        JobApplication j = selected();
        if (j == null) {
            info("Select a job row first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Job Application");
        confirm.setHeaderText("Delete application for " + j.getCompanyName() + "?");
        confirm.setContentText("Are you sure you want to permanently delete this application record?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                jobs.delete(j.getId());
                refresh();
                status.setText("Deleted application for " + j.getCompanyName());
            } catch (Exception ex) {
                error("Could not delete application", ex);
            }
        }
    }

    private void setBusy(boolean busy, String statusText) {
        progress.setVisible(busy);
        status.setText(statusText);
        importBtn.setDisable(busy);
        resumeBtn.setDisable(busy);
        generateBtn.setDisable(busy);
        previewBtn.setDisable(busy);
        sendBtn.setDisable(busy);
        deleteBtn.setDisable(busy);
        refreshBtn.setDisable(busy);
        table.setDisable(busy);
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
        if (msg == null || msg.isBlank()) {
            msg = ex.getClass().getSimpleName();
        }

        alert.setContentText(msg);
        alert.showAndWait();
        status.setText(title + ": " + msg);
    }
}
