package com.hospital.priority.controller;

import com.hospital.priority.dao.DoctorDao;
import com.hospital.priority.dao.impl.DoctorDaoImpl;
import com.hospital.priority.model.*;
import com.hospital.priority.service.*;
import com.hospital.priority.service.ai.AIAgentOrchestrator;
import com.hospital.priority.service.ai.AIClientService;
import com.hospital.priority.service.ai.AIClinicalExtraction;
import com.hospital.priority.util.TokenGenerator;
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
import java.time.Year;
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

    // AI Clinical Intake Nodes
    @FXML private TextArea txtAiNarrative;
    @FXML private Button btnAiAutoFill;
    @FXML private HBox boxAiStatus;
    @FXML private Label lblAiStatus;

    // Step 1: Identity Nodes
    @FXML private HBox bannerReturningPatient;
    @FXML private Label lblReturningPatientInfo;
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
    private String selectedArrivalMode = "Walk-in";

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

    // Step 2: Clinical Decision Support Nodes
    @FXML private Label lblStep2RuleLevel;
    @FXML private Label lblStep2RuleDetail;
    @FXML private Label lblStep2AiLevel;
    @FXML private Label lblStep2AiDetail;
    @FXML private HBox boxDisagreementBanner;
    @FXML private Label lblDisagreementText;

    // Step 3: Recommendation Nodes
    @FXML private Label lblRecRuleLevel;
    @FXML private Label lblRecAiLevel;
    @FXML private Label lblRecFinalLevel;
    @FXML private Label lblRecSummary;

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
    private final DoctorDao doctorDao = new DoctorDaoImpl();
    private final DoctorRoutingService routingService = DoctorRoutingService.getInstance();
    private final JavaTriageRuleEngine ruleEngine = JavaTriageRuleEngine.getInstance();
    private final AIAgentOrchestrator aiOrchestrator = AIAgentOrchestrator.getInstance();
    private final AIClientService aiClientService = AIClientService.getInstance();

    private Patient draftPatient;
    private Patient storedReturningPatient;
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

        // Initialize fresh patient draft with sequential unique token
        patientService.initializeSequence();
        initFreshPatientDraft();

        // Default arrival mode to Walk-in for new patients
        selectedArrivalMode = "Walk-in";
        updateArrivalChips();

        // Step 3 reactive listeners: changing specialty re-filters doctors; changing urgency re-filters bays
        cmbSpecialty.valueProperty().addListener((obs, oldVal, newSpecialty) -> {
            if (newSpecialty != null) {
                updateDoctorsForSpecialty(newSpecialty);
                updateRecommendationSummary();
            }
        });

        cmbFinalUrgency.valueProperty().addListener((obs, oldVal, newUrgency) -> {
            if (newUrgency != null) {
                updateBaysForLevel(newUrgency);
                updateRoutingHint(newUrgency);
                updateRecommendationSummary();
            }
        });

        // Dynamic Real-time Returning Patient Lookup when entering phone
        txtPhone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.replaceAll("[^0-9]", "").length() >= 10) {
                String cleanPhone = newVal.replaceAll("[^0-9]", "");
                try {
                    patientService.findByPhone(cleanPhone).ifPresentOrElse(p -> {
                        storedReturningPatient = p;
                        if (lblReturningPatientInfo != null) {
                            lblReturningPatientInfo.setText(p.getFullName() + " · " + p.getUhid() + " · Last visit: " + p.getFormattedArrivalTime());
                        }
                        bannerReturningPatient.setVisible(true);
                        bannerReturningPatient.setManaged(true);
                    }, () -> {
                        storedReturningPatient = null;
                        bannerReturningPatient.setVisible(false);
                        bannerReturningPatient.setManaged(false);
                    });
                } catch (Exception ignored) {
                }
            } else {
                bannerReturningPatient.setVisible(false);
                bannerReturningPatient.setManaged(false);
            }
        });

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

    private void initFreshPatientDraft() {
        draftPatient = new Patient();
        generatedToken = TokenGenerator.nextToken();
        generatedUhid = String.format("UH-%d-%06d", Year.now().getValue(), (int)(System.currentTimeMillis() % 900000) + 100000);
        draftPatient.setUhid(generatedUhid);
        draftPatient.setTokenNo(generatedToken);
    }

    // =========================================================================
    // AI CLINICAL INTAKE AUTO-FILL
    // =========================================================================

    @FXML
    public void handleAiAutoFill(ActionEvent event) {
        String narrative = txtAiNarrative != null ? txtAiNarrative.getText().trim() : "";
        if (narrative.isEmpty() && txtComplaint != null) {
            narrative = txtComplaint.getText().trim();
        }
        if (narrative.isEmpty()) {
            if (boxAiStatus != null && lblAiStatus != null) {
                boxAiStatus.setVisible(true);
                boxAiStatus.setManaged(true);
                lblAiStatus.setText("Please describe patient symptoms or paste clinical narrative.");
                lblAiStatus.setStyle("-fx-text-fill: #D97706; -fx-font-weight: 600;");
            }
            return;
        }

        if (boxAiStatus != null && lblAiStatus != null) {
            boxAiStatus.setVisible(true);
            boxAiStatus.setManaged(true);
            lblAiStatus.setText("AI analyzing clinical narrative and extracting parameters...");
            lblAiStatus.setStyle("-fx-text-fill: #0D9488; -fx-font-weight: 600;");
        }
        if (btnAiAutoFill != null) btnAiAutoFill.setDisable(true);

        aiClientService.extractClinicalNarrativeAsync(narrative).thenAccept(ext -> {
            Platform.runLater(() -> {
                if (btnAiAutoFill != null) btnAiAutoFill.setDisable(false);

                // Step 1: Demographics (populate if extracted and currently empty)
                if (ext.getFullName() != null && !ext.getFullName().isEmpty()) {
                    txtName.setText(ext.getFullName());
                }
                if (ext.getAge() != null && ext.getAge() > 0) {
                    txtAge.setText(ext.getAge() + " years");
                    draftPatient.setAge(ext.getAge());
                }
                if (ext.getGender() != null) {
                    cmbGender.getSelectionModel().select(ext.getGender());
                }
                if (ext.getPhone() != null && !ext.getPhone().isEmpty()) {
                    txtPhone.setText(ext.getPhone());
                }
                if (ext.getArrivalMode() != null) {
                    selectedArrivalMode = ext.getArrivalMode();
                    updateArrivalChips();
                }

                // Step 2: Vitals & History
                if (ext.getSpo2() != null) txtSpo2.setText(String.valueOf(ext.getSpo2()));
                if (ext.getHeartRate() != null) txtHr.setText(String.valueOf(ext.getHeartRate()));
                if (ext.getTemperature() != null) txtTemp.setText(String.valueOf(ext.getTemperature()));
                if (ext.getSystolicBp() != null) txtSysBp.setText(String.valueOf(ext.getSystolicBp()));
                if (ext.getDiastolicBp() != null) txtDiaBp.setText(String.valueOf(ext.getDiastolicBp()));
                if (ext.getRespiratoryRate() != null) txtRr.setText(String.valueOf(ext.getRespiratoryRate()));
                if (ext.getPainScore() != null) {
                    sliderPain.setValue(ext.getPainScore());
                    lblPainValue.setText(ext.getPainScore() + " / 10");
                }
                if (ext.getChiefComplaint() != null && !ext.getChiefComplaint().isEmpty()) {
                    txtComplaint.setText(ext.getChiefComplaint());
                }
                if (ext.getAllergies() != null) txtAllergies.setText(ext.getAllergies());
                if (ext.getKnownConditions() != null) txtKnownConditions.setText(ext.getKnownConditions());

                // Trigger live decision support calculation
                handleVitalsChange();

                // Step 3 Pre-configuration based on AI clinical inference
                if (ext.getSpecialty() != null) {
                    cmbSpecialty.getSelectionModel().select(ext.getSpecialty());
                    updateDoctorsForSpecialty(ext.getSpecialty());
                }
                if (ext.getUrgency() != null) {
                    cmbFinalUrgency.getSelectionModel().select(ext.getUrgency());
                    updateBaysForLevel(ext.getUrgency());
                    updateRoutingHint(ext.getUrgency());
                }
                updateRecommendationSummary();

                if (boxAiStatus != null && lblAiStatus != null) {
                    lblAiStatus.setText("[OK] AI Clinical Triage Complete. Vitals, priority, and bay routing updated.");
                    lblAiStatus.setStyle("-fx-text-fill: #0D9488; -fx-font-weight: 700;");
                }
            });
        });
    }

    // =========================================================================
    // STEP 1 HANDLERS
    // =========================================================================

    @FXML
    public void handleUseReturningRecord(ActionEvent event) {
        if (storedReturningPatient != null) {
            txtName.setText(storedReturningPatient.getFullName());
            txtPhone.setText(storedReturningPatient.getPhone());
            txtDob.setText(storedReturningPatient.getDateOfBirth() != null ? storedReturningPatient.getDateOfBirth() : "");
            txtAge.setText(storedReturningPatient.getAge() > 0 ? storedReturningPatient.getAge() + " years" : "");
            if (storedReturningPatient.getGender() != null) {
                cmbGender.getSelectionModel().select(storedReturningPatient.getGender());
            }
            txtAddress.setText(storedReturningPatient.getAddress() != null ? storedReturningPatient.getAddress() : "");
            if (storedReturningPatient.getBloodGroup() != null) {
                cmbBloodGroup.getSelectionModel().select(storedReturningPatient.getBloodGroup());
            }
            txtEmergName.setText(storedReturningPatient.getEmergencyContactName() != null ? storedReturningPatient.getEmergencyContactName() : "");
            txtEmergRelation.setText(storedReturningPatient.getEmergencyContactRelation() != null ? storedReturningPatient.getEmergencyContactRelation() : "");
            txtEmergPhone.setText(storedReturningPatient.getEmergencyContactPhone() != null ? storedReturningPatient.getEmergencyContactPhone() : "");
            txtAllergies.setText(storedReturningPatient.getAllergies() != null ? storedReturningPatient.getAllergies() : "");
            txtKnownConditions.setText(storedReturningPatient.getKnownConditions() != null ? storedReturningPatient.getKnownConditions() : "");
            bannerReturningPatient.setVisible(false);
            bannerReturningPatient.setManaged(false);
        }
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
            if (txtAiNarrative != null && (txtAiNarrative.getText() == null || txtAiNarrative.getText().trim().isEmpty())) {
                txtAiNarrative.setText(complaint);
            }
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

            // Compute live NEWS2 score
            int news2 = NEWS2Service.calculateScore(v, gcs);
            draftPatient.setNews2Score(news2);
            lblNews2ScoreBadge.setText("NEWS2: " + news2);
            lblNews2ScoreBadge.getStyleClass().removeAll("badge-critical", "badge-high", "badge-medium", "badge-low");
            lblNews2ScoreBadge.getStyleClass().add(news2 >= 7 ? "badge-critical" : (news2 >= 5 ? "badge-high" : (news2 >= 1 ? "badge-medium" : "badge-low")));

            // Dynamically update clinical decision support & AI consensus cards in Step 2
            updateClinicalDecisionSupport(v, gcs);

        } catch (Exception ignored) {
        }
    }

    private void updateClinicalDecisionSupport(Vitals v, int gcs) {
        String symptoms = txtComplaint.getText() != null ? txtComplaint.getText().trim() : "";
        int age = draftPatient.getAge() > 0 ? draftPatient.getAge() : 58;

        // 1. Rule Engine Evaluation
        JavaTriageRuleEngine.RuleEvaluation ruleEval = ruleEngine.evaluate(age, symptoms, v);
        EmergencyLevel ruleLevel = ruleEval.getRecommendedLevel();
        Specialty ruleSpec = ruleEval.getRecommendedSpecialty();

        if (lblStep2RuleLevel != null) {
            lblStep2RuleLevel.setText(ruleLevel.name());
            lblStep2RuleLevel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + getLevelColorHex(ruleLevel) + ";");
        }
        if (lblStep2RuleDetail != null) {
            String detail = !ruleEval.getTriggeredRules().isEmpty()
                    ? ruleEval.getTriggeredRules().get(0)
                    : "Stable vitals, " + ruleSpec.getDisplayName() + " evaluation";
            lblStep2RuleDetail.setText(detail);
        }

        // 2. Multi-Agent AI Assessment
        AIAssessmentResult aiResult = aiOrchestrator.assessPatient(draftPatient.getFullName(), age, draftPatient.getGender(), symptoms, v);
        EmergencyLevel aiLevel = aiResult.getSuggestedLevel();
        Specialty aiSpec = aiResult.getSuggestedSpecialty();

        if (lblStep2AiLevel != null) {
            lblStep2AiLevel.setText(aiLevel.name());
            lblStep2AiLevel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + getLevelColorHex(aiLevel) + ";");
        }
        if (lblStep2AiDetail != null) {
            String detail = aiResult.getIntakeSummary() != null && !aiResult.getIntakeSummary().isEmpty()
                    ? aiResult.getIntakeSummary()
                    : aiSpec.getDisplayName() + " clinical profile";
            lblStep2AiDetail.setText(detail);
        }

        // 3. Disagreement Detection Banner
        if (boxDisagreementBanner != null && lblDisagreementText != null) {
            boolean disagreement = ruleLevel != aiLevel;
            boxDisagreementBanner.setVisible(disagreement);
            boxDisagreementBanner.setManaged(disagreement);
            if (disagreement) {
                EmergencyLevel higherLevel = ruleLevel.getRank() < aiLevel.getRank() ? ruleLevel : aiLevel;
                lblDisagreementText.setText("Disagreement detected. Defaulting to higher acuity safety protocol: " + higherLevel.name());
            }
        }
    }

    private String getLevelColorHex(EmergencyLevel level) {
        if (level == null) return "#64748B";
        return switch (level) {
            case CRITICAL -> "#B91C1C";
            case HIGH -> "#C2410C";
            case MEDIUM -> "#D97706";
            case LOW -> "#0D9488";
        };
    }

    @FXML
    public void handleContinueToDecision(ActionEvent event) {
        draftPatient.setSymptoms(txtComplaint.getText());
        draftPatient.setAllergies(txtAllergies.getText());
        draftPatient.setKnownConditions(txtKnownConditions.getText());
        draftPatient.setPainScore((int) sliderPain.getValue());

        int age = draftPatient.getAge() > 0 ? draftPatient.getAge() : 58;
        String symptoms = draftPatient.getSymptoms();
        Vitals v = draftPatient.getVitals();
        try {
            int spo2 = Integer.parseInt(txtSpo2.getText().trim());
            int hr = Integer.parseInt(txtHr.getText().trim());
            double temp = Double.parseDouble(txtTemp.getText().trim());
            int sys = Integer.parseInt(txtSysBp.getText().trim());
            int dia = Integer.parseInt(txtDiaBp.getText().trim());
            int rr = Integer.parseInt(txtRr.getText().trim());
            v = new Vitals(spo2, hr, temp, sys, dia, rr);
            draftPatient.setVitals(v);
        } catch (Exception ignored) {
            if (v == null) {
                v = new Vitals(97, 82, 98.6, 120, 80, 16);
                draftPatient.setVitals(v);
            }
        }

        JavaTriageRuleEngine.RuleEvaluation ruleEval = ruleEngine.evaluate(age, symptoms, v);
        AIAssessmentResult aiResult = aiOrchestrator.assessPatient(draftPatient.getFullName(), age, draftPatient.getGender(), symptoms, v);

        // Deterministic safety guardrail: higher acuity wins (CRITICAL=1, HIGH=2, MEDIUM=3, LOW=4)
        EmergencyLevel finalLevel = ruleEval.getRecommendedLevel().getRank() < aiResult.getSuggestedLevel().getRank()
                ? ruleEval.getRecommendedLevel()
                : aiResult.getSuggestedLevel();
        Specialty finalSpecialty = ruleEval.getRecommendedSpecialty() != null
                ? ruleEval.getRecommendedSpecialty()
                : aiResult.getSuggestedSpecialty();

        draftPatient.setEmergencyLevel(finalLevel);
        draftPatient.setAssignedSpecialty(finalSpecialty);
        draftPatient.setAiSuggestedLevel(aiResult.getSuggestedLevel());
        draftPatient.setRuleSuggestedLevel(ruleEval.getRecommendedLevel());

        // Update Step 3 Recommendation Header
        if (lblRecRuleLevel != null) lblRecRuleLevel.setText(ruleEval.getRecommendedLevel().name());
        if (lblRecAiLevel != null) lblRecAiLevel.setText(aiResult.getSuggestedLevel().name());
        if (lblRecFinalLevel != null) {
            lblRecFinalLevel.setText(finalLevel.name());
            lblRecFinalLevel.getStyleClass().removeAll("badge-critical", "badge-high", "badge-medium", "badge-low");
            lblRecFinalLevel.getStyleClass().add(finalLevel.getCssBadgeClass());
        }

        // Set ComboBox selections based on clinical evaluation
        cmbFinalUrgency.getSelectionModel().select(finalLevel);
        cmbSpecialty.getSelectionModel().select(finalSpecialty);

        // Populate doctors and bays for the dynamically inferred specialty and level
        updateDoctorsForSpecialty(finalSpecialty);
        updateBaysForLevel(finalLevel);
        updateRoutingHint(finalLevel);
        updateRecommendationSummary();

        setStep(3);
    }

    private void updateDoctorsForSpecialty(Specialty specialty) {
        if (specialty == null) return;
        List<String> docItems = new ArrayList<>();
        Doctor bestDoc = null;
        try {
            DoctorRoutingService.RoutingDecision decision = routingService.routeDoctor(specialty);
            bestDoc = decision != null ? decision.getAssignedDoctor() : null;
            List<Doctor> specialists = doctorDao.findBySpecialty(specialty);
            for (Doctor d : specialists) {
                docItems.add(d.getName() + " · " + d.getStatus().getDisplayName().toLowerCase());
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load doctors from DB: " + e.getMessage());
        }

        // Fallbacks for any specialty if DB list was empty
        if (docItems.isEmpty()) {
            switch (specialty) {
                case PULMONOLOGY -> {
                    docItems.add("Dr. Ananya Sen · available");
                    docItems.add("Dr. Sneha Reddy · available");
                }
                case CARDIOLOGY -> {
                    docItems.add("Dr. Rajesh Sharma · available");
                    docItems.add("Dr. Arjun Mehta · available");
                    docItems.add("Dr. Rao · busy");
                }
                case NEUROLOGY -> {
                    docItems.add("Dr. Priya Nair · available");
                    docItems.add("Dr. Verma · available");
                }
                case ORTHOPEDICS -> {
                    docItems.add("Dr. Vikram Patel · available");
                    docItems.add("Dr. Joshi · busy");
                }
                case PEDIATRICS -> {
                    docItems.add("Dr. Sneha Reddy · available");
                    docItems.add("Dr. Ana · available");
                }
                case TRAUMA_SURGERY -> {
                    docItems.add("Dr. Arjun Mehta · available");
                    docItems.add("Dr. Vikram Patel · available");
                }
                default -> {
                    docItems.add("Dr. David Miller · available");
                    docItems.add("Dr. Kavita Verma · available");
                }
            }
        }

        cmbDoctor.setItems(FXCollections.observableArrayList(docItems));
        if (bestDoc != null) {
            String bestName = bestDoc.getName();
            for (String item : docItems) {
                if (item.toLowerCase().contains(bestName.toLowerCase())) {
                    cmbDoctor.getSelectionModel().select(item);
                    return;
                }
            }
        }
        if (!docItems.isEmpty()) {
            cmbDoctor.getSelectionModel().select(0);
        }
    }

    private void updateBaysForLevel(EmergencyLevel level) {
        if (level == null) level = EmergencyLevel.LOW;
        List<Bay> matchedBays = bayService.getAvailableBaysForLevel(level);
        List<String> bayItems = new ArrayList<>();
        String bestFreeBay = null;

        for (Bay b : matchedBays) {
            String label = b.getName() + " (" + (b.isAvailable() ? "free" : b.getStatus().getDisplayName().toLowerCase()) + ")";
            bayItems.add(label);
            if (b.isAvailable() && bestFreeBay == null) {
                bestFreeBay = label;
            }
        }

        // Also add other bays so doctor can pick any free bay
        for (Bay b : bayService.getAllBays()) {
            String label = b.getName() + " (" + (b.isAvailable() ? "free" : b.getStatus().getDisplayName().toLowerCase()) + ")";
            if (!bayItems.contains(label)) {
                bayItems.add(label);
                if (b.isAvailable() && bestFreeBay == null) {
                    bestFreeBay = label;
                }
            }
        }

        cmbBay.setItems(FXCollections.observableArrayList(bayItems));
        if (bestFreeBay != null) {
            cmbBay.getSelectionModel().select(bestFreeBay);
        } else if (!bayItems.isEmpty()) {
            cmbBay.getSelectionModel().select(0);
        }
    }

    private void updateRoutingHint(EmergencyLevel level) {
        if (level == null) level = EmergencyLevel.LOW;
        String hint = switch (level) {
            case CRITICAL -> "Critical: immediate resuscitation care. Target wait: 0 min (SLA: 15m).";
            case HIGH -> "High priority: urgent clinical care. Target wait: <15 min (SLA: 15m).";
            case MEDIUM -> "Medium priority: monitored observation. Target wait: <45 min (SLA: 60m).";
            case LOW -> "Low priority: ambulatory care and evaluation. Target wait: <90 min (SLA: 120m).";
        };
        lblRoutingHint.setText(hint);
    }

    private void updateRecommendationSummary() {
        Specialty s = cmbSpecialty.getSelectionModel().getSelectedItem();
        String doc = cmbDoctor.getSelectionModel().getSelectedItem();
        String bay = cmbBay.getSelectionModel().getSelectedItem();
        String specName = s != null ? s.getDisplayName() : "General Emergency";
        String bayName = bay != null ? bay : "Bay 1 - Resus";
        String docName = doc != null ? doc : "Dr. On-Duty";
        if (lblRecSummary != null) {
            lblRecSummary.setText("Suggested: " + specName + " · " + bayName + " · " + docName);
        }
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
        draftPatient.setAssignedSpecialty(specialty != null ? specialty : Specialty.GENERAL_EMERGENCY);
        draftPatient.setAssignedDoctorName(doctor != null ? doctor.split("·")[0].split("-")[0].trim() : "Dr. On-Duty");
        String requestedBay = bay != null ? bay.split("\\(")[0].trim() : "Bay 1 - Resus";
        draftPatient.setAssignedBay(requestedBay);
        draftPatient.setStatus(PatientStatus.WAITING);
        draftPatient.setArrivalTime(LocalDateTime.now());

        try {
            // Guarantee a unique sequential token so duplicate key never occurs
            String currentToken = draftPatient.getTokenNo();
            if (currentToken == null || currentToken.isEmpty() || patientService.findByToken(currentToken).isPresent()) {
                String freshToken = TokenGenerator.nextToken();
                draftPatient.setTokenNo(freshToken);
            }
            if (draftPatient.getUhid() == null || draftPatient.getUhid().isEmpty()) {
                draftPatient.setUhid(String.format("UH-%d-%06d", Year.now().getValue(), (int)(System.currentTimeMillis() % 900000) + 100000));
            }

            // 1. Allocate & occupy Bay in BayService (ensures physical bay occupancy)
            Bay allocatedBay = bayService.allocateBayForPatient(draftPatient.getAssignedBay(), draftPatient);
            if (allocatedBay != null) {
                draftPatient.setAssignedBay(allocatedBay.getName());
            }

            // 2. Insert patient into database (persists confirmed allocated bay)
            patientService.registerPatient(draftPatient);

            // 3. Enqueue into Min-Heap PriorityQueue
            queueService.enqueue(draftPatient);

            // 4. Log compliance audit entry (Page 18)
            AuditService.getInstance().log("PATIENT_REGISTERED", draftPatient.getTokenNo(), draftPatient.getUhid(),
                    "REGISTERED", "WAITING", "Triage registration completed. Priority: " + draftPatient.getEmergencyLevel().name()
                            + ", Specialty: " + draftPatient.getAssignedSpecialty() + ", Bay: " + draftPatient.getAssignedBay());

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
