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

        // 1. Critical arrival: Ramesh Kumar
        ClinicalAlert a1 = new ClinicalAlert(ClinicalAlert.AlertType.CRITICAL_ARRIVAL,
                "Critical arrival: Ramesh Kumar",
                "Chest pain, SpO2 88%. Bay 1 assigned.");
        a1.setAlertId(1);
        a1.setTimestamp(LocalDateTime.now().minusMinutes(1));
        alerts.add(a1);

        // 2. Deteriorating vitals: Meera Nair
        ClinicalAlert a2 = new ClinicalAlert(ClinicalAlert.AlertType.DETERIORATING_VITALS,
                "Deteriorating vitals: Meera Nair",
                "NEWS2 rose from 5 to 8 in 10 minutes.");
        a2.setAlertId(2);
        a2.setTimestamp(LocalDateTime.now().minusMinutes(4));
        alerts.add(a2);

        // 3. SLA breach: Harish Patel
        ClinicalAlert a3 = new ClinicalAlert(ClinicalAlert.AlertType.SLA_BREACH,
                "SLA breach: Harish Patel",
                "High priority, waiting 18m against a 15m target.");
        a3.setAlertId(3);
        a3.setTimestamp(LocalDateTime.now().minusMinutes(6));
        alerts.add(a3);

        // 4. Escalated: Kavita Sundaram
        ClinicalAlert a4 = new ClinicalAlert(ClinicalAlert.AlertType.ESCALATED,
                "Escalated: Kavita Sundaram",
                "Medium promoted to High after 45 minutes waiting.");
        a4.setAlertId(4);
        a4.setTimestamp(LocalDateTime.now().minusMinutes(12));
        alerts.add(a4);

        // 5. No free resus bay (Already acknowledged)
        ClinicalAlert a5 = new ClinicalAlert(ClinicalAlert.AlertType.NO_FREE_BAY,
                "No free resus bay",
                "Acknowledged by Charge nurse Anita at 14:02.");
        a5.setAlertId(5);
        a5.setTimestamp(LocalDateTime.now().minusMinutes(28));
        a5.setAcknowledged(true);
        a5.setAcknowledgedBy("Charge nurse Anita");
        a5.setAcknowledgedAt(LocalDateTime.now().minusMinutes(25));
        alerts.add(a5);
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
