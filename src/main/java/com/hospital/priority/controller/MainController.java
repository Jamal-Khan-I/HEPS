package com.hospital.priority.controller;

import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.service.AlertService;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================================
 * MAIN WORKSTATION CONTROLLER (Page 2)
 * ============================================================================
 * Coordinates the Top Bar, Live Clock, Alerts Badge, Global Search, and Role-Filtered Sidebar.
 */
public class MainController implements Initializable {

    private static MainController instance;

    @FXML private VBox sidebar;
    @FXML private Label lblScreenTitle;
    @FXML private Label lblClock;
    @FXML private Label lblAlertBadge;
    @FXML private TextField txtGlobalSearch;
    @FXML private StackPane contentArea;

    // Sidebar Groups for Role Filtering (Page 18)
    @FXML private VBox grpOperations;
    @FXML private VBox grpClinical;
    @FXML private VBox grpRecords;
    @FXML private VBox grpInsights;
    @FXML private VBox grpAdmin;

    // Nav Buttons
    @FXML private Button btnNavCommandCenter;
    @FXML private Button btnNavRegister;
    @FXML private Button btnNavQueue;
    @FXML private Button btnNavBayBoard;
    @FXML private Button btnNavMassCasualty;
    @FXML private Button btnNavDoctorWorkspace;
    @FXML private Button btnNavOrdersResults;
    @FXML private Button btnNavRecords;
    @FXML private Button btnNavDischarge;
    @FXML private Button btnNavReports;
    @FXML private Button btnNavAlerts;
    @FXML private Button btnNavAdmin;

    // User Profile at bottom of sidebar
    @FXML private Label lblInitials;
    @FXML private Label lblStaffName;
    @FXML private Label lblStaffRole;

    private final List<Button> allNavButtons = new ArrayList<>();
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        // Register nav buttons
        allNavButtons.addAll(Arrays.asList(
                btnNavCommandCenter, btnNavRegister, btnNavQueue, btnNavBayBoard, btnNavMassCasualty,
                btnNavDoctorWorkspace, btnNavOrdersResults, btnNavRecords, btnNavDischarge,
                btnNavReports, btnNavAlerts, btnNavAdmin
        ));

        // Start live clock
        startLiveClock();

        // Apply role permissions to sidebar items
        applyRolePermissions();

        // Update active profile pill
        updateUserProfile();

        // Update alerts badge
        updateAlertBadge();

        // Show Command Center by default (Page 2)
        showCommandCenter(null);
    }

    private void startLiveClock() {
        lblClock.setText(LocalDateTime.now().format(CLOCK_FMT));
        Timeline clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblClock.setText(LocalDateTime.now().format(CLOCK_FMT));
            updateAlertBadge();
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    public void updateAlertBadge() {
        int unread = AlertService.getInstance().getUnreadCount();
        if (lblAlertBadge != null) {
            lblAlertBadge.setText(String.valueOf(unread));
            lblAlertBadge.setVisible(unread > 0);
        }
    }

    public void updateSidebarCounters() {
        updateAlertBadge();
    }

    public void updateUserProfile() {
        UserSession session = UserSession.getInstance();
        if (lblStaffName != null) lblStaffName.setText(session.getStaffName());
        if (lblStaffRole != null) lblStaffRole.setText(session.getRole().getTitle());
        if (lblInitials != null) lblInitials.setText(session.getStaffInitials());
    }

    /**
     * Enforces the Page 18 Permission Matrix.
     */
    public void applyRolePermissions() {
        UserSession session = UserSession.getInstance();

        setNavVisible(btnNavRegister, session.canAccessRegisterPatient());
        setNavVisible(btnNavQueue, session.canAccessEmergencyQueue());
        setNavVisible(btnNavBayBoard, session.canAccessBayBoard());
        setNavVisible(btnNavMassCasualty, session.canAccessMassCasualty());
        setNavVisible(btnNavDoctorWorkspace, session.canAccessDoctorWorkspace());
        setNavVisible(btnNavOrdersResults, session.canAccessOrdersResults());
        setNavVisible(btnNavDischarge, session.canAccessDisposition());
        setNavVisible(btnNavReports, session.canAccessReports());
        setNavVisible(btnNavAlerts, session.canAccessAlerts());
        setNavVisible(btnNavAdmin, session.canAccessAdmin());

        // Hide entire group boxes if all children are hidden
        if (grpAdmin != null) {
            grpAdmin.setVisible(session.canAccessAdmin());
            grpAdmin.setManaged(session.canAccessAdmin());
        }
        if (grpInsights != null) {
            boolean visible = session.canAccessReports() || session.canAccessAlerts();
            grpInsights.setVisible(visible);
            grpInsights.setManaged(visible);
        }
        if (grpClinical != null) {
            boolean visible = session.canAccessDoctorWorkspace() || session.canAccessOrdersResults();
            grpClinical.setVisible(visible);
            grpClinical.setManaged(visible);
        }
    }

    private void setNavVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    // =========================================================================
    // SCREEN NAVIGATION ACTIONS
    // =========================================================================

    @FXML
    public void showCommandCenter(ActionEvent event) {
        lblScreenTitle.setText("Command center");
        setActiveNav(btnNavCommandCenter);
        loadView("/fxml/dashboard.fxml");
    }

    // Backward compatibility aliases
    public void showOverview(ActionEvent event) {
        showCommandCenter(event);
    }
    public void showDashboard(ActionEvent event) {
        showCommandCenter(event);
    }

    @FXML
    public void showRegisterPatient(ActionEvent event) {
        lblScreenTitle.setText("Register patient");
        setActiveNav(btnNavRegister);
        loadView("/fxml/register_patient.fxml");
    }

    @FXML
    public void showEmergencyQueue(ActionEvent event) {
        lblScreenTitle.setText("Emergency queue");
        setActiveNav(btnNavQueue);
        loadView("/fxml/emergency_queue.fxml");
    }

    @FXML
    public void showBayBoard(ActionEvent event) {
        lblScreenTitle.setText("Bay board");
        setActiveNav(btnNavBayBoard);
        loadView("/fxml/bay_board.fxml");
    }

    @FXML
    public void showMassCasualty(ActionEvent event) {
        lblScreenTitle.setText("Mass casualty");
        setActiveNav(btnNavMassCasualty);
        loadView("/fxml/mass_casualty.fxml");
    }

    @FXML
    public void showDoctorWorkspace(ActionEvent event) {
        lblScreenTitle.setText("Doctor workspace");
        setActiveNav(btnNavDoctorWorkspace);
        loadView("/fxml/doctor_workspace.fxml");
    }

    @FXML
    public void showOrdersResults(ActionEvent event) {
        lblScreenTitle.setText("Orders and results");
        setActiveNav(btnNavOrdersResults);
        loadView("/fxml/orders_results.fxml");
    }

    @FXML
    public void showPatientRecords(ActionEvent event) {
        lblScreenTitle.setText("Patient records");
        setActiveNav(btnNavRecords);
        loadView("/fxml/patient_history.fxml");
    }

    public void showPatientHistory(ActionEvent event) {
        showPatientRecords(event);
    }

    @FXML
    public void showDischarge(ActionEvent event) {
        lblScreenTitle.setText("Disposition & discharge");
        setActiveNav(btnNavDischarge);
        loadView("/fxml/disposition.fxml");
    }

    @FXML
    public void showReports(ActionEvent event) {
        lblScreenTitle.setText("Reports and analytics");
        setActiveNav(btnNavReports);
        loadView("/fxml/reports.fxml");
    }

    @FXML
    public void showAlerts(ActionEvent event) {
        lblScreenTitle.setText("Alerts center");
        setActiveNav(btnNavAlerts);
        loadView("/fxml/alerts.fxml");
    }

    @FXML
    public void showAdmin(ActionEvent event) {
        lblScreenTitle.setText("Admin");
        setActiveNav(btnNavAdmin);
        loadView("/fxml/admin.fxml");
    }

    @FXML
    public void handleGlobalSearch(ActionEvent event) {
        String query = txtGlobalSearch.getText() != null ? txtGlobalSearch.getText().trim() : "";
        if (!query.isEmpty()) {
            showPatientRecords(null);
            // Patient records view will filter automatically
        }
    }

    private void setActiveNav(Button active) {
        for (Button b : allNavButtons) {
            if (b != null) {
                b.getStyleClass().remove("nav-item-active");
            }
        }
        if (active != null && !active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setCharset(java.nio.charset.StandardCharsets.UTF_8);
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (Throwable e) {
            e.printStackTrace();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            Label placeholder = new Label("Screen loading error: " + fxmlPath + "\n\n" + e.getMessage() + "\n\nRoot Cause: " + cause.toString());
            placeholder.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626; -fx-padding: 24px;");
            contentArea.getChildren().setAll(placeholder);
        }
    }

    @FXML
    public void handleSignOut(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign out of City General Hospital Emergency Workstation?");
        confirm.setContentText("Your active session will be ended.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            User currentUser = UserSession.getInstance().getCurrentUser();
            String name = currentUser != null ? currentUser.getFullName() : "Staff";
            AuditService.getInstance().log("USER_LOGOUT", null, null, null, null, name + " signed out.");

            UserSession.getInstance().logout();

            // Return to login screen
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) sidebar.getScene().getWindow();
                Scene currentScene = stage.getScene();
                if (currentScene != null) {
                    currentScene.setRoot(root);
                } else {
                    stage.setScene(new Scene(root, 1280, 760));
                }
                stage.setTitle("HEPS – City General Hospital Emergency Department");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
