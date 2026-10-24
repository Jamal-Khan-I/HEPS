package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a physical emergency department bay or bed resource (Page 9).
 * Capacity is strictly enforced (no double booking).
 */
public class Bay implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum BayStatus {
        FREE("Free", "#10B981"),
        OCCUPIED("Occupied", "#EF4444"),
        CLEANING("Cleaning", "#F59E0B"),
        BLOCKED("Blocked", "#64748B");

        private final String displayName;
        private final String colorHex;

        BayStatus(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorHex() {
            return colorHex;
        }

        public String getStyle() {
            switch (this) {
                case FREE: return "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-border-color: #BBF7D0;";
                case OCCUPIED: return "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA;";
                case CLEANING: return "-fx-background-color: #FEF9C3; -fx-text-fill: #A16207; -fx-border-color: #FEF08A;";
                default: return "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-border-color: #E2E8F0;";
            }
        }
    }

    public enum BayType {
        RESUS("Resus", EmergencyLevel.CRITICAL),
        TRAUMA("Trauma", EmergencyLevel.CRITICAL),
        ACUTE("Acute", EmergencyLevel.HIGH),
        OBSERVATION("Obs", EmergencyLevel.MEDIUM),
        FAST_TRACK("Fast track", EmergencyLevel.LOW),
        AMBULATORY("Ambulatory", EmergencyLevel.LOW);

        private final String displayName;
        private final EmergencyLevel recommendedLevel;

        BayType(String displayName, EmergencyLevel recommendedLevel) {
            this.displayName = displayName;
            this.recommendedLevel = recommendedLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public EmergencyLevel getRecommendedLevel() {
            return recommendedLevel;
        }
    }

    private int bayId;
    private String name;
    private BayType type;
    private BayStatus status;
    private Integer currentPatientId;
    private String currentPatientToken;
    private String currentPatientName;
    private EmergencyLevel currentPriority;
    private String assignedDoctorName;
    private LocalDateTime occupiedSince;
    private LocalDateTime cleaningUntil;

    public Bay() {
        this.status = BayStatus.FREE;
        this.type = BayType.OBSERVATION;
    }

    public Bay(int bayId, String name, BayType type, BayStatus status) {
        this.bayId = bayId;
        this.name = name;
        this.type = type != null ? type : BayType.OBSERVATION;
        this.status = status != null ? status : BayStatus.FREE;
    }

    public int getBayId() {
        return bayId;
    }

    public void setBayId(int bayId) {
        this.bayId = bayId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BayType getType() {
        return type;
    }

    public void setType(BayType type) {
        this.type = type;
    }

    public BayStatus getStatus() {
        return status;
    }

    public void setStatus(BayStatus status) {
        this.status = status;
    }

    public Integer getCurrentPatientId() {
        return currentPatientId;
    }

    public void setCurrentPatientId(Integer currentPatientId) {
        this.currentPatientId = currentPatientId;
    }

    public String getCurrentPatientToken() {
        return currentPatientToken;
    }

    public void setCurrentPatientToken(String currentPatientToken) {
        this.currentPatientToken = currentPatientToken;
    }

    public String getCurrentPatientName() {
        return currentPatientName;
    }

    public void setCurrentPatientName(String currentPatientName) {
        this.currentPatientName = currentPatientName;
    }

    public EmergencyLevel getCurrentPriority() {
        return currentPriority;
    }

    public void setCurrentPriority(EmergencyLevel currentPriority) {
        this.currentPriority = currentPriority;
    }

    public String getAssignedDoctorName() {
        return assignedDoctorName != null ? assignedDoctorName : "Unassigned";
    }

    public void setAssignedDoctorName(String assignedDoctorName) {
        this.assignedDoctorName = assignedDoctorName;
    }

    public LocalDateTime getOccupiedSince() {
        return occupiedSince;
    }

    public void setOccupiedSince(LocalDateTime occupiedSince) {
        this.occupiedSince = occupiedSince;
    }

    public LocalDateTime getCleaningUntil() {
        return cleaningUntil;
    }

    public void setCleaningUntil(LocalDateTime cleaningUntil) {
        this.cleaningUntil = cleaningUntil;
    }

    public long getMinutesInBay() {
        if (occupiedSince == null) return 0;
        return java.time.Duration.between(occupiedSince, LocalDateTime.now()).toMinutes();
    }

    public String getFormattedTimeInBay() {
        return getMinutesInBay() + "m in bay";
    }

    public String getInitials() {
        if (currentPatientName == null || currentPatientName.trim().isEmpty()) {
            return "--";
        }
        String[] parts = currentPatientName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public boolean isAvailable() {
        return status == BayStatus.FREE;
    }

    public void occupy(Patient patient) {
        if (patient == null) return;
        this.status = BayStatus.OCCUPIED;
        this.currentPatientId = patient.getPatientId();
        this.currentPatientToken = patient.getTokenNo();
        this.currentPatientName = patient.getFullName();
        this.currentPriority = patient.getEmergencyLevel();
        this.assignedDoctorName = patient.getAssignedDoctorName();
        this.occupiedSince = LocalDateTime.now();
        this.cleaningUntil = null;
    }

    public void releaseToCleaning(int cleaningMinutes) {
        this.status = BayStatus.CLEANING;
        this.currentPatientId = null;
        this.currentPatientToken = null;
        this.currentPatientName = null;
        this.currentPriority = null;
        this.occupiedSince = null;
        this.cleaningUntil = LocalDateTime.now().plusMinutes(cleaningMinutes);
    }

    public void markFree() {
        this.status = BayStatus.FREE;
        this.currentPatientId = null;
        this.currentPatientToken = null;
        this.currentPatientName = null;
        this.currentPriority = null;
        this.assignedDoctorName = null;
        this.occupiedSince = null;
        this.cleaningUntil = null;
    }

    public long getMinutesOccupied() {
        return getMinutesInBay();
    }

    public long getRemainingCleaningMinutes() {
        if (cleaningUntil == null) return 0;
        long rem = java.time.Duration.between(LocalDateTime.now(), cleaningUntil).toMinutes();
        return Math.max(0, rem);
    }

    public void releaseAndClean(int cleaningMinutes) {
        releaseToCleaning(cleaningMinutes);
    }

    public void markCleaned() {
        markFree();
    }

    public void occupyFromBay(Bay other) {
        if (other == null) return;
        this.status = BayStatus.OCCUPIED;
        this.currentPatientId = other.getCurrentPatientId();
        this.currentPatientToken = other.getCurrentPatientToken();
        this.currentPatientName = other.getCurrentPatientName();
        this.currentPriority = other.getCurrentPriority();
        this.assignedDoctorName = other.getAssignedDoctorName();
        this.occupiedSince = other.getOccupiedSince() != null ? other.getOccupiedSince() : LocalDateTime.now();
        this.cleaningUntil = null;
    }

    @Override
    public String toString() {
        return name + " (" + (status == BayStatus.FREE ? "free" : status.getDisplayName().toLowerCase()) + ")";
    }
}
