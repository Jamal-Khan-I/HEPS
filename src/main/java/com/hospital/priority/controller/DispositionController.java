package com.hospital.priority.controller;

import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.model.*;
import com.hospital.priority.service.*;
import com.hospital.priority.util.DischargePdfUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * DISPOSITION & DISCHARGE CONTROLLER (Page 12)
 * ============================================================================
 * Coordinates clinical outcomes, 6 selectable outcome tiles, dynamic input
 * sections, ICD-10 coding, prescriptions, bay release, and automated PDF discharge summary.
 */
public class DispositionController implements Initializable {

    @FXML private ComboBox<Patient> cmbPatientSelector;

    // Header Banner
    @FXML private HBox patientBanner;
    @FXML private StackPane avatarPane;
    @FXML private Label lblInitials;
    @FXML private Label lblPatientName;
    @FXML private Label lblDemographics;
    @FXML private Label lblToken;
    @FXML private Label lblBayDoctor;
    @FXML private Label lblAllergyTag;
    @FXML private Label lblPriorityBadge;

    // Outcome Toggle Group & 6 Interactive Tiles
    @FXML private ToggleGroup outcomeGroup;
    @FXML private VBox tileDischargeHome;
    @FXML private VBox tileAdmitWard;
    @FXML private VBox tileIcuOt;
    @FXML private VBox tileTransferOut;
    @FXML private VBox tileLama;
    @FXML private VBox tileReferred;

    @FXML private RadioButton optDischargeHome;
    @FXML private RadioButton optAdmitWard;
    @FXML private RadioButton optIcuOt;
    @FXML private RadioButton optTransferOut;
    @FXML private RadioButton optLama;
    @FXML private RadioButton optReferred;

    // Outcome Dynamic Context Panels (Page 12)
    @FXML private VBox boxOutcomeSpecificFields;
    @FXML private HBox boxAdmitFields;
    @FXML private ComboBox<String> cmbAdmitWard;
    @FXML private TextField txtAdmitBed;
    @FXML private HBox boxTransferFields;
    @FXML private TextField txtTransferHospital;
    @FXML private TextField txtTransferAmbulance;
    @FXML private HBox boxHomeFields;
    @FXML private TextField txtFollowupDate;

    // Diagnosis & Prescriptions
    @FXML private ComboBox<String> cmbIcd10;
    @FXML private TextField txtSecondaryDiag;
    @FXML private TextArea txtPrescriptions;
    @FXML private TextArea txtInstructions;

    private final QueueService queueService = QueueService.getInstance();
    private final BayService bayService = BayService.getInstance();
    private final PatientDaoImpl patientDao = new PatientDaoImpl();

    private final ObservableList<Patient> activePatients = FXCollections.observableArrayList();
    private Patient selectedPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupIcd10Codes();
        setupOutcomeControls();
        loadActivePatients();
        setupPatientSelection();
    }

    private void setupOutcomeControls() {
        if (cmbAdmitWard != null) {
            cmbAdmitWard.setItems(FXCollections.observableArrayList(
                    "General Medicine Ward (Floor 3)",
                    "Surgical Inpatient Ward (Floor 4)",
                    "Orthopedics & Trauma Unit (Floor 2)",
                    "Pediatric Ward (Floor 1)",
                    "Cardiology Monitored Step-Down (Floor 5)"
            ));
            cmbAdmitWard.getSelectionModel().selectFirst();
        }

        if (txtFollowupDate != null) {
            txtFollowupDate.setText("29 Oct 2026 at Cardiology OPD Room 12");
        }

        if (outcomeGroup != null) {
            outcomeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateTileStyles());
        }
        updateTileStyles();
    }

    @FXML
    public void selectDischargeHome() {
        if (optDischargeHome != null) optDischargeHome.setSelected(true);
        updateTileStyles();
    }

    @FXML
    public void selectAdmitWard() {
        if (optAdmitWard != null) optAdmitWard.setSelected(true);
        updateTileStyles();
    }

    @FXML
    public void selectIcuOt() {
        if (optIcuOt != null) optIcuOt.setSelected(true);
        updateTileStyles();
    }

    @FXML
    public void selectTransferOut() {
        if (optTransferOut != null) optTransferOut.setSelected(true);
        updateTileStyles();
    }

    @FXML
    public void selectLama() {
        if (optLama != null) optLama.setSelected(true);
        updateTileStyles();
    }

    @FXML
    public void selectReferred() {
        if (optReferred != null) optReferred.setSelected(true);
        updateTileStyles();
    }

    private void updateTileStyles() {
        resetTile(tileDischargeHome, optDischargeHome != null && optDischargeHome.isSelected());
        resetTile(tileAdmitWard, optAdmitWard != null && optAdmitWard.isSelected());
        resetTile(tileIcuOt, optIcuOt != null && optIcuOt.isSelected());
        resetTile(tileTransferOut, optTransferOut != null && optTransferOut.isSelected());
        resetTile(tileLama, optLama != null && optLama.isSelected());
        resetTile(tileReferred, optReferred != null && optReferred.isSelected());

        boolean isAdmit = optAdmitWard != null && optAdmitWard.isSelected();
        boolean isTransfer = optTransferOut != null && optTransferOut.isSelected();
        boolean isHomeOrRef = (optDischargeHome != null && optDischargeHome.isSelected())
                || (optReferred != null && optReferred.isSelected());

        if (boxAdmitFields != null) {
            boxAdmitFields.setVisible(isAdmit);
            boxAdmitFields.setManaged(isAdmit);
        }
        if (boxTransferFields != null) {
            boxTransferFields.setVisible(isTransfer);
            boxTransferFields.setManaged(isTransfer);
        }
        if (boxHomeFields != null) {
            boxHomeFields.setVisible(isHomeOrRef);
            boxHomeFields.setManaged(isHomeOrRef);
        }
    }

    private void resetTile(VBox tile, boolean selected) {
        if (tile == null) return;
        tile.getStyleClass().removeAll("outcome-tile", "outcome-tile-selected");
        tile.getStyleClass().add(selected ? "outcome-tile-selected" : "outcome-tile");
    }

    private void setupIcd10Codes() {
        cmbIcd10.setItems(FXCollections.observableArrayList(
                "I21.0 - Acute transmural myocardial infarction of anterior wall",
                "I20.0 - Unstable angina pectoris",
                "J44.1 - Chronic obstructive pulmonary disease with acute exacerbation",
                "J45.901 - Unspecified asthma with acute exacerbation",
                "S06.0 - Concussion / Traumatic brain injury without loss of consciousness",
                "K35.8 - Acute appendicitis, other and unspecified",
                "R07.9 - Chest pain, unspecified etiology",
                "A09 - Infectious gastroenteritis and acute colitis",
                "E11.65 - Type 2 diabetes mellitus with acute hyperglycemia",
                "I10 - Essential primary hypertension crisis"
        ));
        cmbIcd10.getSelectionModel().selectFirst();
    }

    private void loadActivePatients() {
        activePatients.clear();
        List<Patient> queue = queueService.getSortedSnapshot();
        activePatients.addAll(queue);

        try {
            List<Patient> all = patientDao.findAll();
            for (Patient p : all) {
                if (!activePatients.contains(p) && p.getStatus() != PatientStatus.COMPLETED && p.getStatus() != PatientStatus.DISCHARGED) {
                    activePatients.add(p);
                }
            }
        } catch (Exception e) {
            // Fallback gracefully
        }

        cmbPatientSelector.setItems(activePatients);
        if (!activePatients.isEmpty()) {
            cmbPatientSelector.getSelectionModel().selectFirst();
            displayPatient(activePatients.get(0));
        } else {
            displayPatient(null);
        }
    }

    private void setupPatientSelection() {
        cmbPatientSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getFullName() + " (" + p.getTokenNo() + ") - " + p.getEmergencyLevel().getDisplayName());
            }
        });
        cmbPatientSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getFullName() + " (" + p.getTokenNo() + ")");
            }
        });

        cmbPatientSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayPatient(newVal);
            }
        });
    }

    private void displayPatient(Patient p) {
        this.selectedPatient = p;
        if (p == null) {
            lblInitials.setText("--");
            lblPatientName.setText("No Patient Selected");
            lblDemographics.setText("--");
            lblToken.setText("--");
            lblBayDoctor.setText("Select an active patient to process clinical disposition");
            lblAllergyTag.setText("None");
            lblAllergyTag.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");
            lblPriorityBadge.setText("NONE");
            lblPriorityBadge.getStyleClass().setAll("badge", "badge-low");
            txtPrescriptions.clear();
            txtInstructions.clear();
            return;
        }

        lblInitials.setText(getInitials(p.getFullName()));
        lblPatientName.setText(p.getFullName());
        lblDemographics.setText(p.getAge() + "y · " + p.getGender());
        lblToken.setText(p.getTokenNo());

        String bay = p.getAssignedBay() != null ? p.getAssignedBay() : "Bay 1 · Resus";
        String doc = p.getAssignedDoctorName() != null ? p.getAssignedDoctorName() : "Attending Physician";
        lblBayDoctor.setText("📍 " + bay + "  •  👨‍⚕️ " + doc + "  •  ⏱️ " + p.getFormattedWaitTime() + " in ED");

        // Allergy
        String allergy = p.getAllergies();
        if (allergy != null && !allergy.trim().isEmpty() && !allergy.equalsIgnoreCase("None") && !allergy.equalsIgnoreCase("NKDA")) {
            lblAllergyTag.setText("⚠️ ALLERGY: " + allergy);
            lblAllergyTag.setVisible(true);
            lblAllergyTag.setManaged(true);
        } else {
            lblAllergyTag.setText("No Known Allergies");
            lblAllergyTag.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");
        }

        // Priority badge
        lblPriorityBadge.setText(p.getEmergencyLevel().getDisplayName().toUpperCase());
        lblPriorityBadge.getStyleClass().setAll("priority-badge", p.getEmergencyLevel().getCssClass());

        // Dynamic clinical prescriptions & instructions tailored to patient specialty
        if (p.getAssignedSpecialty() == Specialty.CARDIOLOGY) {
            txtPrescriptions.setText(
                    "1. Tab. Aspirin 75 mg - 1 tablet once daily after lunch x 30 days\n" +
                    "2. Tab. Clopidogrel 75 mg - 1 tablet once daily x 30 days\n" +
                    "3. Tab. Atorvastatin 40 mg - 1 tablet at bedtime x 30 days\n" +
                    "4. Tab. Pantoprazole 40 mg - 1 tablet once daily empty stomach x 14 days"
            );
            txtInstructions.setText(
                    "• Follow up in Cardiology OPD for repeat evaluation.\n" +
                    "• Maintain low-salt, low-fat cardiac diet.\n" +
                    "• EMERGENCY RED FLAGS: Return to ED immediately if severe chest pain, diaphoresis, shortness of breath, or radiating pain recurs."
            );
        } else {
            txtPrescriptions.setText(
                    "1. Tab. Paracetamol 650 mg - 1 tablet SOS (as needed for pain/fever)\n" +
                    "2. Tab. Pantoprazole 40 mg - 1 tablet once daily before breakfast x 5 days"
            );
            txtInstructions.setText(
                    "• Follow up at " + (p.getAssignedSpecialty() != null ? p.getAssignedSpecialty().getDisplayName() : "General Medicine") + " OPD within 3 to 5 days.\n" +
                    "• Rest and adequate oral hydration.\n" +
                    "• Return to Emergency if symptoms worsen or new acute warning signs develop."
            );
        }
    }

    @FXML
    public void handleRefreshPatients(ActionEvent event) {
        loadActivePatients();
    }

    private String getSelectedOutcome() {
        if (optDischargeHome != null && optDischargeHome.isSelected()) return "Discharged Home";
        if (optAdmitWard != null && optAdmitWard.isSelected()) {
            String ward = cmbAdmitWard != null && cmbAdmitWard.getValue() != null ? cmbAdmitWard.getValue() : "General Ward";
            String bed = txtAdmitBed != null && !txtAdmitBed.getText().isEmpty() ? txtAdmitBed.getText() : "Pending";
            return "Admitted to Ward (" + ward + ", " + bed + ")";
        }
        if (optIcuOt != null && optIcuOt.isSelected()) return "Transferred to ICU / Cath Lab / OT";
        if (optTransferOut != null && optTransferOut.isSelected()) {
            String hosp = txtTransferHospital != null && !txtTransferHospital.getText().isEmpty() ? txtTransferHospital.getText() : "Tertiary Centre";
            return "Transferred to Tertiary Hospital (" + hosp + ")";
        }
        if (optLama != null && optLama.isSelected()) return "Left Against Medical Advice (LAMA)";
        if (optReferred != null && optReferred.isSelected()) return "Referred Outpatient";
        return "Discharged Home";
    }

    @FXML
    public void handleGeneratePdf(ActionEvent event) {
        if (selectedPatient == null) return;
        try {
            String outcome = getSelectedOutcome();
            String icd = cmbIcd10.getValue();
            String rx = txtPrescriptions.getText();
            String inst = txtInstructions.getText();
            String doc = selectedPatient.getAssignedDoctorName() != null ? selectedPatient.getAssignedDoctorName() : UserSession.getInstance().getStaffName();

            File outFile = new File("ED_Discharge_" + selectedPatient.getTokenNo() + ".pdf");
            DischargePdfUtil.generateDischargeSummaryPdf(selectedPatient, outcome, icd, rx, inst, doc, outFile);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF Generated");
            alert.setHeaderText("Discharge Summary PDF Created Successfully");
            alert.setContentText("Document saved to:\n" + outFile.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("PDF Error");
            alert.setHeaderText("Could not create PDF discharge summary");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleMedicalCertificate(ActionEvent event) {
        if (selectedPatient == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Medical Certificate");
        alert.setHeaderText("Emergency Medical Certificate · " + selectedPatient.getTokenNo());
        alert.setContentText("This certifies that " + selectedPatient.getFullName() + " (" + selectedPatient.getAge() + "y/" + selectedPatient.getGender() +
                ") was evaluated and treated in the Emergency Department on " + java.time.LocalDate.now() +
                " for " + cmbIcd10.getValue() + ".\n\nRecommended: 3 days of medical convalescence and rest.");
        alert.showAndWait();
    }

    @FXML
    public void handleFinalizeDisposition(ActionEvent event) {
        if (selectedPatient == null) return;

        String outcome = getSelectedOutcome();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Patient Disposition");
        confirm.setHeaderText("Finalize encounter for [" + selectedPatient.getTokenNo() + "] " + selectedPatient.getFullName() + "?");
        confirm.setContentText("Outcome: " + outcome + "\nAssigned Bay: " + selectedPatient.getAssignedBay()
                + "\n\nThe patient will be archived, the bay released to 15-minute sanitization, and the audit log updated.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // 1. Remove from waiting queue
                    queueService.removeFromQueue(selectedPatient);

                    // 2. Update patient status
                    PatientStatus nextStatus = outcome.contains("Admitted") ? PatientStatus.ADMITTED
                            : (outcome.contains("ICU") || outcome.contains("Transfer") ? PatientStatus.TRANSFERRED
                            : (outcome.contains("LAMA") ? PatientStatus.LWBS
                            : (outcome.contains("Deceased") ? PatientStatus.DECEASED : PatientStatus.DISCHARGED)));

                    selectedPatient.setStatus(nextStatus);
                    patientDao.updateStatus(selectedPatient.getPatientId(), nextStatus);

                    // 3. Release bay to cleaning
                    bayService.releasePatientBay(selectedPatient.getTokenNo(), 15);

                    // 4. Log compliance audit entry
                    AuditService.getInstance().log("PATIENT_DISPOSITION", selectedPatient.getTokenNo(), selectedPatient.getUhid(),
                            "IN_TREATMENT", nextStatus.name(), "Outcome: " + outcome + " | ICD-10: " + cmbIcd10.getValue());

                    // 5. Success feedback
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Disposition Complete");
                    success.setHeaderText("Patient Encounter Finalized");
                    success.setContentText("Patient " + selectedPatient.getFullName() + " recorded as " + nextStatus.name()
                            + ".\nAssigned bay released to sanitization.");
                    success.showAndWait();

                    // Refresh
                    loadActivePatients();

                } catch (Exception e) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Disposition Error");
                    err.setHeaderText("Failed to finalize disposition");
                    err.setContentText(e.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
