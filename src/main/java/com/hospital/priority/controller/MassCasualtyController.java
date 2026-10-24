package com.hospital.priority.controller;

import com.hospital.priority.model.*;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.BayService;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.TokenGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * MASS CASUALTY INCIDENT (MCI) CONTROLLER (Page 17)
 * ============================================================================
 * Implements the START (Simple Triage and Rapid Treatment) disaster protocol.
 * Enables 10-second rapid intake, bypassing full registration, and instantly
 * dispatching high-acuity disaster casualties to the priority Min-Heap.
 */
public class MassCasualtyController implements Initializable {

    @FXML private Button btnToggleMci;
    @FXML private HBox mciBanner;
    @FXML private Label lblMciVictimCount;

    @FXML private Label lblRedCount;
    @FXML private Label lblYellowCount;
    @FXML private Label lblGreenCount;
    @FXML private Label lblBlackCount;

    // Rapid Intake Form
    @FXML private TextField txtMciIdentifier;
    @FXML private TextField txtMciAge;
    @FXML private ComboBox<String> cmbMciGender;
    @FXML private TextField txtMciInjuries;

    @FXML private ToggleGroup startTagGroup;
    @FXML private RadioButton rbRed;
    @FXML private RadioButton rbYellow;
    @FXML private RadioButton rbGreen;
    @FXML private RadioButton rbBlack;

    @FXML private ComboBox<String> cmbMciBay;

    // Victims Table
    @FXML private TableView<MassCasualtyTag> tableMciVictims;
    @FXML private TableColumn<MassCasualtyTag, String> colTag;
    @FXML private TableColumn<MassCasualtyTag, String> colTime;
    @FXML private TableColumn<MassCasualtyTag, String> colIdentifier;
    @FXML private TableColumn<MassCasualtyTag, String> colLocation;

    private final ObservableList<MassCasualtyTag> victimsList = FXCollections.observableArrayList();
    private final QueueService queueService = QueueService.getInstance();
    private final BayService bayService = BayService.getInstance();

    private boolean isMciActive = true;
    private int mciSequence = 5;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFormControls();
        setupTableColumns();
        seedInitialVictims();
        updateStats();
    }

    private void setupFormControls() {
        cmbMciGender.setItems(FXCollections.observableArrayList("Unknown", "Male", "Female", "Child"));
        cmbMciGender.getSelectionModel().selectFirst();

        cmbMciBay.setItems(FXCollections.observableArrayList(
                "Bay 1 · Resus",
                "Bay 2 · Trauma",
                "Bay 3 · Acute",
                "Bay 4 · Obs",
                "Triage Tent Alpha",
                "Walking Holding Area (Corridor)",
                "Emergency Holding Mortuary"
        ));
        cmbMciBay.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        colTag.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTag().getName()));
        colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedTime()));
        colIdentifier.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPatientIdentifier()));
        colLocation.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLocation()));

        colTag.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tag, boolean empty) {
                super.updateItem(tag, empty);
                if (empty || tag == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(tag.toUpperCase());
                    if ("RED".equalsIgnoreCase(tag)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 3px 10px; -fx-background-radius: 999px;");
                    } else if ("YELLOW".equalsIgnoreCase(tag)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #A16207; -fx-background-color: #FEF9C3; -fx-padding: 3px 10px; -fx-background-radius: 999px;");
                    } else if ("GREEN".equalsIgnoreCase(tag)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 3px 10px; -fx-background-radius: 999px;");
                    } else {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-background-color: #F1F5F9; -fx-padding: 3px 10px; -fx-background-radius: 999px;");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        tableMciVictims.setItems(victimsList);
    }

    private void seedInitialVictims() {
        victimsList.clear();
        victimsList.add(new MassCasualtyTag(MassCasualtyTag.StartTag.RED, "Unknown male, ~40s (MCI-001) - Crush injury", "Bay 1 · Resus"));
        victimsList.add(new MassCasualtyTag(MassCasualtyTag.StartTag.YELLOW, "Priyanka D., 27 (MCI-002) - Open femur fracture", "Bay 3 · Acute"));
        victimsList.add(new MassCasualtyTag(MassCasualtyTag.StartTag.YELLOW, "Suresh M., 52 (MCI-003) - Concussive blast injury", "Bay 4 · Obs"));
        victimsList.add(new MassCasualtyTag(MassCasualtyTag.StartTag.GREEN, "Deepak S., 19 (MCI-004) - Superficial glass shrapnel", "Walking Holding Area"));
    }

    private void updateStats() {
        long red = victimsList.stream().filter(v -> v.getTag() == MassCasualtyTag.StartTag.RED).count();
        long yellow = victimsList.stream().filter(v -> v.getTag() == MassCasualtyTag.StartTag.YELLOW).count();
        long green = victimsList.stream().filter(v -> v.getTag() == MassCasualtyTag.StartTag.GREEN).count();
        long black = victimsList.stream().filter(v -> v.getTag() == MassCasualtyTag.StartTag.BLACK).count();

        lblRedCount.setText(String.valueOf(red));
        lblYellowCount.setText(String.valueOf(yellow));
        lblGreenCount.setText(String.valueOf(green));
        lblBlackCount.setText(String.valueOf(black));

        lblMciVictimCount.setText(victimsList.size() + " Disaster Victims Triaged");
    }

    @FXML
    public void handleToggleMci(ActionEvent event) {
        isMciActive = !isMciActive;
        if (isMciActive) {
            btnToggleMci.setText("🚨 Active MCI Mode (ON)");
            btnToggleMci.getStyleClass().setAll("btn-modern-pill-critical");
            mciBanner.setVisible(true);
            mciBanner.setManaged(true);
        } else {
            btnToggleMci.setText("Standby (MCI OFF)");
            btnToggleMci.getStyleClass().setAll("btn-modern-pill-secondary");
            mciBanner.setVisible(false);
            mciBanner.setManaged(false);
        }
    }

    @FXML
    public void handleRefreshRoster(ActionEvent event) {
        updateStats();
    }

    @FXML
    public void handleRapidAdmit(ActionEvent event) {
        String ident = txtMciIdentifier.getText() != null ? txtMciIdentifier.getText().trim() : "";
        if (ident.isEmpty()) {
            ident = "Unknown Disaster Victim (MCI-00" + mciSequence + ")";
        }

        String ageStr = txtMciAge.getText() != null && !txtMciAge.getText().trim().isEmpty() ? txtMciAge.getText().trim() : "35";
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            age = 35;
        }

        String gender = cmbMciGender.getValue();
        String injuries = txtMciInjuries.getText() != null ? txtMciInjuries.getText().trim() : "Multi-trauma blast injury";
        String bay = cmbMciBay.getValue();

        MassCasualtyTag.StartTag tag = MassCasualtyTag.StartTag.RED;
        if (rbYellow.isSelected()) tag = MassCasualtyTag.StartTag.YELLOW;
        else if (rbGreen.isSelected()) tag = MassCasualtyTag.StartTag.GREEN;
        else if (rbBlack.isSelected()) tag = MassCasualtyTag.StartTag.BLACK;

        // 1. Add to disaster roster
        MassCasualtyTag mciEntry = new MassCasualtyTag(tag, ident + " - " + injuries, bay);
        victimsList.add(0, mciEntry);
        mciSequence++;
        updateStats();

        // 2. Dispatch patient into Min-Heap queue
        EmergencyLevel mappedLevel = tag.getMappedLevel();
        String token = "MCI-" + String.format("%03d", mciSequence);

        Patient disasterPatient = new Patient(token, ident, age, gender, "EMS Disaster Intake", injuries, mappedLevel, LocalDateTime.now());
        disasterPatient.setAssignedBay(bay);
        disasterPatient.setStatus(PatientStatus.WAITING);

        queueService.enqueue(disasterPatient);

        // 3. Log audit trail
        AuditService.getInstance().log("MCI_TRIAGE_ADMIT", token, null, null, tag.getName(),
                "START Tag: " + tag.getName() + " | Location: " + bay);

        // 4. Clear rapid form
        txtMciIdentifier.clear();
        txtMciAge.clear();
        txtMciInjuries.clear();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("START Triage Rapid Admission");
        alert.setHeaderText("Disaster Casualty Admitted to Emergency Queue");
        alert.setContentText("Casualty [" + token + "] tagged " + tag.getName().toUpperCase()
                + " and immediately enqueued at priority position.");
        alert.showAndWait();
    }
}
