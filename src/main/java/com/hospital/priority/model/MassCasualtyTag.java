package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mass Casualty Incident (MCI) triage record (Page 17).
 * Follows the Simple Triage and Rapid Treatment (START) clinical disaster protocol.
 */
public class MassCasualtyTag implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum StartTag {
        RED("Red", "immediate", "#B91C1C", "#FEE2E2", EmergencyLevel.CRITICAL),
        YELLOW("Yellow", "delayed", "#A16207", "#FEF9C3", EmergencyLevel.HIGH),
        GREEN("Green", "minor", "#15803D", "#DCFCE7", EmergencyLevel.LOW),
        BLACK("Black", "deceased", "#0F172A", "#F1F5F9", EmergencyLevel.LOW);

        private final String name;
        private final String subtitle;
        private final String textHex;
        private final String bgHex;
        private final EmergencyLevel mappedLevel;

        StartTag(String name, String subtitle, String textHex, String bgHex, EmergencyLevel mappedLevel) {
            this.name = name;
            this.subtitle = subtitle;
            this.textHex = textHex;
            this.bgHex = bgHex;
            this.mappedLevel = mappedLevel;
        }

        public String getName() {
            return name;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getTextHex() {
            return textHex;
        }

        public String getBgHex() {
            return bgHex;
        }

        public EmergencyLevel getMappedLevel() {
            return mappedLevel;
        }
    }

    private int tagId;
    private StartTag tag;
    private String patientIdentifier; // e.g. "Unknown male, about 40" or "Priyanka D., 27"
    private String location;          // e.g. "Bay 1 · Resus", "Corridor A", "Waiting hall"
    private LocalDateTime taggedAt;

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public MassCasualtyTag() {
        this.taggedAt = LocalDateTime.now();
    }

    public MassCasualtyTag(StartTag tag, String patientIdentifier, String location) {
        this.tag = tag;
        this.patientIdentifier = patientIdentifier;
        this.location = location;
        this.taggedAt = LocalDateTime.now();
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public StartTag getTag() {
        return tag != null ? tag : StartTag.GREEN;
    }

    public void setTag(StartTag tag) {
        this.tag = tag;
    }

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTaggedAt() {
        return taggedAt;
    }

    public void setTaggedAt(LocalDateTime taggedAt) {
        this.taggedAt = taggedAt;
    }

    public String getFormattedTime() {
        if (taggedAt == null) return "";
        return taggedAt.format(TIME_FMT);
    }
}
