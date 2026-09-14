package com.hospital.priority.controller;

import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.service.AuthService;
import com.hospital.priority.service.QueueService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static MainController instance;

    @FXML private Label clockLabel;
    @FXML private Label userBadgeLabel;
    @FXML private Button btnSwitchUser;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavRegister;
    @FXML private Button btnNavQueue;
    @FXML private Button btnNavHistory;

    @FXML private Label sidebarWaitingCount;
    @FXML private Label sidebarCriticalCount;

    @FXML private StackPane contentArea;

    private final QueueService queueService = QueueService.getInstance();
    private final AuthService authService = AuthService.getInstance();

    private final DateTimeFormatter clockFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        initClock();
        updateUserBadge();
        updateSidebarCounters();
        showDashboard(null);
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            clockLabel.setText(LocalDateTime.now().format(clockFormatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void updateUserBadge() {
        User user = authService.getCurrentUser();
        if (user != null) {
            userBadgeLabel.setText("Logged in as: " + user.getFullName());
        } else {
            userBadgeLabel.setText("Logged in as: Guest (Demo)");
        }
    }

    public void updateSidebarCounters() {
        Platform.runLater(() -> {
            sidebarWaitingCount.setText(String.valueOf(queueService.getQueueSize()));
            sidebarCriticalCount.setText(String.valueOf(queueService.getCriticalCount()));
        });
    }

    @FXML
    public void showDashboard(ActionEvent event) {
        setActiveNavButton(btnNavDashboard);
        loadView("/fxml/dashboard.fxml");
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
    public void showPatientHistory(ActionEvent event) {
        setActiveNavButton(btnNavHistory);
        loadView("/fxml/patient_history.fxml");
    }

    private void setActiveNavButton(Button activeButton) {
        List<Button> navButtons = Arrays.asList(btnNavDashboard, btnNavRegister, btnNavQueue, btnNavHistory);
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("nav-button-active");
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
            updateSidebarCounters();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error Loading Screen", "Failed to load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    @FXML
    private void handleSwitchUser(ActionEvent event) {
        List<String> roles = Arrays.asList(
                "Admin (Demo)",
                "Receptionist (Demo)",
                "Doctor (Demo)"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(roles.get(0), roles);
        dialog.setTitle("Switch Staff Account");
        dialog.setHeaderText("Hospital Emergency Department - Staff Switch");
        dialog.setContentText("Select active account role:");

        dialog.showAndWait().ifPresent(selected -> {
            if (selected.contains("Admin")) {
                authService.setCurrentUser(new User(1, "admin", "", "Admin (Demo)", Role.ADMIN, null));
            } else if (selected.contains("Receptionist")) {
                authService.setCurrentUser(new User(2, "receptionist", "", "Receptionist (Demo)", Role.RECEPTIONIST, null));
            } else {
                authService.setCurrentUser(new User(3, "doctor", "", "Doctor (Demo)", Role.DOCTOR, null));
            }
            updateUserBadge();
            showInformationAlert("User Switched", "Active session: " + authService.getCurrentUser().getFullName());
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
