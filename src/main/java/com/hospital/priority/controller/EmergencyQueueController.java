package com.hospital.priority.controller;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.dao.impl.TreatmentRecordDaoImpl;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.model.TreatmentRecord;
import com.hospital.priority.service.AuthService;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.QueueService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EmergencyQueueController implements Initializable {

    @FXML private Button btnServeHead;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterPriority;
    @FXML private Label lblQueueCounter;

    @FXML private TableView<Patient> queueTable;
    @FXML private TableColumn<Patient, Number> colPosition;
    @FXML private TableColumn<Patient, String> colToken;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, Number> colAge;
    @FXML private TableColumn<Patient, EmergencyLevel> colPriority;
    @FXML private TableColumn<Patient, String> colSymptoms;
    @FXML private TableColumn<Patient, String> colArrival;

    @FXML private Label lblSelectedInfo;
    @FXML private Button btnServeSelected;

    private final QueueService queueService = QueueService.getInstance();
    private final AuthService authService = AuthService.getInstance();
    private final PatientService patientService = PatientService.getInstance();
    private final PatientDao patientDao = new PatientDaoImpl();
    private final TreatmentRecordDao treatmentRecordDao = new TreatmentRecordDaoImpl();

    private final ObservableList<Patient> masterQueueList = FXCollections.observableArrayList();
    private FilteredList<Patient> filteredQueueList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        setupSelectionListener();
        refreshQueue();
    }

    private void setupColumns() {
        colPosition.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQueuePosition()));
        colToken.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTokenNo()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colAge.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAge()));
        colSymptoms.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSymptoms()));
        colArrival.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedArrivalTime()));

        colPriority.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getEmergencyLevel()));
        colPriority.setCellFactory(column -> new TableCell<>() {
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

        filteredQueueList = new FilteredList<>(masterQueueList, p -> true);
        SortedList<Patient> sortedList = new SortedList<>(filteredQueueList);
        sortedList.comparatorProperty().bind(queueTable.comparatorProperty());
        queueTable.setItems(filteredQueueList);
    }

    private void setupFilters() {
        cmbFilterPriority.setItems(FXCollections.observableArrayList("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"));
        cmbFilterPriority.setValue("ALL");

        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        cmbFilterPriority.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
    }

    private void applyFilter() {
        String query = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String priorityFilter = cmbFilterPriority.getValue();

        filteredQueueList.setPredicate(patient -> {
            boolean matchesSearch = query.isEmpty()
                    || (patient.getFullName() != null && patient.getFullName().toLowerCase().contains(query))
                    || (patient.getTokenNo() != null && patient.getTokenNo().toLowerCase().contains(query))
                    || (patient.getSymptoms() != null && patient.getSymptoms().toLowerCase().contains(query));

            boolean matchesPriority = priorityFilter == null
                    || "ALL".equalsIgnoreCase(priorityFilter)
                    || (patient.getEmergencyLevel() != null && patient.getEmergencyLevel().name().equalsIgnoreCase(priorityFilter));

            return matchesSearch && matchesPriority;
        });

        lblQueueCounter.setText("Showing " + filteredQueueList.size() + " Waiting Patients");
    }

    private void setupSelectionListener() {
        btnServeSelected.setDisable(true);

        queueTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, selected) -> {
            if (selected != null && selected.getStatus() == PatientStatus.WAITING) {
                lblSelectedInfo.setText("[" + selected.getTokenNo() + "] " + selected.getFullName()
                        + " | Urgency: " + selected.getEmergencyLevel() + " | Position #" + selected.getQueuePosition()
                        + " | Symptoms: " + selected.getSymptoms());
                btnServeSelected.setDisable(false);
            } else {
                lblSelectedInfo.setText("Select a WAITING patient in the table to admit via manual clinician override.");
                btnServeSelected.setDisable(true);
            }
        });
    }

    @FXML
    public void refreshQueue() {
        List<Patient> snapshot = queueService.getSortedSnapshot();
        masterQueueList.setAll(snapshot);
        applyFilter();

        if (MainController.getInstance() != null) {
            MainController.getInstance().updateSidebarCounters();
        }
    }

    @FXML
    private void handleServeNext(ActionEvent event) {
        Patient head = queueService.peekNext();
        if (head == null) {
            showInfoAlert("Queue Empty", "No waiting patients are currently in the emergency triage queue.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Serving Patient (Queue Head)");
        confirm.setHeaderText("Call Highest Priority Waiting Patient");
        confirm.setContentText("Admit Head of Queue into Treatment:\n"
                + "Patient: " + head.getFullName() + " (" + head.getTokenNo() + ")\n"
                + "Urgency: " + head.getEmergencyLevel() + " (Position #1)\n"
                + "Symptoms: " + head.getSymptoms() + "\n\n"
                + "Change status to IN_TREATMENT and open treatment record in MySQL?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String doc = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Attending Doctor";
                Patient served = queueService.serveNext(doc);
                refreshQueue();
                showInfoAlert("Patient Called into Treatment", "Patient " + served.getFullName() + " (" + served.getTokenNo()
                        + ") is now IN TREATMENT.\nAdmitted by: " + doc);
            } catch (Exception e) {
                showErrorAlert("Error Serving Patient", e.getMessage());
            }
        }
    }

    @FXML
    private void handleServeSelected(ActionEvent event) {
        Patient selected = queueTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getStatus() != PatientStatus.WAITING) {
            showInfoAlert("No Waiting Patient Selected", "Please select a WAITING patient from the queue table.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Manual Clinician Override");
        confirm.setHeaderText("Admit Selected Patient (Manual Clinician Override)");
        confirm.setContentText("You are admitting:\n"
                + "[" + selected.getTokenNo() + "] " + selected.getFullName() + "\n"
                + "Urgency Level: " + selected.getEmergencyLevel() + " (Queue Pos #" + selected.getQueuePosition() + ")\n"
                + "Symptoms: " + selected.getSymptoms() + "\n\n"
                + "Purpose: This action overrides standard queue head order to immediately admit this specific waiting patient.\n\n"
                + "Change status to IN_TREATMENT, create treatment record, and remove from active waiting queue?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String doc = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Attending Doctor";
                
                // 1. Remove from in-memory PriorityQueue
                queueService.removeFromQueue(selected);
                
                // 2. Update model and MySQL status
                selected.setStatus(PatientStatus.IN_TREATMENT);
                patientDao.updateStatus(selected.getPatientId(), PatientStatus.IN_TREATMENT);

                // 3. Persist treatment record
                TreatmentRecord rec = new TreatmentRecord(
                        selected.getPatientId(),
                        doc,
                        "Admitted via clinician selection override from triage queue.",
                        LocalDateTime.now()
                );
                treatmentRecordDao.insert(rec);

                refreshQueue();
                showInfoAlert("Patient Admitted", "Patient " + selected.getFullName() + " (" + selected.getTokenNo()
                        + ") has been admitted to treatment via manual clinician override.");
            } catch (Exception e) {
                showErrorAlert("Error Admitting Patient", e.getMessage());
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
