package com.hospital.priority.controller;

import com.hospital.priority.model.Bay;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.BayService;
import com.hospital.priority.service.QueueService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * BAY BOARD CONTROLLER (Page 9)
 * ============================================================================
 * Coordinates the 12 ED bays visual grid, real-time occupancy indicators,
 * sanitization timers, patient allocation, transfer, and release workflows.
 */
public class BayBoardController implements Initializable {

    @FXML private Label lblTotalBays;
    @FXML private Label lblOccupiedBays;
    @FXML private Label lblFreeBays;
    @FXML private Label lblCleaningBays;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterResus;
    @FXML private Button btnFilterAcute;
    @FXML private Button btnFilterObs;
    @FXML private Button btnFilterFast;
    @FXML private TextField txtBaySearch;

    @FXML private FlowPane bayGrid;

    private final BayService bayService = BayService.getInstance();
    private final QueueService queueService = QueueService.getInstance();

    private String activeZoneFilter = "ALL";
    private Timeline refreshTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchListener();
        renderBayBoard();
        startAutoRefresh();
    }

    private void setupSearchListener() {
        txtBaySearch.textProperty().addListener((obs, oldVal, newVal) -> renderBayBoard());
    }

    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> renderBayBoard()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        renderBayBoard();
    }

    @FXML
    public void filterAll(ActionEvent event) {
        setActiveFilter(btnFilterAll, "ALL");
    }

    @FXML
    public void filterResus(ActionEvent event) {
        setActiveFilter(btnFilterResus, "RESUS");
    }

    @FXML
    public void filterAcute(ActionEvent event) {
        setActiveFilter(btnFilterAcute, "ACUTE");
    }

    @FXML
    public void filterObs(ActionEvent event) {
        setActiveFilter(btnFilterObs, "OBS");
    }

    @FXML
    public void filterFast(ActionEvent event) {
        setActiveFilter(btnFilterFast, "FAST");
    }

    private void setActiveFilter(Button activeBtn, String zone) {
        btnFilterAll.getStyleClass().setAll("filter-tab-pill");
        btnFilterResus.getStyleClass().setAll("filter-tab-pill");
        btnFilterAcute.getStyleClass().setAll("filter-tab-pill");
        btnFilterObs.getStyleClass().setAll("filter-tab-pill");
        btnFilterFast.getStyleClass().setAll("filter-tab-pill");

        activeBtn.getStyleClass().setAll("filter-tab-pill-active");
        this.activeZoneFilter = zone;
        renderBayBoard();
    }

    public void renderBayBoard() {
        List<Bay> allBays = bayService.getAllBays();

        // Update counts
        long total = allBays.size();
        long occupied = allBays.stream().filter(b -> b.getStatus() == Bay.BayStatus.OCCUPIED).count();
        long cleaning = allBays.stream().filter(b -> b.getStatus() == Bay.BayStatus.CLEANING).count();
        long free = allBays.stream().filter(Bay::isAvailable).count();

        lblTotalBays.setText(total + " Total Bays");
        lblOccupiedBays.setText(occupied + " Occupied");
        lblFreeBays.setText(free + " Free");
        lblCleaningBays.setText(cleaning + " Cleaning");

        // Filter by zone
        List<Bay> filtered = allBays.stream().filter(b -> {
            if ("RESUS".equals(activeZoneFilter)) {
                return b.getType() == Bay.BayType.RESUS || b.getType() == Bay.BayType.TRAUMA;
            } else if ("ACUTE".equals(activeZoneFilter)) {
                return b.getType() == Bay.BayType.ACUTE;
            } else if ("OBS".equals(activeZoneFilter)) {
                return b.getType() == Bay.BayType.OBSERVATION;
            } else if ("FAST".equals(activeZoneFilter)) {
                return b.getType() == Bay.BayType.FAST_TRACK || b.getType() == Bay.BayType.AMBULATORY;
            }
            return true;
        }).collect(Collectors.toList());

        // Search text filter
        String query = txtBaySearch.getText() != null ? txtBaySearch.getText().trim().toLowerCase() : "";
        if (!query.isEmpty()) {
            filtered = filtered.stream().filter(b ->
                    b.getName().toLowerCase().contains(query) ||
                    (b.getCurrentPatientName() != null && b.getCurrentPatientName().toLowerCase().contains(query)) ||
                    (b.getCurrentPatientToken() != null && b.getCurrentPatientToken().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }

        bayGrid.getChildren().clear();
        for (Bay bay : filtered) {
            bayGrid.getChildren().add(createBayCard(bay));
        }
    }

    private VBox createBayCard(Bay bay) {
        VBox card = new VBox(10);
        card.setPrefWidth(295);
        card.setMinWidth(280);
        card.setMaxWidth(340);
        card.setStyle("-fx-background-color: #FFFFFF; " +
                      "-fx-background-radius: 12px; " +
                      "-fx-border-color: #E2E8F0; " +
                      "-fx-border-width: 1px; " +
                      "-fx-border-radius: 12px; " +
                      "-fx-padding: 14px 16px; " +
                      "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.04), 8, 0, 0, 2);");

        // Top accent line based on status / priority
        Region accentBar = new Region();
        accentBar.setPrefHeight(4);
        accentBar.setMaxHeight(4);
        String accentColor = "#CBD5E1";
        if (bay.getStatus() == Bay.BayStatus.FREE) {
            accentColor = "#10B981"; // Emerald
        } else if (bay.getStatus() == Bay.BayStatus.CLEANING) {
            accentColor = "#F59E0B"; // Amber
        } else if (bay.getStatus() == Bay.BayStatus.OCCUPIED) {
            if (bay.getCurrentPriority() == EmergencyLevel.CRITICAL) accentColor = "#DC2626";
            else if (bay.getCurrentPriority() == EmergencyLevel.HIGH) accentColor = "#EA580C";
            else if (bay.getCurrentPriority() == EmergencyLevel.MEDIUM) accentColor = "#D97706";
            else accentColor = "#16A34A";
        }
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 4px;");

        // Header: Bay Name + Status Pill
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblName = new Label(bay.getName());
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
        Label lblZone = new Label(bay.getType().getDisplayName());
        lblZone.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: 600;");
        titleBox.getChildren().addAll(lblName, lblZone);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusPill = new Label(bay.getStatus().name());
        statusPill.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-padding: 3px 8px; -fx-background-radius: 999px; "
                + bay.getStatus().getStyle());

        header.getChildren().addAll(titleBox, spacer, statusPill);

        // Body content
        VBox body = new VBox(8);
        body.setPadding(new Insets(4, 0, 4, 0));

        if (bay.getStatus() == Bay.BayStatus.OCCUPIED) {
            // Patient details
            HBox patientRow = new HBox(10);
            patientRow.setAlignment(Pos.CENTER_LEFT);

            StackPane avatar = new StackPane();
            avatar.setPrefSize(34, 34);
            String initials = getInitials(bay.getCurrentPatientName());
            Label lblInit = new Label(initials);
            lblInit.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #0D9488;");
            avatar.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 999px; -fx-border-color: #99F6E4; -fx-border-radius: 999px;");
            avatar.getChildren().add(lblInit);

            VBox patInfo = new VBox(2);
            Label lblPatName = new Label(bay.getCurrentPatientName() != null ? bay.getCurrentPatientName() : "Unknown");
            lblPatName.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0F172A;");
            Label lblToken = new Label(bay.getCurrentPatientToken() != null ? bay.getCurrentPatientToken() : "--");
            lblToken.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-family: monospace;");
            patInfo.getChildren().addAll(lblPatName, lblToken);

            Region patSpacer = new Region();
            HBox.setHgrow(patSpacer, Priority.ALWAYS);

            if (bay.getCurrentPriority() != null) {
                Label prioBadge = new Label(bay.getCurrentPriority().getDisplayName());
                prioBadge.getStyleClass().addAll("priority-badge", bay.getCurrentPriority().getCssClass());
                patientRow.getChildren().addAll(avatar, patInfo, patSpacer, prioBadge);
            } else {
                patientRow.getChildren().addAll(avatar, patInfo);
            }

            // Doctor and duration
            HBox metaRow = new HBox(12);
            metaRow.setAlignment(Pos.CENTER_LEFT);
            metaRow.setStyle("-fx-padding: 4px 0 0 0;");

            String doc = bay.getAssignedDoctorName() != null ? bay.getAssignedDoctorName() : "Doctor unassigned";
            Label lblDoc = new Label("👨‍⚕️ " + doc);
            lblDoc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569;");

            Region metaSpacer = new Region();
            HBox.setHgrow(metaSpacer, Priority.ALWAYS);

            long minutesInBay = bay.getMinutesOccupied();
            Label lblTime = new Label("⏱️ " + minutesInBay + "m in bay");
            lblTime.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #64748B;");

            metaRow.getChildren().addAll(lblDoc, metaSpacer, lblTime);
            body.getChildren().addAll(patientRow, metaRow);

        } else if (bay.getStatus() == Bay.BayStatus.CLEANING) {
            VBox cleaningBox = new VBox(4);
            cleaningBox.setAlignment(Pos.CENTER_LEFT);
            Label lblCleanTitle = new Label("Sanitization & cleaning in progress");
            lblCleanTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #A16207;");
            long rem = bay.getRemainingCleaningMinutes();
            Label lblCleanTime = new Label("Ready in ~" + rem + " minutes (auto-clears to Free)");
            lblCleanTime.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
            cleaningBox.getChildren().addAll(lblCleanTitle, lblCleanTime);
            body.getChildren().add(cleaningBox);

        } else {
            // FREE
            VBox freeBox = new VBox(4);
            freeBox.setAlignment(Pos.CENTER_LEFT);
            Label lblFree = new Label("Ready for immediate clinical admission");
            lblFree.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #15803D;");
            Label lblAcuity = new Label("Ideal for: " + bay.getType().getRecommendedLevel().getDisplayName() + " acuity");
            lblAcuity.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            freeBox.getChildren().addAll(lblFree, lblAcuity);
            body.getChildren().add(freeBox);
        }

        // Action Buttons Row
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 4px 0 0 0; -fx-border-color: #F1F5F9; -fx-border-width: 1px 0 0 0;");

        if (bay.getStatus() == Bay.BayStatus.OCCUPIED) {
            Button btnTransfer = new Button("Transfer");
            btnTransfer.getStyleClass().add("btn-modern-pill-secondary");
            btnTransfer.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
            btnTransfer.setOnAction(e -> handleTransferBay(bay));

            Button btnRelease = new Button("Release / Clean");
            btnRelease.getStyleClass().add("btn-modern-pill-secondary");
            btnRelease.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-text-fill: #DC2626;");
            btnRelease.setOnAction(e -> handleReleaseBay(bay));

            actions.getChildren().addAll(btnTransfer, btnRelease);

        } else if (bay.getStatus() == Bay.BayStatus.CLEANING) {
            Button btnForceClean = new Button("Mark Cleaned Now");
            btnForceClean.getStyleClass().add("btn-modern-pill-secondary");
            btnForceClean.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-text-fill: #15803D;");
            btnForceClean.setOnAction(e -> {
                bay.markCleaned();
                renderBayBoard();
            });
            actions.getChildren().add(btnForceClean);

        } else {
            // FREE
            Button btnAssign = new Button("Assign Waiting Patient →");
            btnAssign.getStyleClass().add("btn-modern-pill-primary");
            btnAssign.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
            btnAssign.setOnAction(e -> handleAssignPatient(bay));
            actions.getChildren().add(btnAssign);
        }

        card.getChildren().addAll(accentBar, header, body, actions);
        return card;
    }

    private void handleAssignPatient(Bay bay) {
        List<Patient> waiting = queueService.getSortedSnapshot();
        if (waiting.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Queue Empty");
            alert.setHeaderText("No Waiting Patients");
            alert.setContentText("There are currently no patients waiting in the emergency queue to assign.");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<Patient> dialog = new ChoiceDialog<>(waiting.get(0), waiting);
        dialog.setTitle("Assign Patient to " + bay.getName());
        dialog.setHeaderText("Select a waiting patient from the emergency queue:");
        dialog.setContentText("Patient:");

        Optional<Patient> result = dialog.showAndWait();
        result.ifPresent(patient -> {
            bay.occupy(patient);
            patient.setAssignedBay(bay.getName());
            patient.setStatus(PatientStatus.ASSIGNED);
            AuditService.getInstance().log("BAY_ASSIGNED", patient.getTokenNo(), patient.getUhid(),
                    "FREE", "OCCUPIED", "Patient assigned to " + bay.getName());
            renderBayBoard();
        });
    }

    private void handleTransferBay(Bay sourceBay) {
        List<Bay> availableBays = bayService.getAvailableBays();
        if (availableBays.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Free Bays");
            alert.setHeaderText("All Emergency Bays are Currently Occupied or Cleaning");
            alert.setContentText("Cannot perform transfer until another bay is released.");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<Bay> dialog = new ChoiceDialog<>(availableBays.get(0), availableBays);
        dialog.setTitle("Transfer Patient from " + sourceBay.getName());
        dialog.setHeaderText("Transfer [" + sourceBay.getCurrentPatientToken() + "] " + sourceBay.getCurrentPatientName() + " to:");
        dialog.setContentText("Target Bay:");

        Optional<Bay> result = dialog.showAndWait();
        result.ifPresent(targetBay -> {
            bayService.transferPatient(sourceBay.getBayId(), targetBay.getBayId());
            AuditService.getInstance().log("BAY_TRANSFER", sourceBay.getCurrentPatientToken(), null,
                    sourceBay.getName(), targetBay.getName(), "Patient transferred between emergency bays");
            renderBayBoard();
        });
    }

    private void handleReleaseBay(Bay bay) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Release Bay");
        confirm.setHeaderText("Release " + bay.getName() + " and begin 15-minute sanitization?");
        confirm.setContentText("Patient [" + bay.getCurrentPatientToken() + "] " + bay.getCurrentPatientName()
                + " will be cleared from this bay.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String token = bay.getCurrentPatientToken();
            bay.releaseAndClean(15);
            AuditService.getInstance().log("BAY_RELEASED", token, null,
                    "OCCUPIED", "CLEANING", bay.getName() + " released to cleaning");
            renderBayBoard();
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
