package com.hospital.priority.model;

import java.io.Serializable;

/**
 * ============================================================================
 * HEPS 2.0 DOMAIN MODEL: CLINICAL VITALS
 * ============================================================================
 * Represents structured physiological vital signs recorded during triage intake.
 * Evaluated by both the Java Triage Rule Engine and AI Multi-Agent Orchestrator.
 */
public class Vitals implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer spo2;              // Oxygen Saturation in % (Normal: 95-100)
    private Integer heartRate;         // Beats Per Minute / BPM (Normal: 60-100)
    private Double temperature;        // Body Temperature in Fahrenheit (Normal: 97.0-99.0)
    private Integer systolicBp;        // Systolic Blood Pressure in mmHg (Normal: 90-120)
    private Integer diastolicBp;       // Diastolic Blood Pressure in mmHg (Normal: 60-80)
    private Integer respiratoryRate;   // Breaths Per Minute (Normal: 12-20)

    public Vitals() {
    }

    public Vitals(Integer spo2, Integer heartRate, Double temperature,
                  Integer systolicBp, Integer diastolicBp, Integer respiratoryRate) {
        this.spo2 = spo2;
        this.heartRate = heartRate;
        this.temperature = temperature;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.respiratoryRate = respiratoryRate;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    /**
     * Checks if any vital sign crosses critical danger thresholds.
     */
    public boolean hasCriticalVitals() {
        if (spo2 != null && spo2 < 90) return true;
        if (heartRate != null && (heartRate < 40 || heartRate > 140)) return true;
        if (systolicBp != null && (systolicBp < 80 || systolicBp > 200)) return true;
        if (respiratoryRate != null && (respiratoryRate < 8 || respiratoryRate > 32)) return true;
        if (temperature != null && temperature >= 104.0) return true;
        return false;
    }

    /**
     * Checks if any vital sign is outside standard normal range.
     */
    public boolean hasAbnormalVitals() {
        if (spo2 != null && spo2 < 95) return true;
        if (heartRate != null && (heartRate < 50 || heartRate > 110)) return true;
        if (systolicBp != null && (systolicBp < 90 || systolicBp > 140)) return true;
        if (temperature != null && (temperature > 100.4 || temperature < 96.0)) return true;
        if (respiratoryRate != null && (respiratoryRate < 10 || respiratoryRate > 24)) return true;
        return false;
    }

    public String getBpFormatted() {
        if (systolicBp != null && diastolicBp != null) {
            return systolicBp + "/" + diastolicBp + " mmHg";
        } else if (systolicBp != null) {
            return systolicBp + " mmHg";
        }
        return "N/A";
    }

    public String getBloodPressure() {
        return getBpFormatted();
    }

    public String toSummaryString() {
        StringBuilder sb = new StringBuilder();
        if (spo2 != null) sb.append("SpO₂: ").append(spo2).append("% | ");
        if (heartRate != null) sb.append("HR: ").append(heartRate).append(" bpm | ");
        if (systolicBp != null && diastolicBp != null) sb.append("BP: ").append(systolicBp).append("/").append(diastolicBp).append(" | ");
        if (temperature != null) sb.append("Temp: ").append(temperature).append("°F | ");
        if (respiratoryRate != null) sb.append("RR: ").append(respiratoryRate).append("/min");
        String result = sb.toString();
        if (result.endsWith(" | ")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.isEmpty() ? "Vitals not recorded" : result;
    }

    @Override
    public String toString() {
        return toSummaryString();
    }
}
