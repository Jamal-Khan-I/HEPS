package com.hospital.priority.service;

import com.hospital.priority.model.ClinicalAlert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * REAL-TIME CLINICAL ALERT SERVICE (Page 14)
 * ============================================================================
 * Dispatches and monitors alerts for critical arrivals, deteriorating vitals,
 * SLA breaches, wait-time escalations, and critical abnormal lab results.
 */
public class AlertService {

    private static AlertService instance;

    private final List<ClinicalAlert> alerts = new CopyOnWriteArrayList<>();

    private AlertService() {
        seedInitialAlerts();
    }

    public static synchronized AlertService getInstance() {
        if (instance == null) {
            instance = new AlertService();
        }
        return instance;
    }

    private void seedInitialAlerts() {
        alerts.clear();
    }

    public List<ClinicalAlert> getAllAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    public List<ClinicalAlert> getActiveAlerts() {
        List<ClinicalAlert> list = new ArrayList<>();
        for (ClinicalAlert a : alerts) {
            if (!a.isAcknowledged()) {
                list.add(a);
            }
        }
        return list;
    }

    public List<ClinicalAlert> getAcknowledgedAlerts() {
        List<ClinicalAlert> list = new ArrayList<>();
        for (ClinicalAlert a : alerts) {
            if (a.isAcknowledged()) {
                list.add(a);
            }
        }
        return list;
    }

    public int getUnreadCount() {
        return getActiveAlerts().size();
    }

    public synchronized void triggerAlert(ClinicalAlert.AlertType type, String title, String description) {
        ClinicalAlert a = new ClinicalAlert(type, title, description);
        a.setAlertId(alerts.size() + 1);
        alerts.add(0, a);
    }

    public synchronized void dispatchAlert(ClinicalAlert.AlertType type, String tokenNo, String patientName, String description) {
        triggerAlert(type, (type != null ? type.getDisplayName() : "Alert") + ": " + patientName, description);
    }

    public synchronized void acknowledgeAlert(int alertId, String staffName) {
        for (ClinicalAlert a : alerts) {
            if (a.getAlertId() == alertId) {
                a.acknowledge(staffName);
                AuditService.getInstance().log("ALERT_ACKNOWLEDGED", null, null, "UNACKNOWLEDGED", "ACKNOWLEDGED",
                        a.getTitle() + " acknowledged by " + staffName);
                break;
            }
        }
    }
}
