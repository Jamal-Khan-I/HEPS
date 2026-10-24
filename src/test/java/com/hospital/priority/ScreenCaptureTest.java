package com.hospital.priority;

import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.DBConnection;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScreenCaptureTest {

    private static final String ARTIFACTS_DIR = "C:\\Users\\Jamal Khan\\.gemini\\antigravity-ide\\brain\\4058423c-ebee-416c-9859-8057a2fa01a3";

    @Test
    @DisplayName("Render and capture screenshots of all four clinical screens")
    public void captureScreenshots() throws Exception {
        if (!DBConnection.testConnection()) {
            System.out.println("Notice: Database not connected, skipping screen capture.");
            return;
        }

        QueueService.getInstance().initializeQueueFromDatabase();

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    captureView("/fxml/login.fxml", "screen_login.png");
                    captureView("/fxml/main_layout.fxml", "screen_full_app.png");
                    captureView("/fxml/dashboard.fxml", "screen_overview.png");
                    captureView("/fxml/register_patient.fxml", "screen_register_patient.png");
                    captureView("/fxml/emergency_queue.fxml", "screen_emergency_queue.png");
                    captureView("/fxml/bay_board.fxml", "screen_bay_board.png");
                    captureView("/fxml/doctor_workspace.fxml", "screen_doctor_workspace.png");
                    captureView("/fxml/orders_results.fxml", "screen_orders_results.png");
                    captureView("/fxml/disposition.fxml", "screen_disposition.png");
                    captureView("/fxml/patient_history.fxml", "screen_patient_records.png");
                    captureView("/fxml/alerts.fxml", "screen_alerts.png");
                    captureView("/fxml/reports.fxml", "screen_reports.png");
                    captureView("/fxml/admin.fxml", "screen_admin.png");
                    captureView("/fxml/mass_casualty.fxml", "screen_mass_casualty.png");
                    captureTicketCard("screen_triage_ticket.png");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException e) {
            // Toolkit already started
            Platform.runLater(() -> {
                try {
                    captureView("/fxml/login.fxml", "screen_login.png");
                    captureView("/fxml/main_layout.fxml", "screen_full_app.png");
                    captureView("/fxml/dashboard.fxml", "screen_overview.png");
                    captureView("/fxml/register_patient.fxml", "screen_register_patient.png");
                    captureView("/fxml/emergency_queue.fxml", "screen_emergency_queue.png");
                    captureView("/fxml/bay_board.fxml", "screen_bay_board.png");
                    captureView("/fxml/doctor_workspace.fxml", "screen_doctor_workspace.png");
                    captureView("/fxml/orders_results.fxml", "screen_orders_results.png");
                    captureView("/fxml/disposition.fxml", "screen_disposition.png");
                    captureView("/fxml/patient_history.fxml", "screen_patient_records.png");
                    captureView("/fxml/alerts.fxml", "screen_alerts.png");
                    captureView("/fxml/reports.fxml", "screen_reports.png");
                    captureView("/fxml/admin.fxml", "screen_admin.png");
                    captureView("/fxml/mass_casualty.fxml", "screen_mass_casualty.png");
                    captureTicketCard("screen_triage_ticket.png");
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
    }

    private void captureView(String fxmlPath, String outputFilename) throws Exception {
        if (fxmlPath.contains("main_layout")) {
            com.hospital.priority.model.User doctorUser = new com.hospital.priority.model.User(2, "dr.mehta", "", "Dr. Arjun Mehta", com.hospital.priority.model.Role.DOCTOR, java.time.LocalDateTime.now());
            com.hospital.priority.service.UserSession.getInstance().setLoggedInUser(doctorUser);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        int width = fxmlPath.contains("main_layout") ? 1280 : (fxmlPath.contains("login") ? 1280 : 1060);
        int height = fxmlPath.contains("main_layout") ? 820 : (fxmlPath.contains("login") ? 760 : 780);

        if (fxmlPath.contains("main_layout")) {
            com.hospital.priority.controller.MainController mc = loader.getController();
            if (mc != null) {
                mc.updateUserProfile();
                mc.applyRolePermissions();
                mc.showDoctorWorkspace(new javafx.event.ActionEvent());
            }
        }

        if (fxmlPath.contains("emergency_queue")) {
            @SuppressWarnings("unchecked")
            javafx.scene.control.TableView<com.hospital.priority.model.Patient> table =
                    (javafx.scene.control.TableView<com.hospital.priority.model.Patient>) root.lookup("#queueTable");
            if (table != null && !table.getItems().isEmpty()) {
                table.getSelectionModel().select(1);
            }
        }

        if (fxmlPath.contains("patient_history")) {
            @SuppressWarnings("unchecked")
            javafx.scene.control.TableView<com.hospital.priority.model.Patient> table =
                    (javafx.scene.control.TableView<com.hospital.priority.model.Patient>) root.lookup("#historyTable");
            if (table != null && !table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
            }
        }

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        root.applyCss();
        root.layout();

        if (fxmlPath.contains("doctor_workspace")) {
            javafx.scene.control.TabPane tabPane = (javafx.scene.control.TabPane) root.lookup("#doctorTabPane");
            if (tabPane != null && tabPane.getTabs().size() >= 4) {
                // Tab 1: SOAP
                tabPane.getSelectionModel().select(1);
                root.applyCss();
                root.layout();
                saveSnapshotToFile(root, "screen_doctor_soap.png");

                // Tab 2: Orders
                tabPane.getSelectionModel().select(2);
                root.applyCss();
                root.layout();
                saveSnapshotToFile(root, "screen_doctor_orders.png");

                // Tab 3: Repeat Vitals
                tabPane.getSelectionModel().select(3);
                root.applyCss();
                root.layout();
                saveSnapshotToFile(root, "screen_doctor_vitals.png");

                // Reset to Tab 0: Overview
                tabPane.getSelectionModel().select(0);
                root.applyCss();
                root.layout();
            }
        }

        saveSnapshotToFile(root, outputFilename);
    }

    private void saveSnapshotToFile(Parent root, String outputFilename) throws Exception {
        WritableImage snapshot = root.snapshot(new SnapshotParameters(), null);
        int w = (int) snapshot.getWidth();
        int h = (int) snapshot.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = snapshot.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }

        File outFile = new File(ARTIFACTS_DIR, outputFilename);
        ImageIO.write(bi, "png", outFile);
        System.out.println(">>> Saved screenshot to: " + outFile.getAbsolutePath());
    }

    private void captureTicketCard(String outputFilename) throws Exception {
        com.hospital.priority.model.Patient demoPatient = new com.hospital.priority.model.Patient(
                "TK-2026-0008",
                "Arvind Swaminathan",
                58,
                "Male",
                "+91 98765 43210",
                "Crushing retrosternal chest pain, radiating to left arm, acute diaphoresis",
                com.hospital.priority.model.EmergencyLevel.CRITICAL,
                java.time.LocalDateTime.now().minusMinutes(7)
        );
        demoPatient.setAssignedBay("Bay 1 [Resuscitation - Red]");
        demoPatient.setAssignedSpecialty(com.hospital.priority.model.Specialty.CARDIOLOGY);
        demoPatient.setAssignedDoctorName("Dr. Rajesh Sharma");
        com.hospital.priority.model.Vitals v = new com.hospital.priority.model.Vitals();
        v.setSpo2(89);
        v.setHeartRate(124);
        v.setTemperature(99.2);
        v.setSystolicBp(185);
        v.setDiastolicBp(110);
        v.setRespiratoryRate(26);
        demoPatient.setVitals(v);

        javafx.scene.layout.VBox ticketCard = com.hospital.priority.util.TriageTicketUtil.createTicketCard(demoPatient);
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(ticketCard);
        wrapper.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 30px;");
        wrapper.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(wrapper, 500, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        wrapper.applyCss();
        wrapper.layout();

        WritableImage snapshot = wrapper.snapshot(new SnapshotParameters(), null);
        int w = (int) snapshot.getWidth();
        int h = (int) snapshot.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = snapshot.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }

        File outFile = new File(ARTIFACTS_DIR, outputFilename);
        ImageIO.write(bi, "png", outFile);
        System.out.println(">>> Saved screenshot to: " + outFile.getAbsolutePath());
    }
}
