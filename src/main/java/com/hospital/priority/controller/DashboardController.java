package com.hospital.priority.controller;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.model.*;
import com.hospital.priority.service.*;
import com.hospital.priority.util.TriageTicketUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * ED COMMAND CENTER CONTROLLER (Page 2)
 * ============================================================================
 * Live overview of the whole ED:
 * 1. KPI Strip: Waiting, Critical, SLA breaches, Bed use (refreshes every 15s).
 * 2. Pipeline Strip: Workflow stage counts (Registered, Triaged, Waiting, In treatment, Disposition).
 * 3. Next Patient to Treat Card: O(1) Min-heap peek with vital chips and SLA target.
 * 4. Waiting Queue Table: Heap-ordered rows with live wait vs SLA progress bars.
 */
public class DashboardController implements Initializable {

    // KPI Labels
    @FXML private Label lblKpiWaiting;
    @FXML private Label lblKpiWaitingSub;
    @FXML private Label lblKpiCritical;
    @FXML private Label lblKpiCriticalSub;
    @FXML private Label lblKpiBreaches;
    @FXML private Label lblKpiBreachesSub;
    @FXML private Label lblKpiBedUse;
    @FXML private Label lblKpiBedUseSub;

    // Pipeline Strip Labels
    @FXML private Label lblPipeRegistered;
    @FXML private Label lblPipeTriaged;
    @FXML private Label lblPipeWaiting;
    @FXML private Label lblPipeInTreatment;
    @FXML private Label lblPipeDisposition;

    // Next Patient Card
    @FXML private VBox cardNextPatient;
    @FXML private Label lblNextPriorityBadge;
    @FXML private Label lblNextBay;
    @FXML private Label lblNextTarget;
    @FXML private Label lblNextToken;
    @FXML private Label lblNextName;
    @FXML private Label lblNextMeta;
    @FXML private Label chipSpo2;
    @FXML private Label chipHr;
    @FXML private Label chipBp;
    @FXML private Label chipNews2;
    @FXML private Label chipAllergy;
    @FXML private Label lblNextComplaint;

    // Waiting Queue Container
    @FXML private VBox queueRowsContainer;

    private final QueueService queueService = QueueService.getInstance();
    private final BayService bayService = BayService.getInstance();
    private final PatientDao patientDao = new PatientDaoImpl();
    private Patient currentNextPatient;
    private Timeline refreshTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDashboardDataAsync();

        // 15-second background refresh timeline (Page 2 build notes)
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> loadDashboardDataAsync()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    public void loadDashboardDataAsync() {
        Task<Void> task = new Task<>() {
            private int waitingCount;
            private int criticalCount;
            private int breachCount;
            private int occupiedBays;
            private int totalBays;
            private int inTreatmentCount;
            private int dispositionCount;
            private Patient nextPatient;
            private List<Patient> topQueue;

            @Override
            protected Void call() {
                // Background compute
                queueService.checkAndApplyEscalations();

                waitingCount = queueService.getQueueSize();
                criticalCount = queueService.getCriticalCount();

                occupiedBays = bayService.getOccupiedCount();
                totalBays = bayService.getTotalBaysCount();

                topQueue = queueService.getSortedSnapshot();

                breachCount = 0;
                for (Patient p : topQueue) {
                    if (p.isSlaBreached()) breachCount++;
                }

                inTreatmentCount = 0;
                dispositionCount = 0;
                try {
                    for (Patient p : patientDao.findAll()) {
                        if (p.getStatus() == PatientStatus.IN_TREATMENT) inTreatmentCount++;
                        else if (p.getStatus() == PatientStatus.DISCHARGED || p.getStatus() == PatientStatus.COMPLETED) dispositionCount++;
                    }
                } catch (Exception ignored) {}

                nextPatient = queueService.peekNext();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    updateKpis(waitingCount, criticalCount, breachCount, occupiedBays, totalBays);
                    updatePipelineStrip(waitingCount, inTreatmentCount, dispositionCount);
                    updateNextPatientCard(nextPatient);
                    renderWaitingQueueRows(topQueue);
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateKpis(int waiting, int critical, int breaches, int occupiedBays, int totalBays) {
        lblKpiWaiting.setText(String.valueOf(waiting));
        lblKpiWaitingSub.setText(waiting == 0 ? "queue clear" : "in priority queue");

        lblKpiCritical.setText(String.valueOf(critical));
        lblKpiCriticalSub.setText(critical == 0 ? "resus clear" : "in resus bays");

        lblKpiBreaches.setText(String.valueOf(breaches));
        lblKpiBreachesSub.setText(breaches == 0 ? "all within target" : breaches + " breached SLA");

        int pct = (totalBays > 0 && occupiedBays > 0) ? (int) Math.round((double) occupiedBays / totalBays * 100) : 0;
        lblKpiBedUse.setText(occupiedBays + "/" + totalBays);
        lblKpiBedUseSub.setText(pct + "% occupied");
    }

    private void updatePipelineStrip(int waiting, int inTreatment, int disposition) {
        lblPipeRegistered.setText("0 Registered");
        lblPipeTriaged.setText("0 Triaged");
        lblPipeWaiting.setText(waiting + " Waiting");
        lblPipeInTreatment.setText(inTreatment + " In treatment");
        lblPipeDisposition.setText(disposition + " Disposition");
    }

    private void updateNextPatientCard(Patient patient) {
        this.currentNextPatient = patient;

        if (patient == null) {
            lblNextName.setText("No active emergency patients");
            lblNextToken.setText("--");
            lblNextMeta.setText("Emergency queue currently clear");
            lblNextComplaint.setText("Awaiting intake registration.");
            lblNextTarget.setText("Target: None");
            lblNextPriorityBadge.setText("Normal");
            lblNextPriorityBadge.getStyleClass().removeAll("badge-critical", "badge-high", "badge-medium", "badge-low");
            lblNextPriorityBadge.getStyleClass().add("badge-low");
            lblNextBay.setText("No bay assigned");
            chipSpo2.setText("SpO2 --");
            chipHr.setText("HR --");
            chipBp.setText("BP --/--");
            chipNews2.setText("NEWS2 0");
            chipAllergy.setText("Allergies: None");
            cardNextPatient.setStyle("-fx-border-color: #E5EAF1; -fx-border-width: 1px;");
            return;
        }

        lblNextName.setText(patient.getFullName());
        lblNextToken.setText(patient.getTokenNo());
        lblNextMeta.setText(patient.getAge() + " - " + patient.getGender() + " - " + patient.getUhid());
        lblNextComplaint.setText(patient.getSymptoms());
        lblNextBay.setText(patient.getShortBay());

        EmergencyLevel level = patient.getEmergencyLevel();
        lblNextPriorityBadge.setText(level.name());
        lblNextPriorityBadge.getStyleClass().removeAll("badge-critical", "badge-high", "badge-medium", "badge-low");
        lblNextPriorityBadge.getStyleClass().add(level.getCssBadgeClass());

        // Target wait time
        lblNextTarget.setText("Target: " + patient.getSlaTargetText());

        // Left border color follows priority
        String borderCol = level == EmergencyLevel.CRITICAL ? "#B91C1C"
                : level == EmergencyLevel.HIGH ? "#C2410C"
                : level == EmergencyLevel.MEDIUM ? "#A16207" : "#15803D";
        cardNextPatient.setStyle("-fx-border-color: #E5EAF1 #E5EAF1 #E5EAF1 " + borderCol + "; -fx-border-width: 1px 1px 1px 4px;");

        // Vitals Chips
        Vitals v = patient.getVitals();
        if (v != null) {
            chipSpo2.setText("SpO2 " + (v.getSpo2() != null ? v.getSpo2() + "%" : "--"));
            chipHr.setText("HR " + (v.getHeartRate() != null ? v.getHeartRate() : "--"));
            chipBp.setText("BP " + (v.getSystolicBp() != null && v.getDiastolicBp() != null ? v.getSystolicBp() + "/" + v.getDiastolicBp() : "--/--"));
        } else {
            chipSpo2.setText("SpO2 --");
            chipHr.setText("HR --");
            chipBp.setText("BP --/--");
        }
        int news2 = patient.getNews2Score();
        chipNews2.setText("NEWS2 " + news2);

        String allergy = patient.getAllergies();
        if (allergy != null && !allergy.isEmpty()) {
            chipAllergy.setText("Allergy: " + allergy);
            chipAllergy.setVisible(true);
        } else {
            chipAllergy.setText("Allergies: None");
            chipAllergy.setVisible(true);
        }
    }

    private void renderWaitingQueueRows(List<Patient> list) {
        queueRowsContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("No waiting patients in triage queue.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-padding: 16px;");
            queueRowsContainer.getChildren().add(empty);
            return;
        }

        int rank = 1;
        int limit = Math.min(list.size(), 6);
        for (int i = 0; i < limit; i++) {
            Patient p = list.get(i);
            queueRowsContainer.getChildren().add(createQueueRow(rank++, p));
        }
    }

    private GridPane createQueueRow(int rank, Patient p) {
        GridPane row = new GridPane();
        row.setHgap(12.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10px 8px; -fx-background-color: #FFFFFF; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1px 0; -fx-background-radius: 8px;");

        // 1. Rank #
        Label lblRank = new Label(String.valueOf(rank));
        lblRank.setPrefWidth(30.0);
        lblRank.setStyle("-fx-font-weight: 700; -fx-text-fill: #64748B; -fx-font-size: 13px;");
        row.add(lblRank, 0, 0);

        // 2. Patient details
        VBox nameBox = new VBox(2.0);
        nameBox.setPrefWidth(320.0);
        Label lblName = new Label(p.getFullName() + (p.isEscalated() ? " (escalated)" : ""));
        lblName.setStyle("-fx-font-weight: 700; -fx-font-size: 13.5px; -fx-text-fill: " + (p.isEscalated() ? "#C2410C;" : "#0F172A;"));

        String meta = p.getTokenNo() + " · " + p.getAge() + " " + (p.getGender().startsWith("M") ? "M" : "F") + " · " + p.getSymptoms();
        if (meta.length() > 42) meta = meta.substring(0, 40) + "...";
        Label lblSub = new Label(meta);
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        nameBox.getChildren().addAll(lblName, lblSub);
        row.add(nameBox, 1, 0);

        // 3. Priority Pill
        Label pill = new Label(p.getEmergencyLevel().name());
        pill.setPrefWidth(90.0);
        pill.setAlignment(Pos.CENTER);
        pill.getStyleClass().addAll("badge", p.getEmergencyLevel().getCssBadgeClass());
        row.add(pill, 2, 0);

        // 4. Bay · Doctor
        VBox bayDocBox = new VBox(2.0);
        bayDocBox.setPrefWidth(220.0);
        Label lblBay = new Label(p.getShortBay());
        lblBay.setStyle("-fx-font-weight: 600; -fx-font-size: 12.5px; -fx-text-fill: #0F172A;");
        Label lblDoc = new Label(p.getAssignedDoctorName());
        lblDoc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
        bayDocBox.getChildren().addAll(lblBay, lblDoc);
        row.add(bayDocBox, 3, 0);

        // 5. Wait vs SLA (Time text + Colored Progress Bar)
        VBox slaBox = new VBox(4.0);
        slaBox.setPrefWidth(200.0);

        long waitMins = p.getWaitingMinutes();
        String waitText = waitMins + "m / " + p.getSlaTargetText();
        Label lblSla = new Label(waitText);
        lblSla.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + p.getSlaColorHex() + ";");

        ProgressBar bar = new ProgressBar(Math.min(1.0, Math.max(0.1, p.getSlaRatio())));
        bar.setPrefWidth(160.0);
        bar.setPrefHeight(6.0);
        bar.setStyle("-fx-accent: " + p.getSlaColorHex() + ";");

        slaBox.getChildren().addAll(lblSla, bar);
        row.add(slaBox, 4, 0);

        return row;
    }

    @FXML
    public void handlePrintNextSlip(ActionEvent event) {
        if (currentNextPatient != null) {
            TriageTicketUtil.showTicketDialog(currentNextPatient);
            AuditService.getInstance().log("PRINT_TICKET", currentNextPatient.getTokenNo(), currentNextPatient.getUhid(),
                    null, "PRINTED", "Triage slip printed from Command Center");
        }
    }

    @FXML
    public void handleBeginTreatment(ActionEvent event) {
        if (currentNextPatient != null) {
            try {
                Patient served = queueService.serveNext("Dr. Arjun Mehta");
                AuditService.getInstance().log("BEGIN_TREATMENT", served.getTokenNo(), served.getUhid(),
                        "WAITING", "IN_TREATMENT", "Treatment begun by Dr. Arjun Mehta");
                loadDashboardDataAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleViewFullQueue(ActionEvent event) {
        MainController.getInstance().showEmergencyQueue(null);
    }
}
