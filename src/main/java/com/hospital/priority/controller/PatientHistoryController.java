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
    @FXML private TableColumn<Patient, PatientStatus> colStatus;
    @FXML private TableColumn<Patient, String> colArrival;

    @FXML private VBox detailsCard;
    @FXML private Label lblSelectedTokenAndName;
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
            } else {
                lblSelectedTokenAndName.setText("Select a patient above to inspect clinical history.");
                lblDetailStatus.setVisible(false);
                lblDetailSymptoms.setText("-");
                lblDetailTreatmentNotes.setText("No patient selected.");
                btnCompleteTreatmentAction.setDisable(true);
            }
        });
        btnCompleteTreatmentAction.setDisable(true);
    }

    private void displayPatientDetails(Patient patient) {
        lblSelectedTokenAndName.setText("[" + patient.getTokenNo() + "] " + patient.getFullName() + 
                " (Age " + patient.getAge() + ", " + patient.getGender() + " • Phone: " + patient.getPhone() + ")");
        lblDetailStatus.setVisible(true);
        lblDetailStatus.setText(patient.getStatus().getDisplayName());
        lblDetailStatus.getStyleClass().setAll("badge", patient.getStatus().getCssClass());

        lblDetailSymptoms.setText(patient.getSymptoms());

        try {
            Optional<TreatmentRecord> recOpt = treatmentService.getTreatmentForPatient(patient.getPatientId());
            if (recOpt.isPresent()) {
                TreatmentRecord rec = recOpt.get();
                StringBuilder sb = new StringBuilder();
                sb.append("Attending Doctor: ").append(rec.getDoctorName() != null ? rec.getDoctorName() : "N/A").append("\n");
                if (rec.getCompletedAt() != null) {
                    sb.append("Completed At: ").append(rec.getCompletedAt()).append("\n");
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

        // Dialog for entering Doctor name and Treatment Notes
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Complete Treatment");
        dialog.setHeaderText("Complete Treatment: " + selected.getFullName() + " (" + selected.getTokenNo() + ")");

        ButtonType btnSubmit = new ButtonType("Complete Treatment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSubmit, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 16px;");

        TextField txtDoctor = new TextField();
        String defaultDoctor = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Attending Clinician";
        txtDoctor.setText(defaultDoctor);

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Enter clinical observations, administered procedures, and discharge notes...");
        txtNotes.setPrefRowCount(4);
        txtNotes.setWrapText(true);

        grid.add(new Label("Attending Doctor:"), 0, 0);
        grid.add(txtDoctor, 1, 0);
        grid.add(new Label("Treatment & Discharge Notes *:"), 0, 1);
        grid.add(txtNotes, 1, 1);
        GridPane.setHgrow(txtDoctor, Priority.ALWAYS);
        GridPane.setHgrow(txtNotes, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnSubmit) {
            String doctor = txtDoctor.getText();
            String notes = txtNotes.getText();

            if (notes == null || notes.trim().isEmpty()) {
                showError("Validation Error", "Treatment notes cannot be empty before completing discharge.");
                return;
            }

            try {
                treatmentService.completeTreatment(selected.getPatientId(), doctor, notes);
                refreshHistory();
                if (MainController.getInstance() != null) {
                    MainController.getInstance().updateSidebarCounters();
                }
                showInfo("Treatment Completed", "Patient " + selected.getFullName() + " (" + selected.getTokenNo() + ") marked as COMPLETED.");
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
