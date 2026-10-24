package com.hospital.priority.controller;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.dao.impl.TreatmentRecordDaoImpl;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.service.AuthService;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.QueueService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * HEPS 2.0 EMERGENCY QUEUE CONTROLLER
 * ============================================================================
 * Displays active PriorityQueue, clinical specialty badges, assigned doctors,
 * ER bays, live wait-time target clock, and priority escalation scans.
 */
public class EmergencyQueueController implements Initializable {

    @FXML private Button btnServeHead;
    @FXML private Button btnScanEscalations;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterPriority;
    @FXML private ComboBox<String> cmbFilterSpecialty;
    @FXML private Label lblQueueCounter;

    @FXML private TableView<Patient> queueTable;
    @FXML private TableColumn<Patient, Number> colPosition;
    @FXML private TableColumn<Patient, String> colToken;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, Number> colAge;
    @FXML private TableColumn<Patient, EmergencyLevel> colPriority;
    @FXML private TableColumn<Patient, String> colBay;
    @FXML private TableColumn<Patient, Specialty> colSpecialty;
    @FXML private TableColumn<Patient, String> colDoctor;
    @FXML private TableColumn<Patient, String> colVitals;
    @FXML private TableColumn<Patient, String> colWaitTime;
    @FXML private TableColumn<Patient, String> colSymptoms;

    @FXML private Label lblSelectedInfo;
    @FXML private Button btnPrintTicket;
    @FXML private Button btnServeSelected;

    private final QueueService queueService = QueueService.getInstance();
    private final AuthService authService = AuthService.getInstance();
    private final PatientService patientService = PatientService.getInstance();

    private final ObservableList<Patient> masterQueueList = FXCollections.observableArrayList();
    private FilteredList<Patient> filteredQueueList;
    private Timeline liveClockTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        setupSelectionListener();
        setupLiveClock();
        refreshQueue();
    }

    private void setupLiveClock() {
        // Automatically ticks every 15 seconds to update wait times and threshold colors
        liveClockTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            queueTable.refresh();
        }));
        liveClockTimeline.setCycleCount(Timeline.INDEFINITE);
        liveClockTimeline.play();
    }

    private void setupColumns() {
        colPosition.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQueuePosition()));
        colToken.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTokenNo()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colAge.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getAge()));
        colDoctor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAssignedDoctorName()));
        colVitals.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getVitals() != null ? cell.getValue().getVitals().toSummaryString() : "---"));
        colSymptoms.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSymptoms()));

        // Assigned ER Bay Column
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

        // Live Wait-Time Clock with Target Thresholds
        colWaitTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedWaitingDuration()));
        colWaitTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Patient p = getTableView().getItems().get(getIndex());
                    if (p == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    long mins = p.getWaitingMinutes();
                    EmergencyLevel lvl = p.getEmergencyLevel();
                    long targetLimit = switch (lvl) {
                        case CRITICAL -> 0;
                        case HIGH -> 15;
                        case MEDIUM -> 60;
                        case LOW -> 120;
                    };

                    String targetLabel = (targetLimit == 0) ? "0m" : "<" + targetLimit + "m";
                    String dispText = mins + "m / " + targetLabel;

                    Label pill = new Label();
                    if (targetLimit == 0) {
                        if (mins > 0) {
                            dispText += " 🚨";
                            pill.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: 800; " +
                                          "-fx-font-size: 11px; -fx-padding: 3px 9px; -fx-background-radius: 999px; -fx-border-color: #FCA5A5; -fx-border-radius: 999px;");
                        } else {
                            pill.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-weight: 800; " +
                                          "-fx-font-size: 11px; -fx-padding: 3px 9px; -fx-background-radius: 999px; -fx-border-color: #FCD34D; -fx-border-radius: 999px;");
                        }
                    } else if (mins > targetLimit) {
                        dispText += " ⚠️";
                        pill.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: 800; " +
                                      "-fx-font-size: 11px; -fx-padding: 3px 9px; -fx-background-radius: 999px; -fx-border-color: #FCA5A5; -fx-border-radius: 999px;");
                    } else if (mins >= targetLimit * 0.7) {
                        pill.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-weight: 800; " +
                                      "-fx-font-size: 11px; -fx-padding: 3px 9px; -fx-background-radius: 999px; -fx-border-color: #FCD34D; -fx-border-radius: 999px;");
                    } else {
                        pill.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: 800; " +
                                      "-fx-font-size: 11px; -fx-padding: 3px 9px; -fx-background-radius: 999px; -fx-border-color: #86EFAC; -fx-border-radius: 999px;");
                    }

                    pill.setText(dispText);
                    pill.setTooltip(new Tooltip("Arrived: " + p.getFormattedArrivalTime() + "\nTarget SLA: " + targetLabel));
                    setGraphic(pill);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        // Priority Column with Escalation badge if applicable
        colPriority.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getEmergencyLevel()));
        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EmergencyLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Patient p = getTableView().getItems().get(getIndex());
                    String labelText = item.getDisplayName().toUpperCase();
                    if (p != null && p.isEscalated()) {
                        labelText = "⚡ " + labelText;
                    }
                    Label badge = new Label(labelText);
                    badge.getStyleClass().addAll("badge", item.getCssClass());
                    if (p != null && p.isEscalated()) {
                        badge.setTooltip(new Tooltip("Priority Escalated: " + p.getEscalationReason()));
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        // Specialty Column
        colSpecialty.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getAssignedSpecialty()));
        colSpecialty.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Specialty item, boolean empty) {
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

        ObservableList<String> specialties = FXCollections.observableArrayList("ALL");
        for (Specialty s : Specialty.values()) {
            specialties.add(s.name());
        }
        cmbFilterSpecialty.setItems(specialties);
        cmbFilterSpecialty.setValue("ALL");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        cmbFilterPriority.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        cmbFilterSpecialty.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String search = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String priorityFilter = cmbFilterPriority.getValue();
        String specialtyFilter = cmbFilterSpecialty.getValue();

        filteredQueueList.setPredicate(patient -> {
            if (patient == null) return false;

            // Search filter
            boolean matchesSearch = search.isEmpty()
                    || patient.getFullName().toLowerCase().contains(search)
                    || patient.getTokenNo().toLowerCase().contains(search)
                    || patient.getAssignedDoctorName().toLowerCase().contains(search)
                    || (patient.getSymptoms() != null && patient.getSymptoms().toLowerCase().contains(search));

            // Priority filter
            boolean matchesPriority = priorityFilter == null || priorityFilter.equals("ALL")
                    || patient.getEmergencyLevel().name().equalsIgnoreCase(priorityFilter);

            // Specialty filter
            boolean matchesSpecialty = specialtyFilter == null || specialtyFilter.equals("ALL")
                    || (patient.getAssignedSpecialty() != null && patient.getAssignedSpecialty().name().equalsIgnoreCase(specialtyFilter));

            return matchesSearch && matchesPriority && matchesSpecialty;
        });

        lblQueueCounter.setText(filteredQueueList.size() + " of " + masterQueueList.size() + " Patients Waiting");
    }

    private void setupSelectionListener() {
        queueTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String wait = newSelection.getFormattedWaitingDuration();
                String spec = newSelection.getAssignedSpecialty() != null ? newSelection.getAssignedSpecialty().getDisplayName() : "General Emergency";
                String doc = (newSelection.getAssignedDoctorName() != null && !newSelection.getAssignedDoctorName().isBlank())
                        ? newSelection.getAssignedDoctorName() : "Unassigned";
                String rawBay = newSelection.getAssignedBay();
                String cleanBay = (rawBay != null && rawBay.contains(" - "))
                        ? rawBay.replaceAll(" - [A-Za-z]+", "")
                        : (rawBay != null ? rawBay : "General Bay");

                String escBadge = newSelection.isEscalated() ? " • ⚡ SLA Escalated" : "";

                lblSelectedInfo.setText("[" + newSelection.getTokenNo() + "] " + newSelection.getFullName()
                        + " • " + cleanBay
                        + " • " + newSelection.getEmergencyLevel().getDisplayName()
                        + " • " + spec
                        + " • " + doc
                        + " • Wait: " + wait + escBadge);

                if (newSelection.isEscalated() && newSelection.getEscalationReason() != null) {
                    lblSelectedInfo.setTooltip(new Tooltip("Priority Escalated: " + newSelection.getEscalationReason()));
                } else {
                    lblSelectedInfo.setTooltip(null);
                }

                btnServeSelected.setDisable(false);
                if (btnPrintTicket != null) btnPrintTicket.setDisable(false);
            } else {
                lblSelectedInfo.setText("Select a patient in the table to view details or begin treatment.");
                lblSelectedInfo.setTooltip(null);
                btnServeSelected.setDisable(true);
                if (btnPrintTicket != null) btnPrintTicket.setDisable(true);
            }
        });
        btnServeSelected.setDisable(true);
        if (btnPrintTicket != null) btnPrintTicket.setDisable(true);
    }

    @FXML
    public void handlePrintTicket(ActionEvent event) {
        Patient selected = queueTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            com.hospital.priority.util.TriageTicketUtil.showTicketDialog(selected, false);
        }
    }

    @FXML
    public void refreshQueue() {
        List<Patient> snapshot = queueService.getSortedSnapshot();
        masterQueueList.setAll(snapshot);
        applyFilter();

        boolean hasWaiting = !masterQueueList.isEmpty();
        btnServeHead.setDisable(!hasWaiting);

        if (MainController.getInstance() != null) {
            MainController.getInstance().updateSidebarCounters();
        }
    }

    @FXML
    public void handleScanEscalations(ActionEvent event) {
        List<Patient> escalated = queueService.checkAndApplyEscalations();
        refreshQueue();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Wait-Time Escalation Scanner");
        if (escalated.isEmpty()) {
            alert.setHeaderText("Queue Priority Scan Complete");
            alert.setContentText("All waiting patients are within normal clinical waiting windows. No automatic priority escalations required.");
        } else {
            alert.setHeaderText("⚡ Priority Escalation Applied (" + escalated.size() + " Patients)");
            StringBuilder sb = new StringBuilder("The following patients were escalated due to prolonged wait times:\n\n");
            for (Patient p : escalated) {
                sb.append("• ").append(p.getTokenNo()).append(" - ").append(p.getFullName())
                        .append(" elevated to ").append(p.getEmergencyLevel())
                        .append(" (").append(p.getEscalationReason()).append(")\n");
            }
            alert.setContentText(sb.toString());
        }
        alert.showAndWait();
    }

    @FXML
    public void handleServeNext(ActionEvent event) {
        Patient next = queueService.peekNext();
        if (next == null) {
            showAlert(Alert.AlertType.INFORMATION, "Queue Empty", "No patients currently waiting in the emergency queue.");
            return;
        }
        confirmAndServe(next);
    }

    @FXML
    public void handleServeSelected(ActionEvent event) {
        Patient selected = queueTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Patient head = queueService.peekNext();
        if (head != null && !head.equals(selected)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Out-of-Order Clinical Override");
            confirm.setHeaderText("Caution: Higher Priority Patient Waiting");
            confirm.setContentText("You are calling [" + selected.getTokenNo() + "] " + selected.getFullName()
                    + " (" + selected.getEmergencyLevel() + "), but higher priority patient ["
                    + head.getTokenNo() + "] " + head.getFullName() + " (" + head.getEmergencyLevel()
                    + ") is at the head of the PriorityQueue.\n\nDo you want to proceed?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        confirmAndServe(selected);
    }

    private void confirmAndServe(Patient patient) {
        String currentStaff = authService.getCurrentUser() != null
                ? authService.getCurrentUser().getFullName()
                : (patient.getAssignedDoctorName() != null ? patient.getAssignedDoctorName() : "Attending Physician");

        try {
            queueService.removeFromQueue(patient);
            patient.setStatus(com.hospital.priority.model.PatientStatus.IN_TREATMENT);

            // Persist status
            new PatientDaoImpl().updateStatus(patient.getPatientId(), com.hospital.priority.model.PatientStatus.IN_TREATMENT);

            // Record admission
            new TreatmentRecordDaoImpl().insert(new com.hospital.priority.model.TreatmentRecord(
                    patient.getPatientId(),
                    currentStaff,
                    "Admitted to emergency bay for " + patient.getAssignedSpecialty().getDisplayName() + " care.",
                    java.time.LocalDateTime.now()
            ));

            refreshQueue();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Treatment Begun");
            alert.setHeaderText("Patient Admitted to Treatment Bay");
            alert.setContentText("Patient: [" + patient.getTokenNo() + "] " + patient.getFullName() + "\n"
                    + "Department: " + patient.getAssignedSpecialty().getDisplayName() + "\n"
                    + "Physician: " + currentStaff + "\n"
                    + "Status: IN TREATMENT");
            alert.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Dispatch Error", "Failed to dispatch patient: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
