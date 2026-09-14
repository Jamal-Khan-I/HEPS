package com.hospital.priority.controller;

import com.hospital.priority.exception.InvalidPatientDataException;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.PatientService;
import com.hospital.priority.util.TokenGenerator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class RegisterPatientController implements Initializable {

    @FXML private TextField txtFullName;
    @FXML private TextField txtAge;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<EmergencyLevel> cmbEmergencyLevel;
    @FXML private Label lblLevelDescription;
    @FXML private TextArea txtSymptoms;
    @FXML private Label lblPreviewToken;
    @FXML private Label lblFormMessage;
    @FXML private Button btnRegister;

    private final PatientService patientService = PatientService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFormControls();
    }

    private void setupFormControls() {
        cmbGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        cmbGender.setValue("Male");

        cmbEmergencyLevel.setItems(FXCollections.observableArrayList(EmergencyLevel.values()));
        cmbEmergencyLevel.setValue(EmergencyLevel.MEDIUM);

        cmbEmergencyLevel.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                switch (newVal) {
                    case CRITICAL -> lblLevelDescription.setText("Severe airway obstruction, acute respiratory distress, unresponsive");
                    case HIGH -> lblLevelDescription.setText("Severe uncontrolled hemorrhage, major open trauma, altered consciousness");
                    case MEDIUM -> lblLevelDescription.setText("Moderate acute distress, suspected fracture, persistent fever");
                    case LOW -> lblLevelDescription.setText("Minor superficial abrasion, routine dressing, mild sprain");
                }
            }
        });

        // Set initial description
        lblLevelDescription.setText("Moderate acute distress, suspected fracture, persistent fever");
        lblPreviewToken.setText("Token generated after successful registration.");
    }

    @FXML
    private void handleRegisterPatient(ActionEvent event) {
        String fullName = txtFullName.getText();
        String age = txtAge.getText();
        String gender = cmbGender.getValue();
        String phone = txtPhone.getText();
        EmergencyLevel level = cmbEmergencyLevel.getValue();
        String symptoms = txtSymptoms.getText();

        try {
            Patient registered = patientService.registerPatient(fullName, age, gender, phone, symptoms, level);

            // Update preview token with the generated token
            lblPreviewToken.setText("Assigned: " + registered.getTokenNo());

            if (MainController.getInstance() != null) {
                MainController.getInstance().updateSidebarCounters();
            }

            Alert successAlert = new Alert(Alert.AlertType.CONFIRMATION);
            successAlert.setTitle("Patient Registered & Enqueued");
            successAlert.setHeaderText("Success! Emergency Token Assigned: " + registered.getTokenNo());
            successAlert.setContentText("Patient: " + registered.getFullName() + "\n"
                    + "Urgency: " + registered.getEmergencyLevel() + "\n"
                    + "Position in Triage: Enqueued in PriorityQueue\n\n"
                    + "Would you like to view the Emergency Queue now?");

            ButtonType btnViewQueue = new ButtonType("View Emergency Queue", ButtonBar.ButtonData.YES);
            ButtonType btnAddAnother = new ButtonType("Register Another", ButtonBar.ButtonData.NO);
            successAlert.getButtonTypes().setAll(btnViewQueue, btnAddAnother);

            Optional<ButtonType> choice = successAlert.showAndWait();
            handleClearForm(null);

            if (choice.isPresent() && choice.get() == btnViewQueue) {
                if (MainController.getInstance() != null) {
                    MainController.getInstance().showEmergencyQueue(null);
                }
            }
        } catch (InvalidPatientDataException e) {
            showErrorAlert("Validation Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Database Persistence Error", "Failed to register patient: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearForm(ActionEvent event) {
        txtFullName.clear();
        txtAge.clear();
        cmbGender.setValue("Male");
        txtPhone.clear();
        cmbEmergencyLevel.setValue(EmergencyLevel.MEDIUM);
        txtSymptoms.clear();
        lblPreviewToken.setText("Token generated after successful registration.");
        lblFormMessage.setVisible(false);
        txtFullName.requestFocus();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
