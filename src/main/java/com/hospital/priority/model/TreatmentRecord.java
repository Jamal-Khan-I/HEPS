package com.hospital.priority.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing the medical consultation and notes recorded by a doctor.
 */
public class TreatmentRecord {
    private int recordId;
    private int patientId;
    private String doctorName;
    private String treatmentNotes;
    private String disposition;
    private LocalDateTime admittedAt;
    private LocalDateTime completedAt;

    public TreatmentRecord() {
        this.disposition = "DISCHARGED_HOME";
    }

    public TreatmentRecord(int recordId, int patientId, String doctorName, String treatmentNotes,
                           String disposition, LocalDateTime admittedAt, LocalDateTime completedAt) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.doctorName = doctorName;
        this.treatmentNotes = treatmentNotes;
        this.disposition = disposition != null ? disposition : "DISCHARGED_HOME";
        this.admittedAt = admittedAt;
        this.completedAt = completedAt;
    }

    public TreatmentRecord(int recordId, int patientId, String doctorName, String treatmentNotes,
                           LocalDateTime admittedAt, LocalDateTime completedAt) {
        this(recordId, patientId, doctorName, treatmentNotes, "DISCHARGED_HOME", admittedAt, completedAt);
    }

    public TreatmentRecord(int patientId, String doctorName, String treatmentNotes, LocalDateTime admittedAt) {
        this(0, patientId, doctorName, treatmentNotes, "DISCHARGED_HOME", admittedAt, null);
    }

    public TreatmentRecord(int patientId, String doctorName, String treatmentNotes, String disposition, LocalDateTime admittedAt) {
        this(0, patientId, doctorName, treatmentNotes, disposition, admittedAt, null);
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getTreatmentNotes() {
        return treatmentNotes;
    }

    public void setTreatmentNotes(String treatmentNotes) {
        this.treatmentNotes = treatmentNotes;
    }

    public LocalDateTime getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(LocalDateTime admittedAt) {
        this.admittedAt = admittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getDisposition() {
        return disposition != null ? disposition : "DISCHARGED_HOME";
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getFormattedDisposition() {
        if (disposition == null) return "✓ Discharged Home (Stable)";
        return switch (disposition.toUpperCase()) {
            case "ADMITTED_INPATIENT", "INPATIENT" -> "🏥 Admitted to Inpatient Ward";
            case "TRANSFERRED_ICU", "ICU", "OT" -> "🚨 Transferred to ICU/OT";
            default -> "✓ Discharged Home (Stable)";
        };
    }

    @Override
    public String toString() {
        return "TreatmentRecord{" +
                "recordId=" + recordId +
                ", patientId=" + patientId +
                ", doctorName='" + doctorName + '\'' +
                ", admittedAt=" + admittedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
