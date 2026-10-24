package com.hospital.priority.model;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * ============================================================================
 * HEPS 2.0 ENHANCED DOMAIN ENTITY: PATIENT
 * ============================================================================
 * Core triage entity processed by the Java PriorityQueue and Doctor Routing Engine.
 * Supports structured vitals, AI triage recommendations, deterministic safety flags,
 * specialty assignments, and automatic wait-time priority escalation.
 */
public class Patient {
    private int patientId;
    private String tokenNo;
    private String fullName;
    private int age;
    private String gender;
    private String phone;
    private String symptoms;
    private EmergencyLevel emergencyLevel;
    private LocalDateTime arrivalTime;
    private PatientStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient helper for UI queue display
    private int queuePosition;

    // HEPS 2.0 Upgraded Fields
    private Vitals vitals;
    private Specialty assignedSpecialty;
    private Integer assignedDoctorId;
    private String assignedDoctorName;
    private String assignedBay;
    private EmergencyLevel aiSuggestedLevel;
    private EmergencyLevel ruleSuggestedLevel;
    private String overrideReason;
    private boolean escalated;
    private String escalationReason;

    // HEPS Enterprise ED System Additions (Pages 1-18)
    private String uhid;
    private String dateOfBirth;
    private String address;
    private String bloodGroup;
    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;
    private String arrivalMode = "Walk-in";
    private boolean mlc = false; // Medico-legal case
    private Integer gcs = 15;
    private Integer painScore = 0;
    private Integer news2Score = 0;
    private String allergies = "";
    private String knownConditions = "";
    private LocalDateTime triageTime;
    private LocalDateTime doctorSeenTime;
    private LocalDateTime dispositionTime;
    private String dischargeDiagnosis;
    private String dischargeMedications;
    private String dischargeFollowUpDate;
    private String dischargeAdvice;
    private String dischargeOutcome;

    public static final DateTimeFormatter DISPLAY_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    public Patient() {
        this.status = PatientStatus.WAITING;
        this.arrivalTime = LocalDateTime.now();
        this.vitals = new Vitals();
        this.assignedSpecialty = Specialty.GENERAL_EMERGENCY;
        this.assignedBay = getDefaultBayForLevel(EmergencyLevel.MEDIUM);
        this.escalated = false;
        this.uhid = generateUhidFallback();
    }

    public Patient(int patientId, String tokenNo, String fullName, int age, String gender, String phone,
                   String symptoms, EmergencyLevel emergencyLevel, LocalDateTime arrivalTime,
                   PatientStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.patientId = patientId;
        this.tokenNo = tokenNo;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.symptoms = symptoms;
        this.emergencyLevel = emergencyLevel != null ? emergencyLevel : EmergencyLevel.LOW;
        this.arrivalTime = arrivalTime != null ? arrivalTime : LocalDateTime.now();
        this.status = status != null ? status : PatientStatus.WAITING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.vitals = new Vitals();
        this.assignedSpecialty = Specialty.GENERAL_EMERGENCY;
        this.assignedBay = getDefaultBayForLevel(this.emergencyLevel);
        this.escalated = false;
    }

    public Patient(String tokenNo, String fullName, int age, String gender, String phone,
                   String symptoms, EmergencyLevel emergencyLevel, LocalDateTime arrivalTime) {
        this.tokenNo = tokenNo;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.symptoms = symptoms;
        this.emergencyLevel = emergencyLevel != null ? emergencyLevel : EmergencyLevel.LOW;
        this.arrivalTime = arrivalTime != null ? arrivalTime : LocalDateTime.now();
        this.status = PatientStatus.WAITING;
        this.vitals = new Vitals();
        this.assignedSpecialty = Specialty.GENERAL_EMERGENCY;
        this.assignedBay = getDefaultBayForLevel(this.emergencyLevel);
        this.escalated = false;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getMaskedPhone() {
        return com.hospital.priority.util.ValidationUtil.maskPhone(phone);
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getFormattedArrivalTime() {
        return arrivalTime != null ? arrivalTime.format(DISPLAY_TIME_FMT) : "N/A";
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    // HEPS 2.0 Getters & Setters

    public Vitals getVitals() {
        return vitals;
    }

    public void setVitals(Vitals vitals) {
        this.vitals = vitals != null ? vitals : new Vitals();
    }

    public Specialty getAssignedSpecialty() {
        return assignedSpecialty != null ? assignedSpecialty : Specialty.GENERAL_EMERGENCY;
    }

    public void setAssignedSpecialty(Specialty assignedSpecialty) {
        this.assignedSpecialty = assignedSpecialty;
    }

    public Integer getAssignedDoctorId() {
        return assignedDoctorId;
    }

    public void setAssignedDoctorId(Integer assignedDoctorId) {
        this.assignedDoctorId = assignedDoctorId;
    }

    public String getAssignedDoctorName() {
        return assignedDoctorName != null && !assignedDoctorName.trim().isEmpty()
                ? assignedDoctorName
                : "Unassigned";
    }

    public void setAssignedDoctorName(String assignedDoctorName) {
        this.assignedDoctorName = assignedDoctorName;
    }

    public String getAssignedBay() {
        if (assignedBay == null || assignedBay.trim().isEmpty()) {
            return getDefaultBayForLevel(emergencyLevel);
        }
        return assignedBay;
    }

    public void setAssignedBay(String assignedBay) {
        this.assignedBay = assignedBay;
    }

    public static String getDefaultBayForLevel(EmergencyLevel level) {
        if (level == null) return "Bay 4 [Observation - Yellow]";
        return switch (level) {
            case CRITICAL -> "Bay 1 [Resuscitation - Red]";
            case HIGH -> "Bay 3 [Acute Care - Yellow]";
            case MEDIUM -> "Bay 4 [Observation - Yellow]";
            case LOW -> "Bay 6 [Ambulatory - Green]";
        };
    }

    public EmergencyLevel getAiSuggestedLevel() {
        return aiSuggestedLevel;
    }

    public void setAiSuggestedLevel(EmergencyLevel aiSuggestedLevel) {
        this.aiSuggestedLevel = aiSuggestedLevel;
    }

    public EmergencyLevel getRuleSuggestedLevel() {
        return ruleSuggestedLevel;
    }

    public void setRuleSuggestedLevel(EmergencyLevel ruleSuggestedLevel) {
        this.ruleSuggestedLevel = ruleSuggestedLevel;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    private static Clock clock = Clock.systemDefaultZone();

    public static void setClock(Clock c) {
        clock = (c != null) ? c : Clock.systemDefaultZone();
    }

    public static Clock getClock() {
        return clock;
    }

    /**
     * Calculates the patient's waiting duration in minutes.
     */
    public long getWaitingMinutes() {
        if (arrivalTime == null) return 0;
        return Math.max(0, Duration.between(arrivalTime, LocalDateTime.now(clock)).toMinutes());
    }

    public String getFormattedWaitingDuration() {
        long mins = getWaitingMinutes();
        if (mins < 60) {
            return mins + "m";
        }
        long hours = mins / 60;
        long remMins = mins % 60;
        return String.format(Locale.ENGLISH, "%dh %02dm", hours, remMins);
    }

    public String getFormattedWaitTime() {
        return getFormattedWaitingDuration();
    }

    public String getShortBay() {
        if (assignedBay == null || assignedBay.trim().isEmpty()) {
            return "ER";
        }
        String clean = assignedBay.replace("Â", "").replace("·", "").replace("•", "").trim();
        if (clean.contains("[")) {
            // e.g. "Bay 1 [Resuscitation - Red]" -> "Bay 1 Resus"
            int bracketIdx = clean.indexOf('[');
            String prefix = clean.substring(0, bracketIdx).trim();
            String inside = clean.substring(bracketIdx + 1).replace("]", "").trim();
            if (inside.toLowerCase().contains("resus")) return prefix + " Resus";
            if (inside.toLowerCase().contains("trauma")) return prefix + " Trauma";
            if (inside.toLowerCase().contains("acute")) return prefix + " Acute";
            if (inside.toLowerCase().contains("obs")) return prefix + " Obs";
            if (inside.toLowerCase().contains("fast")) return prefix + " Fast track";
            return prefix;
        }
        return clean.replaceAll("\\s+", " ").trim();
    }

    public String getFormattedTimeInTreatment() {
        long mins = getTimeInTreatmentMinutes();
        if (mins < 60) {
            return mins + "m";
        }
        long hours = mins / 60;
        long remMins = mins % 60;
        return String.format(Locale.ENGLISH, "%dh %02dm", hours, remMins);
    }

    public String getUhid() {
        if (uhid == null || uhid.isEmpty()) {
            uhid = generateUhidFallback();
        }
        return uhid;
    }

    public void setUhid(String uhid) {
        this.uhid = uhid;
    }

    private String generateUhidFallback() {
        int year = arrivalTime != null ? arrivalTime.getYear() : LocalDateTime.now().getYear();
        int hash = Math.abs((tokenNo != null ? tokenNo.hashCode() : (int)(System.currentTimeMillis() % 100000)));
        return String.format("UH-%d-%06d", year, hash % 1000000);
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBloodGroup() {
        return bloodGroup != null ? bloodGroup : "Unknown";
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactRelation() {
        return emergencyContactRelation;
    }

    public void setEmergencyContactRelation(String emergencyContactRelation) {
        this.emergencyContactRelation = emergencyContactRelation;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getArrivalMode() {
        return arrivalMode != null ? arrivalMode : "Walk-in";
    }

    public void setArrivalMode(String arrivalMode) {
        this.arrivalMode = arrivalMode;
    }

    public boolean isMlc() {
        return mlc;
    }

    public void setMlc(boolean mlc) {
        this.mlc = mlc;
    }

    public Integer getGcs() {
        return gcs != null ? gcs : 15;
    }

    public void setGcs(Integer gcs) {
        this.gcs = gcs;
    }

    public Integer getPainScore() {
        return painScore != null ? painScore : 0;
    }

    public void setPainScore(Integer painScore) {
        this.painScore = painScore;
    }

    public Integer getNews2Score() {
        return news2Score != null ? news2Score : 0;
    }

    public void setNews2Score(Integer news2Score) {
        this.news2Score = news2Score;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public boolean hasAllergies() {
        return allergies != null && !allergies.trim().isEmpty() && !allergies.equalsIgnoreCase("None") && !allergies.equalsIgnoreCase("Nil");
    }

    public String getKnownConditions() {
        return knownConditions;
    }

    public void setKnownConditions(String knownConditions) {
        this.knownConditions = knownConditions;
    }

    public LocalDateTime getTriageTime() {
        return triageTime;
    }

    public void setTriageTime(LocalDateTime triageTime) {
        this.triageTime = triageTime;
    }

    public LocalDateTime getDoctorSeenTime() {
        return doctorSeenTime;
    }

    public void setDoctorSeenTime(LocalDateTime doctorSeenTime) {
        this.doctorSeenTime = doctorSeenTime;
    }

    public LocalDateTime getDispositionTime() {
        return dispositionTime;
    }

    public void setDispositionTime(LocalDateTime dispositionTime) {
        this.dispositionTime = dispositionTime;
    }

    public String getDischargeDiagnosis() {
        return dischargeDiagnosis;
    }

    public void setDischargeDiagnosis(String dischargeDiagnosis) {
        this.dischargeDiagnosis = dischargeDiagnosis;
    }

    public String getDischargeMedications() {
        return dischargeMedications;
    }

    public void setDischargeMedications(String dischargeMedications) {
        this.dischargeMedications = dischargeMedications;
    }

    public String getDischargeFollowUpDate() {
        return dischargeFollowUpDate;
    }

    public void setDischargeFollowUpDate(String dischargeFollowUpDate) {
        this.dischargeFollowUpDate = dischargeFollowUpDate;
    }

    public String getDischargeAdvice() {
        return dischargeAdvice;
    }

    public void setDischargeAdvice(String dischargeAdvice) {
        this.dischargeAdvice = dischargeAdvice;
    }

    public String getDischargeOutcome() {
        return dischargeOutcome;
    }

    public void setDischargeOutcome(String dischargeOutcome) {
        this.dischargeOutcome = dischargeOutcome;
    }


    public int getSlaMinutes() {
        if (emergencyLevel == null) return 60;
        return switch (emergencyLevel) {
            case CRITICAL -> 0;
            case HIGH -> 15;
            case MEDIUM -> 60;
            case LOW -> 120;
        };
    }

    public String getSlaTargetText() {
        if (emergencyLevel == EmergencyLevel.CRITICAL) {
            return "immediate";
        }
        return getSlaMinutes() + "m";
    }

    public boolean isSlaBreached() {
        int target = getSlaMinutes();
        if (target == 0) {
            return getWaitingMinutes() > 2; // Critical should be immediate
        }
        return getWaitingMinutes() > target;
    }

    public double getSlaRatio() {
        int target = getSlaMinutes();
        if (target == 0) return 1.0;
        return (double) getWaitingMinutes() / target;
    }

    public String getSlaColorHex() {
        if (isSlaBreached()) {
            return "#EF4444"; // Red breached
        } else if (getSlaRatio() >= 0.75) {
            return "#F59E0B"; // Amber warning
        } else {
            return "#10B981"; // Green within SLA
        }
    }

    public long getTimeInTreatmentMinutes() {
        LocalDateTime start = doctorSeenTime != null ? doctorSeenTime : arrivalTime;
        if (start == null) return 0;
        return Math.max(0, Duration.between(start, LocalDateTime.now(clock)).toMinutes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Patient patient = (Patient) o;
        return Objects.equals(tokenNo, patient.tokenNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenNo);
    }

    @Override
    public String toString() {
        return "[" + tokenNo + "] " + fullName + " (" + emergencyLevel + ", "
                + (assignedSpecialty != null ? assignedSpecialty.getDisplayName() : "General") + ")";
    }
}
