package com.hospital.priority.controller;

import com.hospital.priority.App;
import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.UserSession;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Controller for the Hospital Staff Login screen (Page 3).
 * Supports quick-fill demo chips for fast evaluator access and audit logging.
 */
public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Button btnSignIn;
    @FXML private Label lblError;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbRole.setItems(FXCollections.observableArrayList(
                "Admin",
                "Doctor",
                "Triage nurse",
                "Receptionist",
                "Charge nurse"
        ));

        // When role dropdown selection changes, dynamically update username and password
        cmbRole.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applyRoleCredentials(newVal);
                clearInputHighlight();
            }
        });

        // Set initial role & credentials to Admin
        cmbRole.getSelectionModel().select("Admin");
        applyRoleCredentials("Admin");

        // Prevent initial focus selection/highlight in username box
        clearInputHighlight();

        if (txtUsername != null) {
            txtUsername.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    clearInputHighlight();
                }
            });
        }
    }

    private void clearInputHighlight() {
        javafx.application.Platform.runLater(() -> {
            if (btnSignIn != null) {
                btnSignIn.requestFocus();
            }
            if (txtUsername != null) {
                txtUsername.deselect();
            }
            if (txtPassword != null) {
                txtPassword.deselect();
            }
        });
    }

    private void applyRoleCredentials(String roleTitle) {
        if (roleTitle == null) return;
        switch (roleTitle) {
            case "Receptionist" -> {
                txtUsername.setText("desk.ravi");
                txtPassword.setText("recep123");
            }
            case "Doctor" -> {
                txtUsername.setText("dr.mehta");
                txtPassword.setText("doc123");
            }
            case "Charge nurse" -> {
                txtUsername.setText("charge.anita");
                txtPassword.setText("charge123");
            }
            case "Admin" -> {
                txtUsername.setText("admin");
                txtPassword.setText("admin123");
            }
            case "Triage nurse" -> {
                txtUsername.setText("nurse.priya");
                txtPassword.setText("triage123");
            }
            default -> {
                txtUsername.setText("nurse.priya");
                txtPassword.setText("triage123");
            }
        }
        hideError();
    }

    @FXML
    public void fillReceptionist(ActionEvent event) {
        cmbRole.getSelectionModel().select("Receptionist");
    }

    @FXML
    public void fillTriageNurse(ActionEvent event) {
        cmbRole.getSelectionModel().select("Triage nurse");
    }

    @FXML
    public void fillDoctor(ActionEvent event) {
        cmbRole.getSelectionModel().select("Doctor");
    }

    @FXML
    public void fillChargeNurse(ActionEvent event) {
        cmbRole.getSelectionModel().select("Charge nurse");
    }

    @FXML
    public void fillAdmin(ActionEvent event) {
        cmbRole.getSelectionModel().select("Admin");
    }

    @FXML
    public void handleSignIn(ActionEvent event) {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String roleStr = cmbRole.getSelectionModel().getSelectedItem();

        if (username.isEmpty()) {
            showError("Please enter your hospital staff username.");
            return;
        }

        Role role = switch (roleStr != null ? roleStr : "Admin") {
            case "Receptionist" -> Role.RECEPTIONIST;
            case "Doctor" -> Role.DOCTOR;
            case "Charge nurse" -> Role.CHARGE_NURSE;
            case "Triage nurse" -> Role.TRIAGE_NURSE;
            case "Admin" -> Role.ADMIN;
            default -> Role.ADMIN;
        };

        String fullName = switch (role) {
            case RECEPTIONIST -> "Ravi V.";
            case DOCTOR -> "Dr. Arjun Mehta";
            case CHARGE_NURSE -> "Anita N.";
            case ADMIN -> "Administrator";
            case TRIAGE_NURSE -> "Priya S.";
        };

        User user = new User(1, username, "", fullName, role, LocalDateTime.now());
        UserSession.getInstance().setLoggedInUser(user);

        // Audit log login action (Page 18)
        AuditService.getInstance().log("USER_LOGIN", null, null, null, username,
                fullName + " logged in with role " + role.getTitle());

        // Navigate to Main application window
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);
            } else {
                stage.setScene(new Scene(root, 1360, 780));
            }
            stage.setTitle("HEPS – City General Hospital Emergency Department");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to launch workstation layout: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
