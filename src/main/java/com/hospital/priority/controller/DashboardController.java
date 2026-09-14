package com.hospital.priority.controller;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.AuthService;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.QueueService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label lblTotalWaiting;
    @FXML private Label lblCriticalWaiting;
    @FXML private Label lblInTreatment;
    @FXML private Label lblCompletedToday;

    @FXML private VBox nextPatientBanner;
    @FXML private Label lblNextBadge;
    @FXML private Label lblNextToken;
    @FXML private Label lblNextName;
    @FXML private Label lblNextAge;
    @FXML private Label lblNextSymptoms;
    @FXML private Label lblNextArrival;
    @FXML private Button btnServeNextQuick;

    @FXML private TableView<Patient> previewTable;
    @FXML private TableColumn<Patient, Number> colPos;
    @FXML private TableColumn<Patient, String> colToken;
    @FXML private TableColumn<Patient, String> colName;
    @FXML private TableColumn<Patient, Number> colAge;
    @FXML private TableColumn<Patient, EmergencyLevel> colPriority;
    @FXML private TableColumn<Patient, String> colSymptoms;
    @FXML private TableColumn<Patient, String> colArrival;

    private final PatientService patientService = PatientService.getInstance();
    private final QueueService queueService = QueueService.getInstance();
    private final AuthService authService = AuthService.getInstance();

    private final ObservableList<Patient> previewList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        refreshData();
    }

    private void setupTableColumns() {
        colPos.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQueuePosition()));
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

        previewTable.setItems(previewList);
    }

    @FXML
    public void refreshData() {
        try {
            Map<String, Object> stats = patientService.getDashboardStatistics();

            lblTotalWaiting.setText(String.valueOf(stats.get("totalWaiting")));
            lblCriticalWaiting.setText(String.valueOf(stats.get("criticalWaiting")));
            lblInTreatment.setText(String.valueOf(stats.get("inTreatment")));
            lblCompletedToday.setText(String.valueOf(stats.get("completedToday")));

            Patient nextPatient = (Patient) stats.get("nextPatient");
            updateNextPatientBanner(nextPatient);

            List<Patient> fullQueue = queueService.getSortedSnapshot();
            previewList.setAll(fullQueue);

            if (MainController.getInstance() != null) {
                MainController.getInstance().updateSidebarCounters();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Data Refresh Error", "Unable to load dashboard metrics: " + e.getMessage());
        }
    }

    private void updateNextPatientBanner(Patient patient) {
        if (patient != null) {
            nextPatientBanner.setVisible(true);
            lblNextBadge.setText(patient.getEmergencyLevel().name());
            lblNextBadge.getStyleClass().setAll("badge", patient.getEmergencyLevel().getCssClass());

            lblNextToken.setText(patient.getTokenNo());
            lblNextName.setText(patient.getFullName());
            lblNextAge.setText("(Age: " + patient.getAge() + ", " + patient.getGender() + ")");
            lblNextSymptoms.setText("Chief Symptoms: " + patient.getSymptoms());
            lblNextArrival.setText("Triage Arrival: " + patient.getFormattedArrivalTime());
            btnServeNextQuick.setDisable(false);
        } else {
            lblNextBadge.setText("EMPTY");
            lblNextBadge.getStyleClass().setAll("badge", "badge-low");
            lblNextToken.setText("---");
            lblNextName.setText("All Caught Up! No Patients Waiting");
            lblNextAge.setText("");
            lblNextSymptoms.setText("Emergency triage queue is currently clear.");
            lblNextArrival.setText("");
            btnServeNextQuick.setDisable(true);
        }
    }

    @FXML
    private void handleServeNext(ActionEvent event) {
        Patient head = queueService.peekNext();
        if (head == null) {
            showInfo("Queue Empty", "There are no waiting patients currently in the emergency queue.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Patient Triage Admission");
        confirm.setHeaderText("Serve Highest Priority Patient (Queue Head)");
        confirm.setContentText("Admit Patient: " + head.getFullName() + " (" + head.getTokenNo() + ")\n"
                + "Urgency: " + head.getEmergencyLevel() + "\n"
                + "Symptoms: " + head.getSymptoms() + "\n\n"
                + "Change status to IN_TREATMENT and open treatment record?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String doctorName = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Attending Physician";
                Patient served = queueService.serveNext(doctorName);
                refreshData();
                showInfo("Patient Admitted", "Patient " + served.getFullName() + " (" + served.getTokenNo() + ") is now IN TREATMENT.\nAdmitted by: " + doctorName);
            } catch (Exception e) {
                showError("Admission Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleQuickRegister(ActionEvent event) {
        if (MainController.getInstance() != null) {
            MainController.getInstance().showRegisterPatient(null);
        }
    }

    @FXML
    private void handleViewFullQueue(ActionEvent event) {
        if (MainController.getInstance() != null) {
            MainController.getInstance().showEmergencyQueue(null);
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
