package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured clinical SOAP note (Subjective, Objective, Assessment, Plan) - Page 10.
 * Timestamped and digitally signed by the attending doctor.
 */
public class ClinicalNote implements Serializable {

    private static final long serialVersionUID = 1L;

    private int noteId;
    private int patientId;
    private String tokenNo;
    private String doctorName;
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    private LocalDateTime createdAt;

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public ClinicalNote() {
        this.createdAt = LocalDateTime.now();
    }

    public ClinicalNote(int patientId, String tokenNo, String doctorName,
                        String subjective, String objective, String assessment, String plan) {
        this.patientId = patientId;
        this.tokenNo = tokenNo;
        this.doctorName = doctorName;
        this.subjective = subjective;
        this.objective = objective;
        this.assessment = assessment;
        this.plan = plan;
        this.createdAt = LocalDateTime.now();
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(String tokenNo) {
        this.tokenNo = tokenNo;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSubjective() {
        return subjective;
    }

    public void setSubjective(String subjective) {
        this.subjective = subjective;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getAssessment() {
        return assessment;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedTimestamp() {
        if (createdAt == null) return "";
        return createdAt.format(TIME_FMT);
    }
}
