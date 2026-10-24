package com.hospital.priority.service;

import com.hospital.priority.model.Vitals;

/**
 * ============================================================================
 * NATIONAL EARLY WARNING SCORE 2 (NEWS2) CALCULATION ENGINE
 * ============================================================================
 * Standard UK Royal College of Physicians clinical early warning scoring system.
 * Used across modern Emergency Departments to quantify patient physiological acuity
 * and risk of clinical deterioration.
 *
 * Scoring breakdown:
 * - 0 - 4: Low clinical risk (Ward / Fast-track monitoring)
 * - 5 - 6: Medium clinical risk (Urgent response / Observation)
 * - 7+: High / Critical clinical risk (Emergency resuscitation / Immediate clinician response)
 */
public class NEWS2Service {

    /**
     * Calculates the NEWS2 aggregate score from structured physiological vitals and GCS.
     *
     * @param vitals Structured physiological vitals (SpO2, HR, BP, RR, Temp)
     * @param gcs Glasgow Coma Scale (3-15, default 15 = Alert)
     * @return Aggregate NEWS2 score (0 to 20+)
     */
    public static int calculateScore(Vitals vitals, Integer gcs) {
        if (vitals == null) {
            return 0;
        }

        int score = 0;

        // 1. Respiratory Rate (/min)
        Integer rr = vitals.getRespiratoryRate();
        if (rr != null) {
            if (rr <= 8) {
                score += 3;
            } else if (rr <= 11) {
                score += 1;
            } else if (rr <= 20) {
                score += 0;
            } else if (rr <= 24) {
                score += 2;
            } else {
                score += 3;
            }
        }

        // 2. Oxygen Saturation SpO2 (%) - Scale 1
        Integer spo2 = vitals.getSpo2();
        if (spo2 != null) {
            if (spo2 <= 91) {
                score += 3;
            } else if (spo2 <= 93) {
                score += 2;
            } else if (spo2 <= 95) {
                score += 1;
            } else {
                score += 0;
            }
        }

        // 3. Systolic Blood Pressure (mmHg)
        Integer sbp = vitals.getSystolicBp();
        if (sbp != null) {
            if (sbp <= 90) {
                score += 3;
            } else if (sbp <= 100) {
                score += 2;
            } else if (sbp <= 110) {
                score += 1;
            } else if (sbp <= 219) {
                score += 0;
            } else {
                score += 3;
            }
        }

        // 4. Heart Rate / Pulse (bpm)
        Integer hr = vitals.getHeartRate();
        if (hr != null) {
            if (hr <= 40) {
                score += 3;
            } else if (hr <= 50) {
                score += 1;
            } else if (hr <= 90) {
                score += 0;
            } else if (hr <= 110) {
                score += 1;
            } else if (hr <= 130) {
                score += 2;
            } else {
                score += 3;
            }
        }

        // 5. Body Temperature (Fahrenheit)
        Double temp = vitals.getTemperature();
        if (temp != null) {
            if (temp <= 95.0) {
                score += 3;
            } else if (temp <= 96.8) {
                score += 1;
            } else if (temp <= 100.4) {
                score += 0;
            } else if (temp <= 102.2) {
                score += 1;
            } else {
                score += 2;
            }
        }

        // 6. Consciousness / GCS (Alert = 15 -> 0 pts; CVPU / GCS < 15 -> 3 pts)
        if (gcs != null && gcs < 15) {
            score += 3;
        }

        return score;
    }

    public static int calculateNEWS2(Vitals vitals) {
        return calculateScore(vitals, 15);
    }

    /**
     * Categorizes NEWS2 clinical risk tier: LOW, MEDIUM, or HIGH/CRITICAL.
     */
    public static String getRiskTier(int score) {
        if (score >= 7) {
            return "HIGH";
        } else if (score >= 5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    public static String getRiskCategory(int score) {
        if (score >= 7) return "High Risk";
        if (score >= 5) return "Medium Risk";
        return "Low Risk";
    }

    /**
     * Color hex for the NEWS2 badge.
     */
    public static String getRiskColor(int score) {
        if (score >= 7) {
            return "#B91C1C"; // Red
        } else if (score >= 5) {
            return "#C2410C"; // Orange / High
        } else if (score >= 3) {
            return "#A16207"; // Yellow / Medium
        } else {
            return "#15803D"; // Green / Low
        }
    }
}
