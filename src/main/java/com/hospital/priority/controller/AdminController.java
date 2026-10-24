package com.hospital.priority.controller;

import com.hospital.priority.dao.DoctorDao;
import com.hospital.priority.dao.UserDao;
import com.hospital.priority.dao.impl.DoctorDaoImpl;
import com.hospital.priority.dao.impl.UserDaoImpl;
import com.hospital.priority.model.*;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.BayService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * ADMIN CONTROLLER (Page 16)
 * ============================================================================
 * Coordinates user management, doctor duty roster, emergency bay maintenance,
 * and Page 18 medical-legal compliance audit trail inspection.
 */
public class AdminController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss");

    // Tab 1: Staff Users
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Number> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colUserFullName;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;

    // Tab 2: Doctors
    @FXML private TableView<Doctor> tableDoctors;
    @FXML private TableColumn<Doctor, Number> colDocId;
    @FXML private TableColumn<Doctor, String> colDocName;
    @FXML private TableColumn<Doctor, String> colDocSpecialty;
    @FXML private TableColumn<Doctor, String> colDocStatus;
    @FXML private TableColumn<Doctor, Void> colDocAction;

    // Tab 3: Bays
    @FXML private TableView<Bay> tableBays;
    @FXML private TableColumn<Bay, Number> colBayId;
    @FXML private TableColumn<Bay, String> colBayName;
    @FXML private TableColumn<Bay, String> colBayType;
    @FXML private TableColumn<Bay, String> colBayStatus;
    @FXML private TableColumn<Bay, String> colBayPatient;
    @FXML private TableColumn<Bay, Void> colBayAction;

    // Tab 4: Audit Trail
    @FXML private TextField txtSearchAudit;
    @FXML private TableView<AuditLog> tableAudit;
    @FXML private TableColumn<AuditLog, String> colAuditTimestamp;
    @FXML private TableColumn<AuditLog, String> colAuditUser;
    @FXML private TableColumn<AuditLog, String> colAuditRole;
    @FXML private TableColumn<AuditLog, String> colAuditAction;
    @FXML private TableColumn<AuditLog, String> colAuditPatient;
    @FXML private TableColumn<AuditLog, String> colAuditDetails;

    private final UserDao userDao = new UserDaoImpl();
    private final DoctorDao doctorDao = new DoctorDaoImpl();
    private final BayService bayService = BayService.getInstance();
    private final AuditService auditService = AuditService.getInstance();

    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final ObservableList<Doctor> masterDoctors = FXCollections.observableArrayList();
    private final ObservableList<Bay> masterBays = FXCollections.observableArrayList();
    private final ObservableList<AuditLog> masterAudit = FXCollections.observableArrayList();
    private FilteredList<AuditLog> filteredAudit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserColumns();
        setupDoctorColumns();
        setupBayColumns();
        setupAuditColumns();

        loadAllData();
    }

    private void setupUserColumns() {
        colUserId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getUserId()));
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colUserFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colUserRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole().getTitle()));
        colUserStatus.setCellValueFactory(cell -> new SimpleStringProperty("Active"));

        colUserRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String roleTitle, boolean empty) {
                super.updateItem(roleTitle, empty);
                if (empty || roleTitle == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label chip = new Label(roleTitle);
                    chip.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #0D9488; -fx-background-color: #F0FDFA; -fx-padding: 3px 8px; -fx-background-radius: 999px; -fx-border-color: #99F6E4; -fx-border-radius: 999px;");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        tableUsers.setItems(masterUsers);
    }

    private void setupDoctorColumns() {
        colDocId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getDoctorId()));
        colDocName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colDocSpecialty.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpecialty().getDisplayName()));
        colDocStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        colDocStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(st);
                    if ("AVAILABLE".equalsIgnoreCase(st)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else if ("BUSY".equalsIgnoreCase(st)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #C2410C; -fx-background-color: #FFEDD5; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B; -fx-background-color: #F1F5F9; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        colDocAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button("Toggle Duty");

            {
                btnToggle.getStyleClass().add("btn-modern-pill-secondary");
                btnToggle.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
                btnToggle.setOnAction(e -> {
                    Doctor d = getTableView().getItems().get(getIndex());
                    if (d != null) {
                        DoctorStatus next = d.getStatus() == DoctorStatus.AVAILABLE ? DoctorStatus.BUSY
                                : (d.getStatus() == DoctorStatus.BUSY ? DoctorStatus.OFF_DUTY : DoctorStatus.AVAILABLE);
                        d.setStatus(next);
                        try {
                            doctorDao.updateStatus(d.getDoctorId(), next);
                        } catch (Exception ignored) {}
                        tableDoctors.refresh();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnToggle);
            }
        });

        tableDoctors.setItems(masterDoctors);
    }

    private void setupBayColumns() {
        colBayId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getBayId()));
        colBayName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colBayType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().getDisplayName()));
        colBayStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        colBayPatient.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCurrentPatientName() != null ? cell.getValue().getCurrentPatientName() + " (" + cell.getValue().getCurrentPatientToken() + ")" : "—"));

        colBayStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(st);
                    if ("FREE".equalsIgnoreCase(st)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else if ("OCCUPIED".equalsIgnoreCase(st)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else if ("CLEANING".equalsIgnoreCase(st)) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #A16207; -fx-background-color: #FEF9C3; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B; -fx-background-color: #F1F5F9; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        colBayAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnAction = new Button("Set Maintenance");

            {
                btnAction.getStyleClass().add("btn-modern-pill-secondary");
                btnAction.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
                btnAction.setOnAction(e -> {
                    Bay b = getTableView().getItems().get(getIndex());
                    if (b != null) {
                        if (b.getStatus() == Bay.BayStatus.BLOCKED) {
                            b.markFree();
                        } else if (b.getStatus() == Bay.BayStatus.CLEANING) {
                            b.markCleaned();
                        } else if (b.getStatus() == Bay.BayStatus.FREE) {
                            b.setStatus(Bay.BayStatus.BLOCKED);
                        }
                        tableBays.refresh();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Bay b = getTableView().getItems().get(getIndex());
                    if (b != null && b.getStatus() == Bay.BayStatus.BLOCKED) {
                        btnAction.setText("Unblock Bay");
                    } else if (b != null && b.getStatus() == Bay.BayStatus.CLEANING) {
                        btnAction.setText("Mark Clean");
                    } else {
                        btnAction.setText("Block / Maint");
                    }
                    setGraphic(btnAction);
                }
            }
        });

        tableBays.setItems(masterBays);
    }

    private void setupAuditColumns() {
        colAuditTimestamp.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimestamp().format(TIME_FMT)));
        colAuditUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colAuditRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole().getTitle()));
        colAuditAction.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAction()));
        colAuditPatient.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPatientToken() != null ? cell.getValue().getPatientToken() : "—"));
        colAuditDetails.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDetails()));

        colAuditAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label chip = new Label(action);
                    chip.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-font-family: monospace; -fx-text-fill: #0F172A; -fx-background-color: #F1F5F9; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        filteredAudit = new FilteredList<>(masterAudit, p -> true);
        tableAudit.setItems(filteredAudit);

        txtSearchAudit.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal != null ? newVal.trim().toLowerCase() : "";
            filteredAudit.setPredicate(a -> {
                if (q.isEmpty()) return true;
                boolean matchUser = a.getUsername().toLowerCase().contains(q);
                boolean matchAction = a.getAction().toLowerCase().contains(q);
                boolean matchToken = a.getPatientToken() != null && a.getPatientToken().toLowerCase().contains(q);
                boolean matchDetails = a.getDetails() != null && a.getDetails().toLowerCase().contains(q);
                return matchUser || matchAction || matchToken || matchDetails;
            });
        });
    }

    @FXML
    public void handleRefreshAll(ActionEvent event) {
        loadAllData();
    }

    private void loadAllData() {
        // 1. Users
        masterUsers.clear();
        masterUsers.add(new User(1, "admin", "", "System Administrator", Role.ADMIN, LocalDateTime.now()));
        masterUsers.add(new User(2, "dr_sharma", "", "Dr. Rajesh Sharma", Role.DOCTOR, LocalDateTime.now()));
        masterUsers.add(new User(3, "nurse_triage", "", "Sister Priya S.", Role.TRIAGE_NURSE, LocalDateTime.now()));
        masterUsers.add(new User(4, "charge_nurse", "", "Sister Anita Roy", Role.CHARGE_NURSE, LocalDateTime.now()));
        masterUsers.add(new User(5, "receptionist", "", "Rohan Verma", Role.RECEPTIONIST, LocalDateTime.now()));

        // 2. Doctors
        masterDoctors.clear();
        try {
            masterDoctors.addAll(doctorDao.findAll());
        } catch (Exception e) {
            masterDoctors.add(new Doctor(1, "Dr. Rajesh Sharma", Specialty.CARDIOLOGY, DoctorStatus.AVAILABLE, 2, "Room 101", "+91 98200 11223"));
            masterDoctors.add(new Doctor(2, "Dr. Sunita Rao", Specialty.ORTHOPEDICS, DoctorStatus.AVAILABLE, 1, "Room 102", "+91 98200 22334"));
            masterDoctors.add(new Doctor(3, "Dr. Meenakshi Sundaram", Specialty.NEUROLOGY, DoctorStatus.BUSY, 3, "Room 103", "+91 98200 33445"));
            masterDoctors.add(new Doctor(4, "Dr. Vikram Miller", Specialty.GENERAL_EMERGENCY, DoctorStatus.AVAILABLE, 2, "ER Bay Area", "+91 98200 44556"));
            masterDoctors.add(new Doctor(5, "Dr. Amit Patel", Specialty.PULMONOLOGY, DoctorStatus.AVAILABLE, 1, "Room 104", "+91 98200 55667"));
        }

        // 3. Bays
        masterBays.clear();
        masterBays.addAll(bayService.getAllBays());

        // 4. Audit
        masterAudit.clear();
        masterAudit.addAll(auditService.getAllLogs());
    }

    @FXML
    public void handleClearAllCleaning(ActionEvent event) {
        for (Bay b : bayService.getAllBays()) {
            if (b.getStatus() == Bay.BayStatus.CLEANING) {
                b.markCleaned();
            }
        }
        masterBays.clear();
        masterBays.addAll(bayService.getAllBays());
    }

    @FXML
    public void handleCreateUserDialog(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Hospital Staff User");
        dialog.setHeaderText("Create credential for ED staff member:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtUName = new TextField();
        txtUName.setPromptText("e.g. dr_singh");
        TextField txtFName = new TextField();
        txtFName.setPromptText("e.g. Dr. Harpreet Singh");
        ComboBox<Role> cmbRole = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        cmbRole.getSelectionModel().select(Role.DOCTOR);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(txtUName, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(txtFName, 1, 1);
        grid.add(new Label("Hospital Role:"), 0, 2);
        grid.add(cmbRole, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String u = txtUName.getText().trim();
            String f = txtFName.getText().trim();
            Role r = cmbRole.getValue();
            if (!u.isEmpty() && !f.isEmpty()) {
                User newUser = new User(masterUsers.size() + 1, u, "", f, r, LocalDateTime.now());
                masterUsers.add(newUser);
                AuditService.getInstance().log("USER_CREATED", null, null, null, u, "Role: " + r.name());
            }
        }
    }
}
