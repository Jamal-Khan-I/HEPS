package com.hospital.priority.controller;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.model.TreatmentRecord;
import com.hospital.priority.service.AuthService;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.TreatmentService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PatientHistoryController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbLevelFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Button btnCompleteTreatmentAction;

    @FXML private TableView<Patient> historyTable;
    @FXML private TableColumn<Patient, String> colToken;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, Number> colAge;
    @FXML private TableColumn<Patient, String> colGender;
    @FXML private TableColumn<Patient, String> colPhone;
    @FXML private TableColumn<Patient, EmergencyLevel> colLevel;
    @FXML private TableColumn<Patient, String> colBay;
    @FXML private TableColumn<Patient, PatientStatus> colStatus;
    @FXML private TableColumn<Patient, String> colArrival;

    @FXML private VBox detailsCard;
    @FXML private Label lblSelectedTokenAndName;
    @FXML private Button btnPrintHistoryTicket;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailSymptoms;
    @FXML private Label lblDetailTreatmentNotes;

    private final PatientService patientService = PatientService.getInstance();
    private final TreatmentService treatmentService = TreatmentService.getInstance();
    private final AuthService authService = AuthService.getInstance();

    private final ObservableList<Patient> historyList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        setupSelection();
        refreshHistory();
    }

    private void setupColumns() {
        colToken.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTokenNo()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colAge.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAge()));
        colGender.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGender()));
        colPhone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMaskedPhone()));
        colArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedArrivalTime()));

        colBay.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAssignedBay()));
        colBay.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String bay, boolean empty) {
                super.updateItem(bay, empty);
                if (empty || bay == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String displayBay = bay.contains(" - ") ? bay.replaceAll(" - [A-Za-z]+", "") : bay;
                    Label chip = new Label(displayBay);
                    chip.setStyle(
                            "-fx-background-color: #F8FAFC; " +
                            "-fx-text-fill: #0F172A; " +
                            "-fx-font-weight: 700; " +
                            "-fx-font-size: 11px; " +
                            "-fx-padding: 3px 8px; " +
                            "-fx-background-radius: 999px; " +
                            "-fx-border-color: #CBD5E1; " +
                            "-fx-border-radius: 999px;"
                    );
                    setGraphic(chip);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        colLevel.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getEmergencyLevel()));
        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EmergencyLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.getDisplayName().toUpperCase());
                    badge.getStyleClass().addAll("badge", item.getCssClass());
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PatientStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.getDisplayName());
                    badge.getStyleClass().addAll("badge", item.getCssClass());
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        historyTable.setItems(historyList);
    }

    private void setupFilters() {
        cmbLevelFilter.setItems(FXCollections.observableArrayList("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"));
        cmbLevelFilter.setValue("ALL");

        cmbStatusFilter.setItems(FXCollections.observableArrayList("ALL", "WAITING", "IN_TREATMENT", "COMPLETED", "CANCELLED"));
        cmbStatusFilter.setValue("ALL");

        txtSearch.textProperty().addListener((obs, oldV, newV) -> handleSearch(null));
        cmbLevelFilter.valueProperty().addListener((obs, oldV, newV) -> handleSearch(null));
        cmbStatusFilter.valueProperty().addListener((obs, oldV, newV) -> handleSearch(null));
    }

    private void setupSelection() {
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, selected) -> {
            if (selected != null) {
                displayPatientDetails(selected);
                btnCompleteTreatmentAction.setDisable(selected.getStatus() != PatientStatus.IN_TREATMENT);
                if (btnPrintHistoryTicket != null) btnPrintHistoryTicket.setVisible(true);
            } else {
                lblSelectedTokenAndName.setText("Select a patient above to inspect clinical history.");
                lblDetailStatus.setVisible(false);
                lblDetailSymptoms.setText("-");
                lblDetailTreatmentNotes.setText("No patient selected.");
                btnCompleteTreatmentAction.setDisable(true);
                if (btnPrintHistoryTicket != null) btnPrintHistoryTicket.setVisible(false);
            }
        });
        btnCompleteTreatmentAction.setDisable(true);
        if (btnPrintHistoryTicket != null) btnPrintHistoryTicket.setVisible(false);
    }

    private void displayPatientDetails(Patient patient) {
        lblSelectedTokenAndName.setText("[" + patient.getTokenNo() + "] " + patient.getFullName() + 
                " (Age " + patient.getAge() + ", " + patient.getGender() + " • Phone: " + patient.getPhone() + ")");
        lblDetailStatus.setVisible(true);
        lblDetailStatus.setText(patient.getStatus().getDisplayName());
        lblDetailStatus.getStyleClass().setAll("badge", patient.getStatus().getCssClass());

        StringBuilder symSb = new StringBuilder();
        symSb.append(patient.getSymptoms()).append("\n\n");
        symSb.append("• Assigned ER Bay: ").append(patient.getAssignedBay()).append("\n");
        symSb.append("• Specialty: ").append(patient.getAssignedSpecialty() != null ? patient.getAssignedSpecialty().getDisplayName() : "General Emergency").append("\n");
        symSb.append("• Assigned Doctor: ").append(patient.getAssignedDoctorName()).append("\n");
        symSb.append("• Physiological Vitals: ").append(patient.getVitals() != null ? patient.getVitals().toSummaryString() : "Not recorded");
        if (patient.isEscalated()) {
            symSb.append("\n• ⚡ Escalation Note: ").append(patient.getEscalationReason());
        }
        if (patient.getOverrideReason() != null && !patient.getOverrideReason().isEmpty()) {
            symSb.append("\n• ⚠ Clinical Override Reason: ").append(patient.getOverrideReason());
        }
        lblDetailSymptoms.setText(symSb.toString());

        try {
            Optional<TreatmentRecord> recOpt = treatmentService.getTreatmentForPatient(patient.getPatientId());
            if (recOpt.isPresent()) {
                TreatmentRecord rec = recOpt.get();
                StringBuilder sb = new StringBuilder();
                sb.append("Attending Doctor: ").append(rec.getDoctorName() != null ? rec.getDoctorName() : "N/A").append("\n");
                sb.append("Clinical Disposition: ").append(rec.getFormattedDisposition()).append("\n");
                if (rec.getCompletedAt() != null) {
                    sb.append("Completed At: ").append(rec.getCompletedAt().format(Patient.DISPLAY_TIME_FMT)).append("\n");
                }
                sb.append("Clinical Notes: ").append(rec.getTreatmentNotes() != null ? rec.getTreatmentNotes() : "No notes added.");
                lblDetailTreatmentNotes.setText(sb.toString());
            } else {
                lblDetailTreatmentNotes.setText("No treatment record recorded yet (" + patient.getStatus().getDisplayName() + ").");
            }
        } catch (Exception e) {
            lblDetailTreatmentNotes.setText("Error loading treatment record: " + e.getMessage());
        }
    }

    @FXML
    public void refreshHistory() {
        handleSearch(null);
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String term = txtSearch.getText();
        String level = cmbLevelFilter.getValue();
        String status = cmbStatusFilter.getValue();

        try {
            List<Patient> results = patientService.searchPatients(term, level, status);
            historyList.setAll(results);
            historyTable.getSelectionModel().clearSelection();
            btnCompleteTreatmentAction.setDisable(true);
            if (btnPrintHistoryTicket != null) btnPrintHistoryTicket.setVisible(false);
        } catch (Exception e) {
            showError("Search Error", "Could not fetch patient records: " + e.getMessage());
        }
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        txtSearch.clear();
        cmbLevelFilter.setValue("ALL");
        cmbStatusFilter.setValue("ALL");
        refreshHistory();
    }

    @FXML
    public void handlePrintTicket(ActionEvent event) {
        Patient selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            com.hospital.priority.util.TriageTicketUtil.showTicketDialog(selected, false);
        }
    }

    @FXML
    private void handleCompleteSelected(ActionEvent event) {
        Patient selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select Patient", "Please select an 'In Treatment' patient from the table.");
            return;
        }

        if (selected.getStatus() != PatientStatus.IN_TREATMENT) {
            showInfo("Not In Treatment", "Only patients currently in treatment can be completed.");
            return;
        }

        // 1-Click Clinical Disposition Dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Clinical Disposition & Treatment Completion");
        dialog.setHeaderText("Complete Treatment: " + selected.getFullName() + " (" + selected.getTokenNo() + ") • " + selected.getAssignedBay());

        ButtonType btnSubmit = new ButtonType("Complete & Discharge →", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSubmit, ButtonType.CANCEL);

        VBox contentBox = new VBox(14);
        contentBox.setStyle("-fx-padding: 16px;");

        // Outcome Selector (1-Click Disposition)
        Label lblOutcomeHeading = new Label("1. Select Clinical Disposition / Outcome:");
        lblOutcomeHeading.setStyle("-fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

        ToggleGroup outcomeGroup = new ToggleGroup();
        RadioButton rbDischarge = new RadioButton("✓ Discharged Home (Stable)");
        rbDischarge.setToggleGroup(outcomeGroup);
        rbDischarge.setSelected(true);
        rbDischarge.setStyle("-fx-font-weight: 700; -fx-text-fill: #16A34A;");

        RadioButton rbAdmit = new RadioButton("🏥 Admitted to Inpatient Ward");
        rbAdmit.setToggleGroup(outcomeGroup);
        rbAdmit.setStyle("-fx-font-weight: 700; -fx-text-fill: #0284C7;");

        RadioButton rbIcu = new RadioButton("🚨 Transferred to ICU/OT");
        rbIcu.setToggleGroup(outcomeGroup);
        rbIcu.setStyle("-fx-font-weight: 700; -fx-text-fill: #DC2626;");

        HBox outcomeRow = new HBox(16, rbDischarge, rbAdmit, rbIcu);
        outcomeRow.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 10px 14px; -fx-background-radius: 12px; -fx-border-color: #E2E8F0; -fx-border-radius: 12px;");

        // Doctor & Notes
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtDoctor = new TextField();
        String defaultDoctor = selected.getAssignedDoctorName() != null && !selected.getAssignedDoctorName().equalsIgnoreCase("Unassigned")
                ? selected.getAssignedDoctorName()
                : (authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Attending Clinician");
        txtDoctor.setText(defaultDoctor);

        TextArea txtNotes = new TextArea();
        txtNotes.setText("Patient examined and stabilized. Vital signs normalized. Discharged home in stable condition with outpatient care instructions.");
        txtNotes.setPrefRowCount(4);
        txtNotes.setWrapText(true);

        // Auto-fill template notes on 1-click radio toggle
        outcomeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rbDischarge) {
                txtNotes.setText("Patient examined and stabilized. Vital signs normalized. Discharged home in stable condition with outpatient care instructions.");
            } else if (newVal == rbAdmit) {
                txtNotes.setText("Patient stabilized but requires continuing inpatient observation and treatment. Admitted to General Inpatient Ward under attending specialist care.");
            } else if (newVal == rbIcu) {
                txtNotes.setText("High-acuity emergency transfer initiated. Critical stabilization completed in bay; transferred immediately to Intensive Care Unit (ICU) / OT.");
            }
        });

        Label lblDoc = new Label("Attending Physician:");
        lblDoc.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569;");
        Label lblNotes = new Label("Clinical Observations & Notes *:");
        lblNotes.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569;");

        grid.add(lblDoc, 0, 0);
        grid.add(txtDoctor, 1, 0);
        grid.add(lblNotes, 0, 1);
        grid.add(txtNotes, 1, 1);
        GridPane.setHgrow(txtDoctor, Priority.ALWAYS);
        GridPane.setHgrow(txtNotes, Priority.ALWAYS);

        contentBox.getChildren().addAll(lblOutcomeHeading, outcomeRow, grid);
        dialog.getDialogPane().setContent(contentBox);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnSubmit) {
            String doctor = txtDoctor.getText();
            String notes = txtNotes.getText();
            String disposition = "DISCHARGED_HOME";
            String dispLabel = "Discharged Home";
            if (outcomeGroup.getSelectedToggle() == rbAdmit) {
                disposition = "ADMITTED_INPATIENT";
                dispLabel = "Admitted to Inpatient";
            } else if (outcomeGroup.getSelectedToggle() == rbIcu) {
                disposition = "TRANSFERRED_ICU";
                dispLabel = "Transferred to ICU/OT";
            }

            if (notes == null || notes.trim().isEmpty()) {
                showError("Validation Error", "Clinical treatment notes cannot be empty before recording disposition.");
                return;
            }

            try {
                treatmentService.completeTreatment(selected.getPatientId(), doctor, notes, disposition);
                refreshHistory();
                if (MainController.getInstance() != null) {
                    MainController.getInstance().updateSidebarCounters();
                }
                showInfo("Clinical Disposition Recorded", "Patient [" + selected.getTokenNo() + "] " + selected.getFullName()
                        + " completed with outcome: " + dispLabel + ".");
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
