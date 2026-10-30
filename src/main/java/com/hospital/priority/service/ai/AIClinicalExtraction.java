package com.hospital.priority.service.ai;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;

/**
 * Structured Clinical Entity Model extracted by AI from unstructured clinical narratives.
 */
public class AIClinicalExtraction {

    private String fullName;
    private Integer age;
    private String gender;
    private String phone;
    private String arrivalMode;
    private String chiefComplaint;
    private Integer spo2;
    private Integer heartRate;
    private Double temperature;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer respiratoryRate;
    private Integer painScore;
    private String allergies;
    private String knownConditions;
    private EmergencyLevel urgency;
    private Specialty specialty;
    private String reasoning;

    public AIClinicalExtraction() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
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

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getArrivalMode() {
        return arrivalMode;
    }

    public void setArrivalMode(String arrivalMode) {
        this.arrivalMode = arrivalMode;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
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

    public Integer getPainScore() {
        return painScore;
    }

    public void setPainScore(Integer painScore) {
        this.painScore = painScore;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getKnownConditions() {
        return knownConditions;
    }

    public void setKnownConditions(String knownConditions) {
        this.knownConditions = knownConditions;
    }

    public EmergencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(EmergencyLevel urgency) {
        this.urgency = urgency;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}

