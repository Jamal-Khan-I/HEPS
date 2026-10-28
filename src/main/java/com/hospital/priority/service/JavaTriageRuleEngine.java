package com.hospital.priority.service;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * HEPS 2.0 CORE SAFETY COMPONENT: DETERMINISTIC JAVA TRIAGE RULE ENGINE
 * ============================================================================
 * Implements strict, objective clinical triage rules based on physiological
 * thresholds (Emergency Severity Index / Manchester Triage Protocol).
 *
 * KEY SAFETY PRINCIPLE:
 * This engine acts as the authoritative safety guardrail. Even if an AI multi-agent
 * model suggests a mild priority, critical vital deviations (e.g., SpO₂ < 90%)
 * trigger an automatic forced escalation to CRITICAL in Java code!
 */
public class JavaTriageRuleEngine {

    private static JavaTriageRuleEngine instance;

    private JavaTriageRuleEngine() {
    }

    public static synchronized JavaTriageRuleEngine getInstance() {
        if (instance == null) {
            instance = new JavaTriageRuleEngine();
        }
        return instance;
    }

    public static class RuleEvaluation {
        private final EmergencyLevel recommendedLevel;
        private final Specialty recommendedSpecialty;
        private final List<String> triggeredRules;
        private final boolean criticalVitalTriggered;

        public RuleEvaluation(EmergencyLevel recommendedLevel, Specialty recommendedSpecialty,
                              List<String> triggeredRules, boolean criticalVitalTriggered) {
            this.recommendedLevel = recommendedLevel;
            this.recommendedSpecialty = recommendedSpecialty;
            this.triggeredRules = triggeredRules;
            this.criticalVitalTriggered = criticalVitalTriggered;
        }

        public EmergencyLevel getRecommendedLevel() {
            return recommendedLevel;
        }

        public Specialty getRecommendedSpecialty() {
            return recommendedSpecialty;
        }

        public List<String> getTriggeredRules() {
            return triggeredRules;
        }

        public boolean isCriticalVitalTriggered() {
            return criticalVitalTriggered;
        }

        @Override
        public String toString() {
            return "RuleEvaluation [Level=" + recommendedLevel + ", Specialty=" + recommendedSpecialty
                    + ", Critical=" + criticalVitalTriggered + ", Triggers=" + triggeredRules + "]";
        }
    }

    /**
     * Evaluates a patient's vitals, symptoms, and demographics against deterministic medical rules.
     */
    public RuleEvaluation evaluate(Patient patient) {
        if (patient == null) {
            return new RuleEvaluation(EmergencyLevel.LOW, Specialty.GENERAL_EMERGENCY, List.of("Default fallback: empty patient"), false);
        }
        return evaluate(patient.getAge(), patient.getSymptoms(), patient.getVitals());
    }

    public RuleEvaluation evaluate(int age, String symptoms, Vitals vitals) {
        List<String> triggers = new ArrayList<>();
        boolean criticalVital = false;
        EmergencyLevel level = EmergencyLevel.LOW;

        String symClean = symptoms != null ? symptoms.toLowerCase() : "";

        // 1. EVALUATE CRITICAL VITAL SIGNS (Red Line Clinical Thresholds)
        if (vitals != null) {
            if (vitals.getSpo2() != null && vitals.getSpo2() < 90) {
                triggers.add("CRITICAL: Severe Hypoxemia (SpO₂ " + vitals.getSpo2() + "% < 90%)");
                criticalVital = true;
            }
            if (vitals.getHeartRate() != null) {
                if (vitals.getHeartRate() < 40) {
                    triggers.add("CRITICAL: Severe Bradycardia (Heart Rate " + vitals.getHeartRate() + " bpm < 40)");
                    criticalVital = true;
                } else if (vitals.getHeartRate() > 140) {
                    triggers.add("CRITICAL: Extreme Tachycardia (Heart Rate " + vitals.getHeartRate() + " bpm > 140)");
                    criticalVital = true;
                }
            }
            if (vitals.getSystolicBp() != null) {
                if (vitals.getSystolicBp() < 80) {
                    triggers.add("CRITICAL: Hypotensive Shock (Systolic BP " + vitals.getSystolicBp() + " mmHg < 80)");
                    criticalVital = true;
                } else if (vitals.getSystolicBp() > 200) {
                    triggers.add("CRITICAL: Hypertensive Crisis (Systolic BP " + vitals.getSystolicBp() + " mmHg > 200)");
                    criticalVital = true;
                }
            }
            if (vitals.getRespiratoryRate() != null) {
                if (vitals.getRespiratoryRate() < 8 || vitals.getRespiratoryRate() > 32) {
                    triggers.add("CRITICAL: Extreme Respiratory Rate (" + vitals.getRespiratoryRate() + " breaths/min)");
                    criticalVital = true;
                }
            }
            if (vitals.getTemperature() != null && vitals.getTemperature() >= 104.0) {
                triggers.add("CRITICAL: Hyperpyrexia (Temperature " + vitals.getTemperature() + "°F >= 104°F)");
                criticalVital = true;
            }
        }

        // Critical symptoms trigger
        if (symClean.contains("cardiac arrest") || symClean.contains("unresponsive")
                || symClean.contains("choking") || symClean.contains("not breathing")
                || symClean.contains("massive bleed") || symClean.contains("gunshot")) {
            triggers.add("CRITICAL: Life-threatening symptom keywords detected");
            criticalVital = true;
        }

        if (criticalVital) {
            level = EmergencyLevel.CRITICAL;
        } else {
            // 2. EVALUATE HIGH URGENCY CONDITIONS
            boolean highTriggered = false;
            if (vitals != null) {
                if (vitals.getSpo2() != null && vitals.getSpo2() >= 90 && vitals.getSpo2() <= 94) {
                    triggers.add("HIGH: Moderate Hypoxia (SpO₂ " + vitals.getSpo2() + "%)");
                    highTriggered = true;
                }
                if (vitals.getHeartRate() != null && vitals.getHeartRate() >= 115 && vitals.getHeartRate() <= 140) {
                    triggers.add("HIGH: Significant Tachycardia (HR " + vitals.getHeartRate() + " bpm)");
                    highTriggered = true;
                }
                if (vitals.getSystolicBp() != null && vitals.getSystolicBp() >= 180 && vitals.getSystolicBp() <= 200) {
                    triggers.add("HIGH: Severe Hypertension (Systolic BP " + vitals.getSystolicBp() + " mmHg)");
                    highTriggered = true;
                }
                if (vitals.getTemperature() != null && vitals.getTemperature() >= 102.5 && vitals.getTemperature() < 104.0) {
                    triggers.add("HIGH: High Fever (Temperature " + vitals.getTemperature() + "°F)");
                    highTriggered = true;
                }
            }

            if (symClean.contains("chest pain") || symClean.contains("heart attack")
                    || symClean.contains("stroke") || symClean.contains("paralysis")
                    || symClean.contains("severe asthma") || symClean.contains("breathless")
                    || symClean.contains("respiratory distress") || symClean.contains("open fracture")
                    || symClean.contains("uncontrolled bleed") || symClean.contains("bleeding")
                    || symClean.contains("head injury")) {
                triggers.add("HIGH: Acute severe clinical presentation");
                highTriggered = true;
            }

            if (highTriggered) {
                level = EmergencyLevel.HIGH;
            } else {
                // 3. EVALUATE MEDIUM URGENCY
                boolean mediumTriggered = false;
                if (vitals != null && vitals.hasAbnormalVitals()) {
                    triggers.add("MEDIUM: Borderline abnormal vitals");
                    mediumTriggered = true;
                }
                if (symClean.contains("fracture") || symClean.contains("deep cut")
                        || symClean.contains("stitches") || symClean.contains("vomiting")
                        || symClean.contains("moderate pain") || symClean.contains("sprain")
                        || symClean.contains("fever") || symClean.contains("burn")) {
                    triggers.add("MEDIUM: Moderate distress requiring intervention");
                    mediumTriggered = true;
                }

                if (mediumTriggered) {
                    level = EmergencyLevel.MEDIUM;
                } else {
                    triggers.add("LOW: Vitals stable and minor non-urgent complaint");
                    level = EmergencyLevel.LOW;
                }
            }
        }

        // 4. EVALUATE SPECIALTY RECOMMENDATION
        Specialty specialty = evaluateSpecialty(age, symClean, vitals);

        return new RuleEvaluation(level, specialty, triggers, criticalVital);
    }

    private Specialty evaluateSpecialty(int age, String symptoms, Vitals vitals) {
        if (age <= 14) {
            return Specialty.PEDIATRICS;
        }

        if (symptoms.contains("chest pain") || symptoms.contains("cardiac") || symptoms.contains("heart")
                || symptoms.contains("palpitation") || symptoms.contains("angina")) {
            return Specialty.CARDIOLOGY;
        }

        if (symptoms.contains("stroke") || symptoms.contains("numbness") || symptoms.contains("paralysis")
                || symptoms.contains("seizure") || symptoms.contains("slurred speech") || symptoms.contains("headache")
                || symptoms.contains("unconscious") || symptoms.contains("fainting")) {
            return Specialty.NEUROLOGY;
        }

        if (symptoms.contains("asthma") || symptoms.contains("breath") || symptoms.contains("cough")
                || symptoms.contains("wheez") || symptoms.contains("lungs") || symptoms.contains("hypoxia")
                || (vitals != null && vitals.getSpo2() != null && vitals.getSpo2() < 94)) {
            return Specialty.PULMONOLOGY;
        }

        if (symptoms.contains("fracture") || symptoms.contains("bone") || symptoms.contains("dislocation")
                || symptoms.contains("sprain") || symptoms.contains("knee") || symptoms.contains("joint")
                || symptoms.contains("ankle") || symptoms.contains("wrist")) {
            return Specialty.ORTHOPEDICS;
        }

        if (symptoms.contains("gunshot") || symptoms.contains("stab") || symptoms.contains("accident")
                || symptoms.contains("collision") || symptoms.contains("heavy bleed") || symptoms.contains("trauma")) {
            return Specialty.TRAUMA_SURGERY;
        }

        return Specialty.GENERAL_EMERGENCY;
    }
}
