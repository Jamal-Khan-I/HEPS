package com.hospital.priority.service.ai;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Vitals;

/**
 * ============================================================================
 * HEPS 2.0 AI AGENT 2: TRIAGE AGENT
 * ============================================================================
 * Role: Emergency Urgency Categorization based on clinical severity standards.
 */
public class TriageAgent {

    public static class TriageRecommendation {
        private final EmergencyLevel level;
        private final double confidence;
        private final String rationale;

        public TriageRecommendation(EmergencyLevel level, double confidence, String rationale) {
            this.level = level;
            this.confidence = confidence;
            this.rationale = rationale;
        }

        public EmergencyLevel getLevel() {
            return level;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getRationale() {
            return rationale;
        }

        @Override
        public String toString() {
            return level + " (Conf: " + (int)(confidence * 100) + "%) - " + rationale;
        }
    }

    public TriageRecommendation assess(int age, String symptoms, Vitals vitals) {
        String s = symptoms != null ? symptoms.toLowerCase() : "";

        // Immediate Life Threat Indicators
        if (s.contains("cardiac arrest") || s.contains("unconscious") || s.contains("unresponsive")
                || s.contains("severe anaphylaxis") || s.contains("massive bleeding")
                || (vitals != null && vitals.getSpo2() != null && vitals.getSpo2() < 88)) {
            return new TriageRecommendation(EmergencyLevel.CRITICAL, 0.98,
                    "Immediate life-threat detected: Acute physiological compromise requiring instant intervention.");
        }

        // Acute Severe Distress
        if (s.contains("chest") || s.contains("angina") || s.contains("heart attack") || s.contains("stroke")
                || s.contains("seizure") || s.contains("asthma") || s.contains("breathless") || s.contains("wheezing")
                || s.contains("bleeding") || s.contains("fracture")
                || s.contains("paralysis") || (vitals != null && vitals.getSpo2() != null && vitals.getSpo2() <= 93)) {
            return new TriageRecommendation(EmergencyLevel.HIGH, 0.91,
                    "High clinical urgency: High risk of rapid organ deterioration without priority attention.");
        }

        // Moderate Distress
        if (s.contains("deep cut") || s.contains("stitches") || s.contains("vomiting")
                || s.contains("moderate pain") || s.contains("sprain") || s.contains("fever")
                || (vitals != null && vitals.hasAbnormalVitals())) {
            return new TriageRecommendation(EmergencyLevel.MEDIUM, 0.85,
                    "Moderate distress: Patient condition stable but requires prompt evaluation within standard window.");
        }

        // Low / Non-urgent
        return new TriageRecommendation(EmergencyLevel.LOW, 0.92,
                "Non-urgent: Vital signs stable, superficial complaints, safe for routine emergency waiting queue.");
    }
}
