package com.hospital.priority.controller;

import com.hospital.priority.model.Order;
import com.hospital.priority.model.OrderResultItem;
import com.hospital.priority.service.AuditService;
import com.hospital.priority.service.OrderService;
import com.hospital.priority.service.UserSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ============================================================================
 * ORDERS AND RESULTS CONTROLLER (Page 11)
 * ============================================================================
 * Centralized diagnostic monitor for labs, imaging, ECGs, and medications.
 * Tracks turn-around times, highlights abnormal findings, and supports clinical acknowledgement.
 */
public class OrdersResultsController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label lblTotalOrders;
    @FXML private Label lblPendingOrders;
    @FXML private Label lblResultedOrders;
    @FXML private Label lblAbnormalOrders;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterLab;
    @FXML private Button btnFilterImaging;
    @FXML private Button btnFilterEcg;
    @FXML private Button btnFilterMeds;

    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtSearch;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Number> colId;
    @FXML private TableColumn<Order, String> colPatient;
    @FXML private TableColumn<Order, String> colBay;
    @FXML private TableColumn<Order, String> colCategory;
    @FXML private TableColumn<Order, String> colOrderName;
    @FXML private TableColumn<Order, String> colOrderedBy;
    @FXML private TableColumn<Order, Order.OrderStatus> colStatus;
    @FXML private TableColumn<Order, String> colResults;
    @FXML private TableColumn<Order, Void> colAction;

    private final OrderService orderService = OrderService.getInstance();
    private final ObservableList<Order> masterList = FXCollections.observableArrayList();
    private FilteredList<Order> filteredList;

    private String activeCategory = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatusCombo();
        setupTableColumns();
        setupFilters();
        refreshOrders();
    }

    private void setupStatusCombo() {
        cmbStatus.setItems(FXCollections.observableArrayList("All Statuses", "Ordered", "In Progress", "Resulted"));
        cmbStatus.getSelectionModel().selectFirst();
        cmbStatus.setOnAction(e -> applyFilter());
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getOrderId()));
        colPatient.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPatientName() + "\n(" + cell.getValue().getTokenNo() + ")"));
        colBay.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBayName()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrderType().name()));
        colOrderName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrderName()));
        colOrderedBy.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDoctorName() + "\n" + cell.getValue().getOrderedAt().format(TIME_FMT)));
        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getStatus()));

        // Bay Chip Cell
        colBay.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String bay, boolean empty) {
                super.updateItem(bay, empty);
                if (empty || bay == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label chip = new Label(bay);
                    chip.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #0F172A; " +
                            "-fx-background-color: #F8FAFC; -fx-padding: 3px 8px; -fx-background-radius: 999px; " +
                            "-fx-border-color: #CBD5E1; -fx-border-radius: 999px;");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        // Category Cell
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label chip = new Label(cat);
                    chip.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #0D9488; " +
                            "-fx-background-color: #F0FDFA; -fx-padding: 3px 8px; -fx-background-radius: 999px; " +
                            "-fx-border-color: #99F6E4; -fx-border-radius: 999px;");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        // Status Cell
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Order.OrderStatus st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(st.name());
                    if (st == Order.OrderStatus.RESULTED) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else if (st == Order.OrderStatus.IN_PROGRESS) {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #A16207; -fx-background-color: #FEF9C3; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    } else {
                        pill.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 3px 8px; -fx-background-radius: 999px;");
                    }
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Findings / Results Cell with Abnormal Highlights
        colResults.setCellValueFactory(cell -> {
            Order o = cell.getValue();
            if (o.getResultItems().isEmpty()) {
                return new SimpleStringProperty(o.getStatus() == Order.OrderStatus.RESULTED ? "Normal findings reported" : "Awaiting laboratory processing");
            }
            StringBuilder sb = new StringBuilder();
            for (OrderResultItem r : o.getResultItems()) {
                sb.append(r.getParameterName()).append(": ").append(r.getValue());
                if (!"Normal".equalsIgnoreCase(r.getFlag())) {
                    sb.append(" [").append(r.getFlag().toUpperCase()).append("]");
                }
                sb.append("  ");
            }
            return new SimpleStringProperty(sb.toString().trim());
        });

        colResults.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Order o = getTableRow() != null ? getTableRow().getItem() : null;
                    Label lbl = new Label(val);
                    if (o != null && o.isAbnormal()) {
                        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
                    } else {
                        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
                    }
                    lbl.setWrapText(true);
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        // Action Cell (Acknowledge)
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnAck = new Button("Acknowledge");

            {
                btnAck.getStyleClass().add("btn-modern-pill-secondary");
                btnAck.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
                btnAck.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    if (o != null) {
                        String staff = UserSession.getInstance().getStaffName();
                        orderService.acknowledgeResult(o.getOrderId(), staff);
                        refreshOrders();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Order o = getTableView().getItems().get(getIndex());
                    if (o != null && o.getStatus() != Order.OrderStatus.RESULTED) {
                        setGraphic(btnAck);
                    } else {
                        Label done = new Label("✓ Signed");
                        done.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #15803D;");
                        setGraphic(done);
                    }
                }
            }
        });
    }

    private void setupFilters() {
        filteredList = new FilteredList<>(masterList, p -> true);
        ordersTable.setItems(filteredList);

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        refreshOrders();
    }

    @FXML
    public void filterAll(ActionEvent event) {
        setCategoryFilter(btnFilterAll, "ALL");
    }

    @FXML
    public void filterLab(ActionEvent event) {
        setCategoryFilter(btnFilterLab, "LAB");
    }

    @FXML
    public void filterImaging(ActionEvent event) {
        setCategoryFilter(btnFilterImaging, "IMAGING");
    }

    @FXML
    public void filterEcg(ActionEvent event) {
        setCategoryFilter(btnFilterEcg, "CARDIAC_ECG");
    }

    @FXML
    public void filterMeds(ActionEvent event) {
        setCategoryFilter(btnFilterMeds, "MEDICATION");
    }

    private void setCategoryFilter(Button btn, String cat) {
        btnFilterAll.getStyleClass().setAll("filter-tab-pill");
        btnFilterLab.getStyleClass().setAll("filter-tab-pill");
        btnFilterImaging.getStyleClass().setAll("filter-tab-pill");
        btnFilterEcg.getStyleClass().setAll("filter-tab-pill");
        btnFilterMeds.getStyleClass().setAll("filter-tab-pill");

        btn.getStyleClass().setAll("filter-tab-pill-active");
        this.activeCategory = cat;
        applyFilter();
    }

    private void applyFilter() {
        String query = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String statusFilter = cmbStatus.getValue();

        filteredList.setPredicate(o -> {
            // Category check
            if (!"ALL".equals(activeCategory) && !o.getOrderType().name().equalsIgnoreCase(activeCategory)) {
                return false;
            }

            // Status check
            if (statusFilter != null && !statusFilter.equals("All Statuses")) {
                if (statusFilter.equals("Ordered") && o.getStatus() != Order.OrderStatus.ORDERED) return false;
                if (statusFilter.equals("In Progress") && o.getStatus() != Order.OrderStatus.IN_PROGRESS) return false;
                if (statusFilter.equals("Resulted") && o.getStatus() != Order.OrderStatus.RESULTED) return false;
            }

            // Search query
            if (!query.isEmpty()) {
                boolean matchName = o.getPatientName() != null && o.getPatientName().toLowerCase().contains(query);
                boolean matchToken = o.getTokenNo() != null && o.getTokenNo().toLowerCase().contains(query);
                boolean matchOrder = o.getOrderName() != null && o.getOrderName().toLowerCase().contains(query);
                return matchName || matchToken || matchOrder;
            }

            return true;
        });
    }

    public void refreshOrders() {
        List<Order> list = orderService.getAllOrders();
        masterList.setAll(list);

        long total = list.size();
        long pending = list.stream().filter(o -> o.getStatus() != Order.OrderStatus.RESULTED).count();
        long resulted = list.stream().filter(o -> o.getStatus() == Order.OrderStatus.RESULTED).count();
        long abnormal = list.stream().filter(Order::isAbnormal).count();

        lblTotalOrders.setText(total + " Orders Total");
        lblPendingOrders.setText(pending + " Pending");
        lblResultedOrders.setText(resulted + " Resulted");
        lblAbnormalOrders.setText(abnormal + " Abnormal");

        applyFilter();
    }
}
