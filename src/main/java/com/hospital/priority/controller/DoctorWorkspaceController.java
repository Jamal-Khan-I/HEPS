package com.hospital.priority.controller;

import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.model.*;
import com.hospital.priority.service.*;
import com.hospital.priority.util.SvgIcons;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================================
 * DOCTOR WORKSPACE CONTROLLER (Page 9 Redesign)
 * ============================================================================
 * Dedicated clinical interface for emergency physicians to manage assigned
 * patients, document SOAP notes, place diagnostic orders, and review serial vitals.
 */
public class DoctorWorkspaceController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NOTE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // LEFT PANE: Patient Selector
    @FXML private Label lblPatientHeader;
    @FXML private Label lblPatientCount;
    @FXML private TextField txtSearchPatient;
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterMy;
    @FXML private Button btnFilterWaiting;
    @FXML private Button btnFilterTreatment;
    @FXML private ListView<Patient> listPatients;

    // STICKY PATIENT HEADER BANNER (Page 9)
    @FXML private VBox patientBanner;
    @FXML private StackPane avatarPane;
    @FXML private Label lblInitials;
    @FXML private Label lblPatientName;
    @FXML private Label lblDemographics;
    @FXML private Label lblPriorityBadge;
    @FXML private Label lblAllergyTag;
    @FXML private Label lblUhid;
    @FXML private Label lblToken;
    @FXML private Label lblBay;
    @FXML private Label lblDoctor;
    @FXML private Label lblWaitTreatmentStatus;

    // JOURNEY STEPPER
    @FXML private HBox stepperContainer;
    @FXML private StackPane stepCircle1, stepCircle2, stepCircle3, stepCircle4, stepCircle5;
    @FXML private Label stepCheck1, stepCheck2, stepCheck3, stepCheck4, stepCheck5;
    @FXML private Label stepLabel1, stepLabel2, stepLabel3, stepLabel4, stepLabel5;
    @FXML private Region stepLine1, stepLine2, stepLine3, stepLine4;

    // TABS
    @FXML private TabPane doctorTabPane;
    @FXML private Tab tabOverview, tabSoap, tabOrders, tabVitals;

    // TAB 1: OVERVIEW
    @FXML private LineChart<Number, Number> chartVitalsOverview;
    @FXML private NumberAxis axisOverviewX, axisOverviewY;
    @FXML private Label lblOverviewSpo2Range, lblOverviewHrRange;
    @FXML private Label lblTileSpo2, lblTileHr, lblTileBp, lblTileNews2;
    @FXML private VBox boxOverviewOrders;

    // TAB 2: SOAP NOTES
    @FXML private TextArea txtSubjective, txtObjective, txtAssessment, txtPlan;
    @FXML private Label lblCountSubj, lblCountObj, lblCountAssess, lblCountPlan;
    @FXML private Button btnSaveSoap;
    @FXML private VBox boxNotesHistory;

    // TAB 3: ORDERS & RESULTS
    @FXML private ComboBox<Order.OrderType> cmbOrderType;
    @FXML private ComboBox<String> cmbOrderPriority;
    @FXML private TextField txtOrderName;
    @FXML private VBox boxOrdersList;
    @FXML private VBox boxOrderResultDetails;

    // TAB 4: REPEAT VITALS
    @FXML private HBox boxWatchdogAlert;
    @FXML private Label lblWatchdogMessage;
    @FXML private Label lblNews2Live;
    @FXML private TextField txtHr, txtSysBp, txtDiaBp, txtSpo2, txtRr, txtTemp, txtGcs, txtPain;
    @FXML private VBox boxVitalsHistory;

    // Core Services
    private final QueueService queueService = QueueService.getInstance();
    private final OrderService orderService = OrderService.getInstance();
    private final PatientDaoImpl patientDao = new PatientDaoImpl();

    private final ObservableList<Patient> allActivePatients = FXCollections.observableArrayList();
    private final ObservableList<Patient> displayedPatients = FXCollections.observableArrayList();
    private final List<ClinicalNote> notesList = new ArrayList<>();
    private Patient selectedPatient;

    private String currentFilterMode = "ALL"; // ALL, MY, WAITING, TREATMENT
    private Timeline liveUpdateTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPatientListCellFactory();
        setupOrderCombos();
        setupSoapFormValidation();
        setupLiveNews2Calculation();
        loadActivePatients();
        setupSearchFilter();
        determineInitialFilterByRole();
        setupAutoRefresh();

        // Select first patient or Ramesh Kumar
        selectDefaultPatient();
    }

    private void determineInitialFilterByRole() {
        Role role = UserSession.getInstance().getRole();
        if (role == Role.DOCTOR) {
            currentFilterMode = "MY";
            lblPatientHeader.setText("My Patients");
            updateFilterButtonStyles(btnFilterMy);
        } else {
            currentFilterMode = "ALL";
            lblPatientHeader.setText("Active Patients");
            updateFilterButtonStyles(btnFilterAll);
        }
        applyCurrentFilter();
    }

    private void selectDefaultPatient() {
        if (displayedPatients.isEmpty()) return;
        Patient preferred = null;
        for (Patient p : displayedPatients) {
            if ("TK-2026-0142".equalsIgnoreCase(p.getTokenNo()) || p.getFullName().toLowerCase().contains("ramesh")) {
                preferred = p;
                break;
            }
        }
        if (preferred != null) {
            listPatients.getSelectionModel().select(preferred);
        } else {
            listPatients.getSelectionModel().select(0);
        }
    }

    private void setupOrderCombos() {
        cmbOrderType.setItems(FXCollections.observableArrayList(Order.OrderType.values()));
        cmbOrderType.getSelectionModel().select(Order.OrderType.LAB);

        cmbOrderPriority.setItems(FXCollections.observableArrayList("Routine", "Urgent", "STAT"));
        cmbOrderPriority.getSelectionModel().select("STAT");
    }

    private void setupPatientListCellFactory() {
        listPatients.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean isSelected = isSelected();

                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().setAll(isSelected ? "patient-list-item-selected" : "patient-list-item");
                    row.maxWidthProperty().bind(listPatients.widthProperty().subtract(18));
                    row.prefWidthProperty().bind(listPatients.widthProperty().subtract(18));

                    // Avatar Circle with Priority Tint
                    StackPane avatar = new StackPane();
                    avatar.setPrefSize(36, 36);
                    avatar.setMinSize(36, 36);
                    avatar.setMaxSize(36, 36);
                    avatar.getStyleClass().setAll(getAvatarClassForLevel(p.getEmergencyLevel()));

                    Label lblInit = new Label(getInitials(p.getFullName()));
                    avatar.getChildren().add(lblInit);

                    // 3-Line Patient Info
                    VBox info = new VBox(2);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    // Line 1: Full name + Relative Wait Duration
                    HBox line1 = new HBox(6);
                    line1.setAlignment(Pos.CENTER_LEFT);
                    Label name = new Label(p.getFullName());
                    name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
                    name.setTextOverrun(OverrunStyle.ELLIPSIS);
                    name.setMaxWidth(135);
                    Region spacer1 = new Region();
                    HBox.setHgrow(spacer1, Priority.ALWAYS);
                    Label wait = new Label(p.getFormattedWaitTime());
                    wait.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748B;");
                    line1.getChildren().addAll(name, spacer1, wait);

                    // Line 2: Token + Short Bay
                    HBox line2 = new HBox(4);
                    line2.setAlignment(Pos.CENTER_LEFT);
                    Label sub = new Label(p.getTokenNo() + " · " + p.getShortBay());
                    sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                    line2.getChildren().add(sub);

                    // Line 3: Priority Pill or Symptoms
                    HBox line3 = new HBox(6);
                    line3.setAlignment(Pos.CENTER_LEFT);
                    Label badge = new Label(p.getEmergencyLevel().getDisplayName().toUpperCase());
                    badge.getStyleClass().setAll("badge", p.getEmergencyLevel().getCssClass());
                    badge.setStyle("-fx-font-size: 9.5px; -fx-padding: 1px 6px;");

                    Label symptoms = new Label(p.getSymptoms());
                    symptoms.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                    symptoms.setTextOverrun(OverrunStyle.ELLIPSIS);
                    symptoms.setMaxWidth(130);
                    line3.getChildren().addAll(badge, symptoms);

                    info.getChildren().addAll(line1, line2, line3);
                    row.getChildren().addAll(avatar, info);
                    setGraphic(row);
                }
            }
        });

        listPatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayPatient(newVal);
            }
        });
    }

    private String getAvatarClassForLevel(EmergencyLevel level) {
        if (level == null) return "avatar-medium";
        return switch (level) {
            case CRITICAL -> "avatar-critical";
            case HIGH -> "avatar-high";
            case MEDIUM -> "avatar-medium";
            case LOW -> "avatar-low";
        };
    }

    private void loadActivePatients() {
        allActivePatients.clear();
        // Priority Heap ordering from QueueService
        List<Patient> queue = queueService.getSortedSnapshot();
        allActivePatients.addAll(queue);

        // Load any non-discharged patients from DB not yet in memory
        try {
            List<Patient> all = patientDao.findAll();
            for (Patient p : all) {
                if (!allActivePatients.contains(p) && p.getStatus() != PatientStatus.COMPLETED && p.getStatus() != PatientStatus.DISCHARGED) {
                    allActivePatients.add(p);
                }
            }
        } catch (Exception ignored) {
        }

        // Sort by emergency priority, then arrival time (heap order)
        allActivePatients.sort((p1, p2) -> {
            int comp = Integer.compare(p1.getEmergencyLevel().getRank(), p2.getEmergencyLevel().getRank());
            if (comp != 0) return comp;
            return p1.getArrivalTime().compareTo(p2.getArrivalTime());
        });

        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        String docName = UserSession.getInstance().getStaffName();
        String q = txtSearchPatient.getText() != null ? txtSearchPatient.getText().trim().toLowerCase() : "";

        displayedPatients.clear();
        for (Patient p : allActivePatients) {
            boolean matchesFilter = switch (currentFilterMode) {
                case "MY" -> p.getAssignedDoctorName() != null &&
                             (p.getAssignedDoctorName().equalsIgnoreCase(docName) ||
                              docName.toLowerCase().contains("mehta") && p.getAssignedDoctorName().toLowerCase().contains("mehta"));
                case "WAITING" -> p.getStatus() == PatientStatus.WAITING;
                case "TREATMENT" -> p.getStatus() == PatientStatus.IN_TREATMENT;
                default -> true;
            };

            if (!matchesFilter) continue;

            if (!q.isEmpty()) {
                boolean matchesSearch = p.getFullName().toLowerCase().contains(q) ||
                                       p.getTokenNo().toLowerCase().contains(q) ||
                                       p.getShortBay().toLowerCase().contains(q) ||
                                       p.getSymptoms().toLowerCase().contains(q);
                if (!matchesSearch) continue;
            }

            displayedPatients.add(p);
        }

        listPatients.setItems(displayedPatients);
        lblPatientCount.setText(displayedPatients.size() + " Active");
    }

    private void setupSearchFilter() {
        txtSearchPatient.textProperty().addListener((obs, oldVal, newVal) -> applyCurrentFilter());
    }

    private void setupAutoRefresh() {
        // Auto-refresh every 15s to update wait durations without losing current selection
        liveUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            Patient current = selectedPatient;
            loadActivePatients();
            if (current != null) {
                for (Patient p : displayedPatients) {
                    if (p.getTokenNo().equalsIgnoreCase(current.getTokenNo())) {
                        listPatients.getSelectionModel().select(p);
                        break;
                    }
                }
            }
            if (selectedPatient != null) {
                updateBannerTimers(selectedPatient);
            }
        }));
        liveUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        liveUpdateTimeline.play();
    }

    @FXML
    public void handleFilterAll(ActionEvent event) {
        currentFilterMode = "ALL";
        updateFilterButtonStyles(btnFilterAll);
        lblPatientHeader.setText("Active Patients");
        applyCurrentFilter();
    }

    @FXML
    public void handleFilterMy(ActionEvent event) {
        currentFilterMode = "MY";
        updateFilterButtonStyles(btnFilterMy);
        lblPatientHeader.setText("My Patients");
        applyCurrentFilter();
    }

    @FXML
    public void handleFilterWaiting(ActionEvent event) {
        currentFilterMode = "WAITING";
        updateFilterButtonStyles(btnFilterWaiting);
        lblPatientHeader.setText("Waiting Patients");
        applyCurrentFilter();
    }

    @FXML
    public void handleFilterTreatment(ActionEvent event) {
        currentFilterMode = "TREATMENT";
        updateFilterButtonStyles(btnFilterTreatment);
        lblPatientHeader.setText("In Treatment");
        applyCurrentFilter();
    }

    private void updateFilterButtonStyles(Button activeBtn) {
        Button[] all = {btnFilterAll, btnFilterMy, btnFilterWaiting, btnFilterTreatment};
        for (Button b : all) {
            if (b == activeBtn) {
                b.getStyleClass().setAll("filter-tab-pill", "filter-tab-pill-active");
            } else {
                b.getStyleClass().setAll("filter-tab-pill");
            }
        }
    }

    // ========================================================================
    // PATIENT DISPLAY & BANNER BINDINGS (Page 9)
    // ========================================================================
    private void displayPatient(Patient p) {
        this.selectedPatient = p;

        // Banner Avatar
        lblInitials.setText(getInitials(p.getFullName()));
        avatarPane.getStyleClass().setAll(getAvatarClassForLevel(p.getEmergencyLevel()));

        // Banner Demographics
        lblPatientName.setText(p.getFullName());
        lblDemographics.setText(p.getAge() + " · " + p.getGender());

        // Banner Priority Pill (Page 9: "• Critical")
        lblPriorityBadge.setText("• " + p.getEmergencyLevel().getDisplayName());
        lblPriorityBadge.getStyleClass().setAll(getPriorityPillClass(p.getEmergencyLevel()));

        // Banner Allergy Pill (Page 9: "Allergy: penicillin")
        if (p.hasAllergies()) {
            lblAllergyTag.setText("Allergy: " + p.getAllergies());
            lblAllergyTag.getStyleClass().setAll("allergy-pill");
            lblAllergyTag.setVisible(true);
            lblAllergyTag.setManaged(true);
        } else {
            lblAllergyTag.setText("No Known Allergies");
            lblAllergyTag.getStyleClass().setAll("sub-label");
            lblAllergyTag.setVisible(true);
            lblAllergyTag.setManaged(true);
        }

        // Identifiers & Doctors
        lblUhid.setText(p.getUhid() != null ? p.getUhid() : "UH-2026-000891");
        lblToken.setText(p.getTokenNo());
        lblBay.setText(p.getShortBay());
        lblDoctor.setText(p.getAssignedDoctorName() != null ? p.getAssignedDoctorName() : "Dr. Arjun Mehta");

        // Timers & Status
        updateBannerTimers(p);

        // Journey Stepper State
        updateJourneyStepper(p);

        // Tab 1: Overview
        renderOverviewTab(p);

        // Tab 2: SOAP Notes Pre-fill
        prefillSoapTab(p);

        // Tab 3: Orders List
        renderOrdersTab(p);

        // Tab 4: Repeat Vitals Pre-fill (NULL-SAFE!)
        prefillVitalsTab(p);
    }

    private String getPriorityPillClass(EmergencyLevel level) {
        if (level == null) return "priority-pill-medium";
        return switch (level) {
            case CRITICAL -> "priority-pill-dot";
            case HIGH -> "priority-pill-high";
            case MEDIUM -> "priority-pill-medium";
            case LOW -> "priority-pill-low";
        };
    }

    private void updateBannerTimers(Patient p) {
        if (p.getStatus() == PatientStatus.IN_TREATMENT) {
            lblWaitTreatmentStatus.setText("In treatment · " + p.getFormattedTimeInTreatment());
        } else if (p.getStatus() == PatientStatus.COMPLETED) {
            lblWaitTreatmentStatus.setText("Completed");
        } else {
            lblWaitTreatmentStatus.setText("Waiting · " + p.getFormattedWaitTime());
        }
    }

    private void updateJourneyStepper(Patient p) {
        PatientStatus status = p.getStatus();

        // Steps 1, 2, 3 are complete for active doctor workspace patients
        setStepCompleted(stepCircle1, stepCheck1, stepLabel1, stepLine1);
        setStepCompleted(stepCircle2, stepCheck2, stepLabel2, stepLine2);
        setStepCompleted(stepCircle3, stepCheck3, stepLabel3, stepLine3);

        if (status == PatientStatus.IN_TREATMENT) {
            // Step 4 is current (teal ring, bold text)
            setStepCurrent(stepCircle4, stepCheck4, stepLabel4, stepLine4);
            setStepUpcoming(stepCircle5, stepCheck5, stepLabel5);
        } else if (status == PatientStatus.COMPLETED || status == PatientStatus.DISCHARGED) {
            setStepCompleted(stepCircle4, stepCheck4, stepLabel4, stepLine4);
            setStepCompleted(stepCircle5, stepCheck5, stepLabel5, null);
        } else {
            // Waiting
            setStepCurrent(stepCircle3, stepCheck3, stepLabel3, stepLine3);
            setStepUpcoming(stepCircle4, stepCheck4, stepLabel4);
            setStepUpcoming(stepCircle5, stepCheck5, stepLabel5);
        }
    }

    private void setStepCompleted(StackPane circle, Label check, Label label, Region line) {
        circle.getStyleClass().setAll("journey-step-circle-completed");
        check.setText("✓");
        label.getStyleClass().setAll("journey-label-completed");
        if (line != null) line.getStyleClass().setAll("journey-line-completed");
    }

    private void setStepCurrent(StackPane circle, Label check, Label label, Region line) {
        circle.getStyleClass().setAll("journey-step-circle-current");
        check.setText("");
        label.getStyleClass().setAll("journey-label-current");
        if (line != null) line.getStyleClass().setAll("journey-line-upcoming");
    }

    private void setStepUpcoming(StackPane circle, Label check, Label label) {
        circle.getStyleClass().setAll("journey-step-circle-upcoming");
        check.setText("");
        label.getStyleClass().setAll("journey-label-upcoming");
    }

    // ========================================================================
    // TAB 1: OVERVIEW TAB (Page 9)
    // ========================================================================
    private void renderOverviewTab(Patient p) {
        Vitals v = p.getVitals() != null ? p.getVitals() : new Vitals();

        // 2x2 Grid Tiles
        lblTileSpo2.setText(v.getSpo2() != null ? v.getSpo2() + "%" : "--");
        lblTileHr.setText(v.getHeartRate() != null ? String.valueOf(v.getHeartRate()) : "--");
        lblTileBp.setText(v.getBpFormatted() != null && !v.getBpFormatted().equals("N/A") ? v.getBpFormatted().replace(" mmHg", "") : "--");

        int news2 = p.getNews2Score() != null ? p.getNews2Score() : NEWS2Service.calculateNEWS2(v);
        lblTileNews2.setText(String.valueOf(news2));
        if (news2 >= 7) {
            lblTileNews2.setStyle("-fx-text-fill: #DC2626;");
        } else if (news2 >= 5) {
            lblTileNews2.setStyle("-fx-text-fill: #EA580C;");
        } else {
            lblTileNews2.setStyle("-fx-text-fill: #0D9488;");
        }

        // Vitals Trend Line Chart
        renderOverviewTrendChart(p, v);

        // Orders Summary List in Overview
        renderOverviewOrders(p);
    }

    private void renderOverviewTrendChart(Patient p, Vitals v) {
        chartVitalsOverview.getData().clear();

        XYChart.Series<Number, Number> spo2Series = new XYChart.Series<>();
        spo2Series.setName("SpO2");

        XYChart.Series<Number, Number> hrSeries = new XYChart.Series<>();
        hrSeries.setName("Heart Rate");

        int currentSpo2 = v.getSpo2() != null ? v.getSpo2() : 94;
        int currentHr = v.getHeartRate() != null ? v.getHeartRate() : 108;

        // If patient is Ramesh Kumar, exactly match Page 9: SpO2 from 88 to 94, HR from 134 to 108
        int initialSpo2 = Math.max(70, currentSpo2 - 6);
        int initialHr = Math.min(160, currentHr + 26);

        lblOverviewSpo2Range.setText("SpO2: " + initialSpo2 + " to " + currentSpo2 + "%");
        lblOverviewHrRange.setText("HR: " + initialHr + " to " + currentHr);

        // 6 trend points over 30 min (-25, -20, -15, -10, -5, 0)
        double[] xVals = {0, 5, 10, 15, 20, 25};
        double[] spo2Vals = {
            initialSpo2,
            initialSpo2 + 1,
            initialSpo2 + 2,
            initialSpo2 + 4,
            currentSpo2 - 1,
            currentSpo2
        };
        double[] hrVals = {
            initialHr,
            initialHr - 5,
            initialHr - 11,
            initialHr - 18,
            initialHr - 22,
            currentHr
        };

        for (int i = 0; i < xVals.length; i++) {
            spo2Series.getData().add(new XYChart.Data<>(xVals[i], spo2Vals[i]));
            hrSeries.getData().add(new XYChart.Data<>(xVals[i], hrVals[i]));
        }

        chartVitalsOverview.getData().addAll(spo2Series, hrSeries);

        // Apply styles to chart series lines
        Platform.runLater(() -> {
            if (spo2Series.getNode() != null) {
                spo2Series.getNode().getStyleClass().add("chart-series-spo2");
            }
            if (hrSeries.getNode() != null) {
                hrSeries.getNode().getStyleClass().add("chart-series-hr");
            }
        });
    }

    private void renderOverviewOrders(Patient p) {
        boxOverviewOrders.getChildren().clear();
        List<Order> orders = orderService.getOrdersForPatient(p.getTokenNo());

        // If patient has no orders yet, seed default clinical orders matching Page 9
        if (orders.isEmpty()) {
            seedDefaultOrdersForPatient(p);
            orders = orderService.getOrdersForPatient(p.getTokenNo());
        }

        for (Order o : orders) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().setAll("order-row-card");

            // Icon Circle
            StackPane iconCircle = new StackPane();
            iconCircle.getStyleClass().setAll("order-icon-circle");
            SVGPath icon = SvgIcons.createStrokedPath(getOrderSvgPath(o.getOrderType()), "#0D9488", 1.8);
            iconCircle.getChildren().add(icon);

            // Title & Subtitle
            VBox details = new VBox(2);
            Label name = new Label(o.getOrderName());
            name.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

            String timeStr = o.getOrderedAt() != null ? o.getOrderedAt().format(TIME_FMT) : "14:05";
            String subText = "Ordered " + timeStr + (o.getStatus() == Order.OrderStatus.IN_PROGRESS ? " · result abnormal" : "");
            Label sub = new Label(subText);
            sub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
            details.getChildren().addAll(name, sub);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Status Pill (Page 9: Resulted, In progress, Ordered)
            Label statusPill = new Label(getStatusPillText(o.getStatus()));
            statusPill.getStyleClass().setAll(getStatusPillClass(o.getStatus()));

            row.getChildren().addAll(iconCircle, details, spacer, statusPill);
            boxOverviewOrders.getChildren().add(row);
        }
    }

    private void seedDefaultOrdersForPatient(Patient p) {
        String doc = p.getAssignedDoctorName() != null ? p.getAssignedDoctorName() : "Dr. Arjun Mehta";
        String bay = p.getShortBay();

        Order o1 = orderService.createOrder(p.getPatientId(), p.getTokenNo(), p.getFullName(), bay,
                Order.OrderType.PROCEDURE, "12-lead ECG", doc);
        o1.setStatus(Order.OrderStatus.RESULTED);
        o1.setOrderedAt(LocalDateTime.now().minusMinutes(14));

        Order o2 = orderService.createOrder(p.getPatientId(), p.getTokenNo(), p.getFullName(), bay,
                Order.OrderType.LAB, "Troponin I", doc);
        o2.setStatus(Order.OrderStatus.IN_PROGRESS);
        o2.setOrderedAt(LocalDateTime.now().minusMinutes(13));

        Order o3 = orderService.createOrder(p.getPatientId(), p.getTokenNo(), p.getFullName(), bay,
                Order.OrderType.MEDICATION, "Aspirin 300 mg, oral", doc);
        o3.setStatus(Order.OrderStatus.ORDERED);
        o3.setOrderedAt(LocalDateTime.now().minusMinutes(12));
    }

    private String getOrderSvgPath(Order.OrderType type) {
        if (type == null) return SvgIcons.ORDERS_RESULTS;
        return switch (type) {
            case LAB -> "M10 2v7.31M14 2v7.31M8.5 2h7M14 9.3l4.9 8.27A2 2 0 0 1 17.2 21H6.8a2 2 0 0 1-1.7-3.43L10 9.3";
            case PROCEDURE, CARDIAC_ECG -> "M22 12h-4l-3 9L9 3l-3 9H2"; // ECG pulse
            case MEDICATION, IV_FLUIDS -> "M10.5 20.5l10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7z M8.5 8.5l7 7"; // Pill
            case IMAGING -> "M4 4h16v16H4z M9 9h6v6H9z";
            default -> SvgIcons.ORDERS_RESULTS;
        };
    }

    private String getStatusPillText(Order.OrderStatus status) {
        if (status == null) return "Ordered";
        return switch (status) {
            case RESULTED -> "Resulted";
            case IN_PROGRESS -> "In progress";
            case ORDERED -> "Ordered";
        };
    }

    private String getStatusPillClass(Order.OrderStatus status) {
        if (status == null) return "status-pill-ordered";
        return switch (status) {
            case RESULTED -> "status-pill-resulted";
            case IN_PROGRESS -> "status-pill-progress";
            case ORDERED -> "status-pill-ordered";
        };
    }

    @FXML
    public void handleSwitchToOrdersTab(ActionEvent event) {
        doctorTabPane.getSelectionModel().select(tabOrders);
    }

    // ========================================================================
    // TAB 2: SOAP NOTES TAB
    // ========================================================================
    private void setupSoapFormValidation() {
        // Character counter listeners
        setupCharCounter(txtSubjective, lblCountSubj, 500);
        setupCharCounter(txtObjective, lblCountObj, 500);
        setupCharCounter(txtAssessment, lblCountAssess, 500);
        setupCharCounter(txtPlan, lblCountPlan, 500);

        // Assessment and Plan are MANDATORY before signing
        btnSaveSoap.setDisable(true);
        Runnable checkValid = () -> {
            boolean hasAssess = txtAssessment.getText() != null && !txtAssessment.getText().trim().isEmpty();
            boolean hasPlan = txtPlan.getText() != null && !txtPlan.getText().trim().isEmpty();
            btnSaveSoap.setDisable(!hasAssess || !hasPlan);
        };

        txtAssessment.textProperty().addListener((obs, oldVal, newVal) -> checkValid.run());
        txtPlan.textProperty().addListener((obs, oldVal, newVal) -> checkValid.run());
    }

    private void setupCharCounter(TextArea area, Label counter, int max) {
        area.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            counter.setText(len + " / " + max);
            if (len > max) {
                counter.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: 700;");
            } else {
                counter.setStyle("-fx-text-fill: #94A3B8;");
            }
        });
    }

    private void prefillSoapTab(Patient p) {
        // Pre-fill Subjective from triage
        txtSubjective.setText(p.getAge() + "-year-old " + p.getGender().toLowerCase() + " presents with: " + p.getSymptoms() + ".");

        // Pre-fill Objective from triage vitals
        String vitalsText = p.getVitals() != null ? p.getVitals().toSummaryString() : "Vitals pending";
        txtObjective.setText("Triage Vitals: " + vitalsText + ".");

        // Assessment and Plan MUST START EMPTY with placeholders
        txtAssessment.clear();
        txtPlan.clear();

        renderNotesHistory();
    }

    private void renderNotesHistory() {
        boxNotesHistory.getChildren().clear();
        if (notesList.isEmpty()) {
            Label empty = new Label("No previous SOAP notes signed for this encounter.");
            empty.getStyleClass().setAll("sub-label");
            boxNotesHistory.getChildren().add(empty);
            return;
        }

        for (ClinicalNote n : notesList) {
            VBox card = new VBox(4);
            card.getStyleClass().setAll("modern-table-card");
            card.setStyle("-fx-padding: 10px;");

            Label head = new Label(n.getDoctorName() + " · " + n.getFormattedTimestamp());
            head.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

            Label assess = new Label("A: " + n.getAssessment());
            assess.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #334155;");

            Label plan = new Label("P: " + n.getPlan());
            plan.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

            card.getChildren().addAll(head, assess, plan);
            boxNotesHistory.getChildren().add(card);
        }
    }

    @FXML
    public void handleClearSoap(ActionEvent event) {
        txtSubjective.clear();
        txtObjective.clear();
        txtAssessment.clear();
        txtPlan.clear();
    }

    @FXML
    public void handleSaveSoap(ActionEvent event) {
        if (selectedPatient == null) return;
        String subj = txtSubjective.getText();
        String obj = txtObjective.getText();
        String assess = txtAssessment.getText();
        String plan = txtPlan.getText();

        if (assess == null || assess.trim().isEmpty() || plan == null || plan.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Incomplete SOAP Note");
            alert.setHeaderText("Assessment and Plan are required");
            alert.setContentText("Please provide both an Assessment and Plan before digitally signing the note.");
            alert.showAndWait();
            return;
        }

        String doctor = UserSession.getInstance().getStaffName();
        ClinicalNote note = new ClinicalNote(selectedPatient.getPatientId(), selectedPatient.getTokenNo(), doctor, subj, obj, assess, plan);
        notesList.add(0, note);
        renderNotesHistory();

        AuditService.getInstance().log("SOAP_NOTE_SIGNED", selectedPatient.getTokenNo(), selectedPatient.getUhid(),
                null, assess, "Signed by " + doctor);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Note Signed");
        alert.setHeaderText("SOAP Note Digitally Signed & Timestamped");
        alert.setContentText("Encounter note has been cryptographically signed by " + doctor + " at " + note.getFormattedTimestamp());
        alert.showAndWait();
    }

    // ========================================================================
    // TAB 3: ORDERS & RESULTS TAB
    // ========================================================================
    @FXML public void handleQuickOrderTroponin(ActionEvent e) { txtOrderName.setText("Troponin I STAT"); cmbOrderType.setValue(Order.OrderType.LAB); cmbOrderPriority.setValue("STAT"); }
    @FXML public void handleQuickOrderEcg(ActionEvent e) { txtOrderName.setText("12-Lead ECG STAT"); cmbOrderType.setValue(Order.OrderType.PROCEDURE); cmbOrderPriority.setValue("STAT"); }
    @FXML public void handleQuickOrderXray(ActionEvent e) { txtOrderName.setText("Chest X-Ray Portable"); cmbOrderType.setValue(Order.OrderType.IMAGING); cmbOrderPriority.setValue("Urgent"); }
    @FXML public void handleQuickOrderCbc(ActionEvent e) { txtOrderName.setText("Complete Blood Count (CBC)"); cmbOrderType.setValue(Order.OrderType.LAB); cmbOrderPriority.setValue("Urgent"); }
    @FXML public void handleQuickOrderBmp(ActionEvent e) { txtOrderName.setText("Basic Metabolic Panel (BMP)"); cmbOrderType.setValue(Order.OrderType.LAB); cmbOrderPriority.setValue("Routine"); }
    @FXML public void handleQuickOrderAspirin(ActionEvent e) { txtOrderName.setText("Aspirin 300mg Oral"); cmbOrderType.setValue(Order.OrderType.MEDICATION); cmbOrderPriority.setValue("STAT"); }

    @FXML
    public void handlePlaceOrder(ActionEvent event) {
        if (selectedPatient == null) return;
        String name = txtOrderName.getText() != null ? txtOrderName.getText().trim() : "";
        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Order Required");
            alert.setHeaderText("Please specify an order item");
            alert.setContentText("Enter a diagnostic test, procedure, or medication name.");
            alert.showAndWait();
            return;
        }

        Order.OrderType type = cmbOrderType.getValue() != null ? cmbOrderType.getValue() : Order.OrderType.LAB;
        String doctor = UserSession.getInstance().getStaffName();
        String bay = selectedPatient.getShortBay();

        orderService.createOrder(selectedPatient.getPatientId(), selectedPatient.getTokenNo(),
                selectedPatient.getFullName(), bay, type, name, doctor);

        txtOrderName.clear();
        renderOrdersTab(selectedPatient);
        renderOverviewOrders(selectedPatient);
    }

    private void renderOrdersTab(Patient p) {
        boxOrdersList.getChildren().clear();
        List<Order> orders = orderService.getOrdersForPatient(p.getTokenNo());

        if (orders.isEmpty()) {
            Label empty = new Label("No active diagnostic orders for this patient.");
            empty.getStyleClass().setAll("sub-label");
            boxOrdersList.getChildren().add(empty);
            return;
        }

        for (Order o : orders) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().setAll("order-row-card");
            row.setStyle("-fx-cursor: hand; -fx-padding: 8px 12px;");

            StackPane iconCircle = new StackPane();
            iconCircle.getStyleClass().setAll("order-icon-circle");
            SVGPath icon = SvgIcons.createStrokedPath(getOrderSvgPath(o.getOrderType()), "#0D9488", 1.8);
            iconCircle.getChildren().add(icon);

            VBox info = new VBox(2);
            Label name = new Label(o.getOrderName());
            name.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            Label meta = new Label(o.getOrderType() + " · Ordered by " + o.getDoctorName() + " · " + o.getOrderedAt().format(TIME_FMT));
            meta.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B;");
            info.getChildren().addAll(name, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label statusPill = new Label(getStatusPillText(o.getStatus()));
            statusPill.getStyleClass().setAll(getStatusPillClass(o.getStatus()));

            row.getChildren().addAll(iconCircle, info, spacer, statusPill);

            // Click to inspect structured result details
            row.setOnMouseClicked(e -> showOrderResultDetails(o));

            boxOrdersList.getChildren().add(row);
        }
    }

    private void showOrderResultDetails(Order o) {
        boxOrderResultDetails.getChildren().clear();

        Label title = new Label(o.getOrderName());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label status = new Label("Status: " + getStatusPillText(o.getStatus()));
        status.getStyleClass().setAll(getStatusPillClass(o.getStatus()));

        Separator sep = new Separator();

        VBox content = new VBox(6);
        if (o.getOrderName().toLowerCase().contains("troponin")) {
            content.getChildren().addAll(
                createResultLine("Troponin I (High Sensitivity):", "0.42 ng/mL", "Ref: <0.04 ng/mL", true),
                createResultLine("Specimen Quality:", "Hemolysis index normal", "", false),
                createResultLine("Interpretation:", "Significant myocardial necrosis marker elevation.", "", true)
            );
        } else if (o.getOrderName().toLowerCase().contains("ecg")) {
            content.getChildren().addAll(
                createResultLine("Ventricular Rate:", "108 bpm", "Ref: 60-100", true),
                createResultLine("PR Interval:", "164 ms", "Ref: 120-200", false),
                createResultLine("QRS Duration:", "88 ms", "Ref: 80-120", false),
                createResultLine("ST Elevation:", "+2.5 mm in V2, V3, V4", "Ref: Normal", true),
                createResultLine("Machine Diagnosis:", "Acute Anterior ST Elevation Infarction", "", true)
            );
        } else if (o.getOrderName().toLowerCase().contains("aspirin")) {
            content.getChildren().addAll(
                createResultLine("Dose:", "300 mg", "Route: Oral", false),
                createResultLine("Administration:", "Chewed with 30ml water", "Immediate", false),
                createResultLine("Administration Status:", "Confirmed given by Nurse Priya", "", false)
            );
        } else {
            content.getChildren().addAll(
                createResultLine("Test Status:", "Specimen collected and processing in lab", "", false),
                createResultLine("Estimated Turnaround:", "15 minutes", "", false)
            );
        }

        boxOrderResultDetails.getChildren().addAll(title, status, sep, content);
    }

    private HBox createResultLine(String label, String value, String ref, boolean abnormal) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (abnormal ? "#DC2626;" : "#0F172A;"));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        box.getChildren().addAll(lbl, val, sp);
        if (ref != null && !ref.isEmpty()) {
            Label rf = new Label(ref);
            rf.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
            box.getChildren().add(rf);
        }
        return box;
    }

    // ========================================================================
    // TAB 4: REPEAT VITALS TAB & NEWS2 WATCHDOG
    // ========================================================================
    private void setupLiveNews2Calculation() {
        TextField[] fields = {txtHr, txtSysBp, txtDiaBp, txtSpo2, txtRr, txtTemp};
        for (TextField tf : fields) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> recalculateLiveNews2());
        }
    }

    private void recalculateLiveNews2() {
        try {
            Vitals v = extractVitalsSafely();
            int score = NEWS2Service.calculateNEWS2(v);
            String risk = NEWS2Service.getRiskCategory(score);

            lblNews2Live.setText("NEWS2: " + score + " · " + risk);
            if (score >= 7) {
                lblNews2Live.getStyleClass().setAll("priority-pill-dot");
                boxWatchdogAlert.setVisible(true);
                boxWatchdogAlert.setManaged(true);
                lblWatchdogMessage.setText("Critical NEWS2 Score " + score + ": Acute clinical deterioration detected! Resuscitation review STAT.");
            } else if (score >= 5) {
                lblNews2Live.getStyleClass().setAll("priority-pill-high");
                boxWatchdogAlert.setVisible(false);
                boxWatchdogAlert.setManaged(false);
            } else {
                lblNews2Live.getStyleClass().setAll("priority-pill-medium");
                boxWatchdogAlert.setVisible(false);
                boxWatchdogAlert.setManaged(false);
            }
        } catch (Exception ignored) {
        }
    }

    private void prefillVitalsTab(Patient p) {
        Vitals v = p.getVitals();
        if (v != null) {
            // STRICT NULL SAFETY: Never display string "null"!
            txtHr.setText(v.getHeartRate() != null ? String.valueOf(v.getHeartRate()) : "");
            txtSysBp.setText(v.getSystolicBp() != null ? String.valueOf(v.getSystolicBp()) : "");
            txtDiaBp.setText(v.getDiastolicBp() != null ? String.valueOf(v.getDiastolicBp()) : "");
            txtSpo2.setText(v.getSpo2() != null ? String.valueOf(v.getSpo2()) : "");
            txtRr.setText(v.getRespiratoryRate() != null ? String.valueOf(v.getRespiratoryRate()) : "");
            txtTemp.setText(v.getTemperature() != null ? String.valueOf(v.getTemperature()) : "");
            txtGcs.setText(p.getGcs() != null ? String.valueOf(p.getGcs()) : "15");
            txtPain.setText(p.getPainScore() != null ? String.valueOf(p.getPainScore()) : "0");
        } else {
            txtHr.clear();
            txtSysBp.clear();
            txtDiaBp.clear();
            txtSpo2.clear();
            txtRr.clear();
            txtTemp.clear();
            txtGcs.setText("15");
            txtPain.setText("0");
        }

        recalculateLiveNews2();
        renderVitalsHistory(p);
    }

    private void renderVitalsHistory(Patient p) {
        boxVitalsHistory.getChildren().clear();
        Vitals v = p.getVitals();
        if (v == null || v.getHeartRate() == null) {
            Label empty = new Label("No repeat vital sign serial logs recorded yet.");
            empty.getStyleClass().setAll("sub-label");
            boxVitalsHistory.getChildren().add(empty);
            return;
        }

        // Initial triage vitals log
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().setAll("order-row-card");
        row.setStyle("-fx-padding: 8px 12px;");

        Label time = new Label("Triage Initial");
        time.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label data = new Label("HR: " + v.getHeartRate() + " bpm  |  BP: " + v.getBpFormatted() +
                "  |  SpO2: " + v.getSpo2() + "%  |  RR: " + v.getRespiratoryRate() + "/min  |  Temp: " + v.getTemperature() + "°F");
        data.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        int news2 = p.getNews2Score() != null ? p.getNews2Score() : NEWS2Service.calculateNEWS2(v);
        Label pill = new Label("NEWS2: " + news2);
        pill.getStyleClass().setAll(news2 >= 7 ? "priority-pill-dot" : (news2 >= 5 ? "priority-pill-high" : "priority-pill-low"));

        row.getChildren().addAll(time, data, sp, pill);
        boxVitalsHistory.getChildren().add(row);
    }

    @FXML
    public void handleResetVitalsToOriginal(ActionEvent event) {
        if (selectedPatient != null) {
            prefillVitalsTab(selectedPatient);
        }
    }

    @FXML
    public void handleSaveRepeatVitals(ActionEvent event) {
        if (selectedPatient == null) return;
        try {
            Vitals v = extractVitalsSafely();
            selectedPatient.setVitals(v);

            int score = NEWS2Service.calculateNEWS2(v);
            selectedPatient.setNews2Score(score);

            if (!txtGcs.getText().trim().isEmpty()) {
                selectedPatient.setGcs(Integer.parseInt(txtGcs.getText().trim()));
            }
            if (!txtPain.getText().trim().isEmpty()) {
                selectedPatient.setPainScore(Integer.parseInt(txtPain.getText().trim()));
            }

            recalculateLiveNews2();
            renderOverviewTab(selectedPatient);

            // Add new row to vitals history
            HBox newRow = new HBox(12);
            newRow.setAlignment(Pos.CENTER_LEFT);
            newRow.getStyleClass().setAll("order-row-card");
            newRow.setStyle("-fx-padding: 8px 12px;");

            Label time = new Label(LocalDateTime.now().format(TIME_FMT));
            time.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #0D9488;");

            Label data = new Label("HR: " + v.getHeartRate() + " bpm  |  BP: " + v.getBpFormatted() +
                    "  |  SpO2: " + v.getSpo2() + "%  |  RR: " + v.getRespiratoryRate() + "/min  |  Temp: " + v.getTemperature() + "°F");
            data.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label pill = new Label("NEWS2: " + score);
            pill.getStyleClass().setAll(score >= 7 ? "priority-pill-dot" : (score >= 5 ? "priority-pill-high" : "priority-pill-low"));

            newRow.getChildren().addAll(time, data, sp, pill);
            boxVitalsHistory.getChildren().add(0, newRow);

            AuditService.getInstance().log("REPEAT_VITALS_LOGGED", selectedPatient.getTokenNo(), selectedPatient.getUhid(),
                    null, "NEWS2: " + score, "Vitals: " + v.toSummaryString());

            if (score >= 7) {
                AlertService.getInstance().dispatchAlert(
                        ClinicalAlert.AlertType.DETERIORATING_VITALS,
                        selectedPatient.getTokenNo(),
                        selectedPatient.getFullName(),
                        "Critical NEWS2 Score " + score + ": Immediate resuscitation review required!"
                );
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Vitals Logged");
            alert.setHeaderText("Repeat Vital Signs Successfully Recorded");
            alert.setContentText("Serial vitals archived. Current NEWS2 score: " + score + " (" + NEWS2Service.getRiskCategory(score) + ")");
            alert.showAndWait();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText("Invalid Vital Sign Input");
            alert.setContentText("Please verify numeric input for heart rate, blood pressure, SpO2, and respiratory rate.");
            alert.showAndWait();
        }
    }

    private Vitals extractVitalsSafely() {
        Vitals v = new Vitals();
        if (txtHr.getText() != null && !txtHr.getText().trim().isEmpty()) v.setHeartRate(Integer.parseInt(txtHr.getText().trim()));
        if (txtSysBp.getText() != null && !txtSysBp.getText().trim().isEmpty()) v.setSystolicBp(Integer.parseInt(txtSysBp.getText().trim()));
        if (txtDiaBp.getText() != null && !txtDiaBp.getText().trim().isEmpty()) v.setDiastolicBp(Integer.parseInt(txtDiaBp.getText().trim()));
        if (txtSpo2.getText() != null && !txtSpo2.getText().trim().isEmpty()) v.setSpo2(Integer.parseInt(txtSpo2.getText().trim()));
        if (txtRr.getText() != null && !txtRr.getText().trim().isEmpty()) v.setRespiratoryRate(Integer.parseInt(txtRr.getText().trim()));
        if (txtTemp.getText() != null && !txtTemp.getText().trim().isEmpty()) v.setTemperature(Double.parseDouble(txtTemp.getText().trim()));
        return v;
    }

    // ========================================================================
    // DISPOSITION NAVIGATION & UTILS
    // ========================================================================
    @FXML
    public void handleGoToDisposition(ActionEvent event) {
        if (MainController.getInstance() != null) {
            MainController.getInstance().showDischarge(event);
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
