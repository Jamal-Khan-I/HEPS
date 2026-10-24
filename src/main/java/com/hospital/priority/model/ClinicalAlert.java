package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Real-time clinical alert entity (Page 14).
 * Types: Critical arrival, SLA breach, Escalation, Deteriorating vitals, No free bay, Abnormal result.
 * Named ClinicalAlert to prevent ambiguity with JavaFX's UI modal javafx.scene.control.Alert.
 */
public class ClinicalAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum AlertType {
        CRITICAL_ARRIVAL("Critical arrival", "#B91C1C", "#FEE2E2"),
        DETERIORATING_VITALS("Deteriorating vitals", "#C2410C", "#FFEDD5"),
        SLA_BREACH("SLA breach", "#C2410C", "#FFEDD5"),
        ESCALATED("Escalated", "#A16207", "#FEF9C3"),
        NO_FREE_BAY("No free resus bay", "#64748B", "#F1F5F9"),
        ABNORMAL_RESULT("Abnormal result", "#B91C1C", "#FEE2E2");

        private final String displayName;
        private final String borderHex;
        private final String bgHex;

        AlertType(String displayName, String borderHex, String bgHex) {
            this.displayName = displayName;
            this.borderHex = borderHex;
            this.bgHex = bgHex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBorderHex() {
            return borderHex;
        }

        public String getBgHex() {
            return bgHex;
        }
    }

    private int alertId;
    private AlertType type;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;

    public ClinicalAlert() {
        this.timestamp = LocalDateTime.now();
        this.acknowledged = false;
    }

    public ClinicalAlert(AlertType type, String title, String description) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.acknowledged = false;
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public AlertType getType() {
        return type != null ? type : AlertType.CRITICAL_ARRIVAL;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getTimeAgo() {
        if (timestamp == null) return "just now";
        long mins = java.time.Duration.between(timestamp, LocalDateTime.now()).toMinutes();
        if (mins <= 1) return "1 min ago";
        if (mins < 60) return mins + " min ago";
        long hours = mins / 60;
        return hours + "h ago";
    }

    public String getFormattedAge() {
        return getTimeAgo();
    }

    public void acknowledge(String staffName) {
        this.acknowledged = true;
        this.acknowledgedBy = staffName;
        this.acknowledgedAt = LocalDateTime.now();
    }
}
