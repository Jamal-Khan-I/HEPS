package com.hospital.priority.controller;

import com.hospital.priority.model.ClinicalAlert;
import com.hospital.priority.service.AlertService;
import com.hospital.priority.service.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ALERTS CENTER CONTROLLER (Page 14)
 * ============================================================================
 * Displays clinical alerts, vital deterioration warnings, SLA breaches,
 * and allows medical staff to acknowledge critical notifications.
 */
public class AlertsController implements Initializable {

    @FXML private Label lblActiveCount;
    @FXML private Label lblAcknowledgedCount;

    @FXML private Button btnFilterActive;
    @FXML private Button btnFilterAcknowledged;
    @FXML private Button btnFilterAll;
    @FXML private TextField txtSearch;

    @FXML private VBox boxAlertsFeed;

    private final AlertService alertService = AlertService.getInstance();
    private String activeFilter = "ACTIVE";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearch();
        renderAlerts();
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> renderAlerts());
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        renderAlerts();
    }

    @FXML
    public void filterActive(ActionEvent event) {
        setFilter(btnFilterActive, "ACTIVE");
    }

    @FXML
    public void filterAcknowledged(ActionEvent event) {
        setFilter(btnFilterAcknowledged, "ACKNOWLEDGED");
    }

    @FXML
    public void filterAll(ActionEvent event) {
        setFilter(btnFilterAll, "ALL");
    }

    private void setFilter(Button btn, String filter) {
        btnFilterActive.getStyleClass().setAll("filter-tab-pill");
        btnFilterAcknowledged.getStyleClass().setAll("filter-tab-pill");
        btnFilterAll.getStyleClass().setAll("filter-tab-pill");

        btn.getStyleClass().setAll("filter-tab-pill-active");
        this.activeFilter = filter;
        renderAlerts();
    }

    public void renderAlerts() {
        List<ClinicalAlert> list;
        if ("ACTIVE".equals(activeFilter)) {
            list = alertService.getActiveAlerts();
        } else if ("ACKNOWLEDGED".equals(activeFilter)) {
            list = alertService.getAcknowledgedAlerts();
        } else {
            list = alertService.getAllAlerts();
        }

        // Update top stats
        lblActiveCount.setText(alertService.getActiveAlerts().size() + " Active Alerts");
        lblAcknowledgedCount.setText(alertService.getAcknowledgedAlerts().size() + " Acknowledged");

        // Filter by search query
        String q = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        if (!q.isEmpty()) {
            list = list.stream().filter(a ->
                    a.getTitle().toLowerCase().contains(q) ||
                    (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)) ||
                    a.getType().name().toLowerCase().contains(q)
            ).collect(Collectors.toList());
        }

        boxAlertsFeed.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("No alerts match the selected filter.");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-padding: 24px;");
            boxAlertsFeed.getChildren().add(empty);
            return;
        }

        for (ClinicalAlert alert : list) {
            boxAlertsFeed.getChildren().add(createAlertCard(alert));
        }

        if (MainController.getInstance() != null) {
            MainController.getInstance().updateAlertBadge();
        }
    }

    private VBox createAlertCard(ClinicalAlert alert) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 14px 18px; -fx-background-radius: 10px; " +
                      "-fx-border-color: #E2E8F0; -fx-border-radius: 10px; " +
                      "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.03), 6, 0, 0, 1);");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Icon + Badge
        Label typeBadge = new Label(alert.getType().getDisplayName());
        String bg = "#FEE2E2";
        String fg = "#B91C1C";
        String border = "#FECACA";

        if (alert.getType() == ClinicalAlert.AlertType.SLA_BREACH || alert.getType() == ClinicalAlert.AlertType.ESCALATED) {
            bg = "#FFEDD5";
            fg = "#C2410C";
            border = "#FED7AA";
        } else if (alert.getType() == ClinicalAlert.AlertType.NO_FREE_BAY) {
            bg = "#FEF9C3";
            fg = "#A16207";
            border = "#FEF08A";
        }

        typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + fg + "; " +
                           "-fx-background-color: " + bg + "; -fx-padding: 3px 8px; -fx-background-radius: 999px; " +
                           "-fx-border-color: " + border + "; -fx-border-radius: 999px;");

        Label title = new Label(alert.getTitle());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label("⏱️ " + alert.getFormattedAge());
        time.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B; -fx-font-weight: 600;");

        topRow.getChildren().addAll(typeBadge, title, spacer, time);

        // Description
        Label desc = new Label(alert.getDescription());
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");
        desc.setWrapText(true);

        // Bottom Action / Acknowledgement
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        bottomRow.setStyle("-fx-padding: 4px 0 0 0; -fx-border-color: #F8FAFC; -fx-border-width: 1px 0 0 0;");

        if (!alert.isAcknowledged()) {
            Button btnAck = new Button("✓ Acknowledge Alert");
            btnAck.getStyleClass().add("btn-modern-pill-secondary");
            btnAck.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
            btnAck.setOnAction(e -> {
                String staff = UserSession.getInstance().getStaffName();
                alertService.acknowledgeAlert(alert.getAlertId(), staff);
                renderAlerts();
            });
            bottomRow.getChildren().add(btnAck);
        } else {
            Label ackInfo = new Label("✓ Acknowledged by " + alert.getAcknowledgedBy() + " (" + alert.getFormattedAge() + ")");
            ackInfo.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #15803D;");
            bottomRow.getChildren().add(ackInfo);
        }

        card.getChildren().addAll(topRow, desc, bottomRow);
        return card;
    }
}
