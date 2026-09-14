package com.hospital.priority.controller;

import com.hospital.priority.model.User;
import com.hospital.priority.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static MainController instance;

    @FXML private Label userBadgeLabel;
    @FXML private Button btnSignOut;

    @FXML private Button btnNavOverview;
    @FXML private Button btnNavRegister;
    @FXML private Button btnNavQueue;
    @FXML private Button btnNavRecords;

    @FXML private StackPane contentArea;

    private final AuthService authService = AuthService.getInstance();

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        updateUserBadge();
        showOverview(null);
    }

    public void updateUserBadge() {
        User user = authService.getCurrentUser();
        if (user != null) {
            userBadgeLabel.setText(user.getFullName());
        } else {
            userBadgeLabel.setText("Administrator");
        }
    }

    public void updateSidebarCounters() {
        // Safe hook retained for multi-controller notifications
    }

    @FXML
    public void showOverview(ActionEvent event) {
        setActiveNavButton(btnNavOverview);
        loadView("/fxml/dashboard.fxml");
    }

    // Backward compatibility alias for existing callers
    public void showDashboard(ActionEvent event) {
        showOverview(event);
    }

    @FXML
    public void showRegisterPatient(ActionEvent event) {
        setActiveNavButton(btnNavRegister);
        loadView("/fxml/register_patient.fxml");
    }

    @FXML
    public void showEmergencyQueue(ActionEvent event) {
        setActiveNavButton(btnNavQueue);
        loadView("/fxml/emergency_queue.fxml");
    }

    @FXML
    public void showPatientRecords(ActionEvent event) {
        setActiveNavButton(btnNavRecords);
        loadView("/fxml/patient_history.fxml");
    }

    // Backward compatibility alias
    public void showPatientHistory(ActionEvent event) {
        showPatientRecords(event);
    }

    private void setActiveNavButton(Button activeButton) {
        List<Button> navButtons = Arrays.asList(btnNavOverview, btnNavRegister, btnNavQueue, btnNavRecords);
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error Loading Screen", "Failed to load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    @FXML
    private void handleSignOut(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of Emergency Department Workstation?");
        confirm.setContentText("Your active session will be ended.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            authService.logout();
            updateUserBadge();
            showOverview(null);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
