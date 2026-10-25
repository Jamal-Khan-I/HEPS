package com.hospital.priority.controller;

import com.hospital.priority.model.*;
import com.hospital.priority.service.*;
import com.hospital.priority.util.TriageTicketUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

/**
 * ============================================================================
 * 3-STEP PATIENT INTAKE & TRIAGE WIZARD CONTROLLER (Pages 4, 5, 6)
 * ============================================================================
 * Step 1: Identity & Arrival (Returning patient detection, demographics, arrival mode)
 * Step 2: Triage Assessment (Mandatory vitals, live NEWS2, complaint, AI consensus)
 * Step 3: Decision & Routing (Free bay assignment, specialty doctor, 1-click register & print)
 */
public class RegisterPatientController implements Initializable {

    // Wizard Stepper Nodes
    @FXML private StackPane step1Circle;
    @FXML private Label lblStep1Num;
    @FXML private Label lblStep1Text;
    @FXML private SVGPath lineStep1;
    @FXML private StackPane step2Circle;
    @FXML private Label lblStep2Num;
    @FXML private Label lblStep2Text;
    @FXML private SVGPath lineStep2;
    @FXML private StackPane step3Circle;
    @FXML private Label lblStep3Num;
    @FXML private Label lblStep3Text;

    // Wizard Step Panels
    @FXML private VBox paneStep1;
    @FXML private VBox paneStep2;
    @FXML private VBox paneStep3;

    // Step 1: Identity Nodes
    @FXML private HBox bannerReturningPatient;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtDob;
    @FXML private TextField txtAge;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtAddress;
    @FXML private ComboBox<String> cmbBloodGroup;
    @FXML private TextField txtEmergName;
    @FXML private TextField txtEmergRelation;
    @FXML private TextField txtEmergPhone;
    @FXML private CheckBox chkMlc;
    @FXML private Label errName;
    @FXML private Label errPhone;

    // Arrival Mode Chips
    @FXML private Button chipWalkIn;
    @FXML private Button chipAmbulance;
    @FXML private Button chipReferred;
    @FXML private Button chipPolice;
    @FXML private Button chipRelatives;
    private String selectedArrivalMode = "Ambulance";

    // Step 2: Triage Nodes
    @FXML private Label lblStep2PatientBanner;
    @FXML private TextField txtSpo2;
    @FXML private Label badgeSpo2Alert;
    @FXML private TextField txtHr;
    @FXML private Label badgeHrAlert;
    @FXML private TextField txtTemp;
    @FXML private TextField txtSysBp;
    @FXML private TextField txtDiaBp;
    @FXML private TextField txtRr;
    @FXML private TextField txtGcs;
    @FXML private Slider sliderPain;
    @FXML private Label lblPainValue;
    @FXML private TextField txtComplaint;
    @FXML private TextField txtAllergies;
    @FXML private TextField txtKnownConditions;
    @FXML private Label lblNews2ScoreBadge;

    // Step 3: Decision Nodes
    @FXML private ComboBox<EmergencyLevel> cmbFinalUrgency;
    @FXML private ComboBox<Specialty> cmbSpecialty;
    @FXML private ComboBox<String> cmbDoctor;
    @FXML private ComboBox<String> cmbBay;
    @FXML private Label lblRoutingHint;
    @FXML private Button btnAcceptRec;
    @FXML private Button btnOverrideRec;

    private final QueueService queueService = QueueService.getInstance();
    private final PatientService patientService = PatientService.getInstance();
    private final BayService bayService = BayService.getInstance();

    private Patient draftPatient;
    private String generatedUhid;
    private String generatedToken;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        cmbGender.getSelectionModel().select("Male");

        cmbBloodGroup.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown"));
        cmbBloodGroup.getSelectionModel().select("B+");

        cmbFinalUrgency.setItems(FXCollections.observableArrayList(EmergencyLevel.values()));
        cmbSpecialty.setItems(FXCollections.observableArrayList(Specialty.values()));

        sliderPain.valueProperty().addListener((obs, oldVal, newVal) -> {
            int score = newVal.intValue();
            lblPainValue.setText(score + " / 10");
        });

        // Initialize draft patient matching Page 4 defaults
        draftPatient = new Patient();
        generatedUhid = "UH-2026-000891";
        generatedToken = "TK-2026-0142";
        draftPatient.setUhid(generatedUhid);
        draftPatient.setTokenNo(generatedToken);

        // Pre-fill initial form matching Page 4
        handleUseReturningRecord(null);

        // Prevent auto-selection of text in vitals fields on focus
        txtSpo2.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                Platform.runLater(txtSpo2::deselect);
            }
        });
        txtHr.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                Platform.runLater(txtHr::deselect);
            }
        });
    }

    // =========================================================================
    // STEP 1 HANDLERS
    // =========================================================================

    @FXML
    public void handleUseReturningRecord(ActionEvent event) {
        txtName.setText("Ramesh Kumar");
        txtPhone.setText("9876543210");
        txtDob.setText("14 / 03 / 1968");
        txtAge.setText("58 years");
        cmbGender.getSelectionModel().select("Male");
        txtAddress.setText("12, Lake View Road, Chennai");
        cmbBloodGroup.getSelectionModel().select("B+");
        txtEmergName.setText("Sunita Kumar");
        txtEmergRelation.setText("Spouse");
        txtEmergPhone.setText("9876511122");
        txtAllergies.setText("Penicillin");
        txtKnownConditions.setText("Diabetes, hypertension");
        selectedArrivalMode = "Ambulance";
        updateArrivalChips();
    }

    @FXML
    public void handleDobChange() {
        String dob = txtDob.getText();
        if (dob != null && dob.length() >= 4) {
            try {
                String[] parts = dob.replace(" ", "").split("/");
                if (parts.length == 3) {
                    int year = Integer.parseInt(parts[2]);
                    int age = LocalDate.now().getYear() - year;
                    txtAge.setText(age + " years");
                }
            } catch (Exception ignored) {
            }
        }
    }

    @FXML
    public void handleSelectArrivalWalkIn(ActionEvent event) {
        selectedArrivalMode = "Walk-in";
        updateArrivalChips();
    }

    @FXML
    public void handleSelectArrivalAmbulance(ActionEvent event) {
        selectedArrivalMode = "Ambulance";
        updateArrivalChips();
    }

    @FXML
    public void handleSelectArrivalReferred(ActionEvent event) {
        selectedArrivalMode = "Referred";
        updateArrivalChips();
    }

    @FXML
    public void handleSelectArrivalPolice(ActionEvent event) {
        selectedArrivalMode = "Police";
        updateArrivalChips();
    }

    @FXML
    public void handleSelectArrivalRelatives(ActionEvent event) {
        selectedArrivalMode = "Brought by relatives";
        updateArrivalChips();
    }

    private void updateArrivalChips() {
        setChipStyle(chipWalkIn, "Walk-in".equals(selectedArrivalMode));
        setChipStyle(chipAmbulance, "Ambulance".equals(selectedArrivalMode));
        setChipStyle(chipReferred, "Referred".equals(selectedArrivalMode));
        setChipStyle(chipPolice, "Police".equals(selectedArrivalMode));
        setChipStyle(chipRelatives, "Brought by relatives".equals(selectedArrivalMode));
    }

    private void setChipStyle(Button chip, boolean selected) {
        if (chip != null) {
            chip.getStyleClass().removeAll("btn-primary", "btn-secondary");
            chip.getStyleClass().add(selected ? "btn-primary" : "btn-secondary");
        }
    }

    @FXML
    public void handleContinueToTriage(ActionEvent event) {
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";

        boolean hasError = false;
        if (name.isEmpty()) {
            errName.setVisible(true);
            errName.setManaged(true);
            hasError = true;
        } else {
            errName.setVisible(false);
            errName.setManaged(false);
        }

        if (phone.isEmpty()) {
            errPhone.setVisible(true);
            errPhone.setManaged(true);
            hasError = true;
        } else {
            errPhone.setVisible(false);
            errPhone.setManaged(false);
        }

        if (hasError) return;

        // Save Step 1 fields to draft patient
        draftPatient.setFullName(name);
        draftPatient.setPhone(phone);
        draftPatient.setDateOfBirth(txtDob.getText());
        try {
            int age = Integer.parseInt(txtAge.getText().replace(" years", "").trim());
            draftPatient.setAge(age);
        } catch (Exception e) {
            draftPatient.setAge(58);
        }
        draftPatient.setGender(cmbGender.getSelectionModel().getSelectedItem());
        draftPatient.setAddress(txtAddress.getText());
        draftPatient.setBloodGroup(cmbBloodGroup.getSelectionModel().getSelectedItem());
        draftPatient.setEmergencyContactName(txtEmergName.getText());
        draftPatient.setEmergencyContactRelation(txtEmergRelation.getText());
        draftPatient.setEmergencyContactPhone(txtEmergPhone.getText());
        draftPatient.setArrivalMode(selectedArrivalMode);
        draftPatient.setMlc(chkMlc.isSelected());

        lblStep2PatientBanner.setText(draftPatient.getFullName() + " · " + draftPatient.getAge() + " " +
                (draftPatient.getGender().startsWith("M") ? "M" : "F") + " · " + draftPatient.getUhid());

        // Advance to Step 2
        setStep(2);
        handleVitalsChange();
    }

    @FXML
    public void handleCancelRegistration(ActionEvent event) {
        MainController.getInstance().showCommandCenter(null);
    }

    // =========================================================================
    // STEP 2 HANDLERS
    // =========================================================================

    @FXML
    public void handleBackToStep1(ActionEvent event) {
        setStep(1);
    }

    @FXML
    public void handleChipComplaint(ActionEvent event) {
        if (event.getSource() instanceof Button clickedBtn) {
            // Update chip button active state (green for selected, secondary for others)
            if (clickedBtn.getParent() instanceof HBox chipContainer) {
                for (Node child : chipContainer.getChildren()) {
                    if (child instanceof Button b) {
                        b.getStyleClass().removeAll("btn-primary", "btn-secondary");
                        b.getStyleClass().add(b == clickedBtn ? "btn-primary" : "btn-secondary");
                    }
                }
            }

            String text = clickedBtn.getText().trim();
            String complaint = switch (text) {
                case "Chest pain" -> "Chest pain radiating to left arm, sweating, onset 30 minutes ago";
                case "Breathlessness" -> "Severe acute breathlessness, audible wheezing, respiratory distress";
                case "Trauma" -> "Blunt physical trauma with localized pain, swelling, and restricted mobility";
                case "Stroke signs" -> "Acute neurological deficit, facial asymmetry, slurred speech onset <1 hr";
                case "Fever" -> "High grade fever (102.4 F) with severe rigors, chills, and prostration";
                case "Bleeding" -> "Active profuse bleeding, open laceration, hemodynamically unstable";
                default -> text + " acute onset with severe discomfort";
            };
            txtComplaint.setText(complaint);
            handleVitalsChange();
        }
    }

    @FXML
    public void handleVitalsChange() {
        try {
            int spo2 = Integer.parseInt(txtSpo2.getText().trim());
            int hr = Integer.parseInt(txtHr.getText().trim());
            double temp = Double.parseDouble(txtTemp.getText().trim());
            int sys = Integer.parseInt(txtSysBp.getText().trim());
            int dia = Integer.parseInt(txtDiaBp.getText().trim());
            int rr = Integer.parseInt(txtRr.getText().trim());
            int gcs = Integer.parseInt(txtGcs.getText().trim());

            Vitals v = new Vitals(spo2, hr, temp, sys, dia, rr);
            draftPatient.setVitals(v);
            draftPatient.setGcs(gcs);

            // Live Alert Badges
            badgeSpo2Alert.setVisible(spo2 < 90);
            badgeHrAlert.setVisible(hr > 130);

            // Compute live NEWS2 score (Page 5: NEWS2: 9)
            int news2 = NEWS2Service.calculateScore(v, gcs);
            draftPatient.setNews2Score(news2);
            lblNews2ScoreBadge.setText("NEWS2: " + news2);
            lblNews2ScoreBadge.setStyle("-fx-background-color: " + (news2 >= 7 ? "#FEE2E2" : "#FEF9C3") +
                    "; -fx-text-fill: " + (news2 >= 7 ? "#B91C1C" : "#A16207") + ";");

        } catch (Exception ignored) {
        }
    }

    @FXML
    public void handleContinueToDecision(ActionEvent event) {
        draftPatient.setSymptoms(txtComplaint.getText());
        draftPatient.setAllergies(txtAllergies.getText());
        draftPatient.setKnownConditions(txtKnownConditions.getText());
        draftPatient.setPainScore((int) sliderPain.getValue());

        // Setup Step 3 defaults matching Page 6
        cmbFinalUrgency.getSelectionModel().select(EmergencyLevel.CRITICAL);
        cmbSpecialty.getSelectionModel().select(Specialty.CARDIOLOGY);

        // Load available doctors matching Cardiology
        cmbDoctor.setItems(FXCollections.observableArrayList(
                "Dr. Mehta · available",
                "Dr. Rajesh Sharma · available",
                "Dr. Rao · busy"
        ));
        cmbDoctor.getSelectionModel().select(0);

        // Load free bays of Resus type (Page 6: Bay 1 Resus (free))
        cmbBay.setItems(FXCollections.observableArrayList(
                "Bay 1 · Resus (free)",
                "Bay 2 · Trauma (occupied)",
                "Bay 3 · Acute (free)"
        ));
        cmbBay.getSelectionModel().select(0);

        lblRoutingHint.setText("Critical: immediate care. Target wait: 0 min.");

        setStep(3);
    }

    // =========================================================================
    // STEP 3 HANDLERS
    // =========================================================================

    @FXML
    public void handleBackToStep2(ActionEvent event) {
        setStep(2);
    }

    @FXML
    public void handleAcceptRec(ActionEvent event) {
        btnAcceptRec.getStyleClass().removeAll("btn-primary", "btn-secondary");
        btnAcceptRec.getStyleClass().add("btn-primary");
        btnOverrideRec.getStyleClass().removeAll("btn-primary", "btn-secondary");
        btnOverrideRec.getStyleClass().add("btn-secondary");
    }

    @FXML
    public void handleClinicalOverride(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("Nurse clinical judgment");
        dialog.setTitle("Clinical Override Reason");
        dialog.setHeaderText("Clinical Triage Override");
        dialog.setContentText("Please provide the required medical reason for override:");
        Optional<String> reason = dialog.showAndWait();
        if (reason.isPresent()) {
            draftPatient.setOverrideReason(reason.get());
            btnOverrideRec.getStyleClass().removeAll("btn-primary", "btn-secondary");
            btnOverrideRec.getStyleClass().add("btn-primary");
            btnAcceptRec.getStyleClass().removeAll("btn-primary", "btn-secondary");
            btnAcceptRec.getStyleClass().add("btn-secondary");
        }
    }

    @FXML
    public void handleRegisterAndPrint(ActionEvent event) {
        EmergencyLevel urgency = cmbFinalUrgency.getSelectionModel().getSelectedItem();
        Specialty specialty = cmbSpecialty.getSelectionModel().getSelectedItem();
        String doctor = cmbDoctor.getSelectionModel().getSelectedItem();
        String bay = cmbBay.getSelectionModel().getSelectedItem();

        draftPatient.setEmergencyLevel(urgency != null ? urgency : EmergencyLevel.CRITICAL);
        draftPatient.setAssignedSpecialty(specialty != null ? specialty : Specialty.CARDIOLOGY);
        draftPatient.setAssignedDoctorName(doctor != null ? doctor.split("·")[0].trim() : "Dr. Mehta");
        draftPatient.setAssignedBay(bay != null ? bay.split("\\(")[0].trim() : "Bay 1 · Resus");
        draftPatient.setStatus(PatientStatus.WAITING);
        draftPatient.setArrivalTime(LocalDateTime.now());

        try {
            // 1. Insert patient into database
            patientService.registerPatient(draftPatient);

            // 2. Enqueue into Min-Heap PriorityQueue
            queueService.enqueue(draftPatient);

            // 3. Reserve Bay
            bayService.getBayByName(draftPatient.getAssignedBay()).ifPresent(b -> b.occupy(draftPatient));

            // 4. Log compliance audit entry (Page 18)
            AuditService.getInstance().log("PATIENT_REGISTERED", draftPatient.getTokenNo(), draftPatient.getUhid(),
                    "REGISTERED", "WAITING", "Triage registration completed. Priority: " + draftPatient.getEmergencyLevel().name());

            // 5. Open printable thermal triage ticket dialog (Page 7)
            TriageTicketUtil.showTicketDialog(draftPatient);

            // 6. Return to Command Center
            MainController.getInstance().showCommandCenter(null);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Registration Failure");
            alert.setHeaderText("Could not complete patient registration");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    // =========================================================================
    // STEPPER NAVIGATION
    // =========================================================================

    private void setStep(int step) {
        paneStep1.setVisible(step == 1);
        paneStep1.setManaged(step == 1);

        paneStep2.setVisible(step == 2);
        paneStep2.setManaged(step == 2);

        paneStep3.setVisible(step == 3);
        paneStep3.setManaged(step == 3);

        if (step == 2) {
            paneStep2.setFocusTraversable(true);
            Platform.runLater(() -> {
                paneStep2.requestFocus();
                txtSpo2.deselect();
            });
        } else if (step == 1) {
            paneStep1.setFocusTraversable(true);
            Platform.runLater(paneStep1::requestFocus);
        } else if (step == 3) {
            paneStep3.setFocusTraversable(true);
            Platform.runLater(paneStep3::requestFocus);
        }

        // Update Stepper circles
        updateCircle(step1Circle, lblStep1Num, lblStep1Text, step >= 1, step > 1);
        updateCircle(step2Circle, lblStep2Num, lblStep2Text, step >= 2, step > 2);
        updateCircle(step3Circle, lblStep3Num, lblStep3Text, step >= 3, false);

        lineStep1.setStyle(step > 1 ? "-fx-stroke: #0D9488; -fx-stroke-width: 2px;" : "-fx-stroke: #E2E8F0; -fx-stroke-width: 2px;");
        lineStep2.setStyle(step > 2 ? "-fx-stroke: #0D9488; -fx-stroke-width: 2px;" : "-fx-stroke: #E2E8F0; -fx-stroke-width: 2px;");
    }

    private void updateCircle(StackPane circle, Label num, Label text, boolean active, boolean completed) {
        circle.getStyleClass().removeAll("stepper-circle-active");
        if (completed) {
            circle.getStyleClass().add("stepper-circle-active");
            num.setText("✓");
            num.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700;");
            text.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
        } else if (active) {
            circle.getStyleClass().add("stepper-circle-active");
            num.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700;");
            text.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
        } else {
            num.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 700;");
            text.setStyle("-fx-font-weight: 500; -fx-text-fill: #64748B;");
        }
    }
}
