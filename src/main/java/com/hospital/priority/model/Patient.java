package com.hospital.priority.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Encapsulated domain entity representing a patient in the hospital emergency triage system.
 * Core entity processed by the Java PriorityQueue.
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

    private static final DateTimeFormatter DISPLAY_TIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public Patient() {
        this.status = PatientStatus.WAITING;
        this.arrivalTime = LocalDateTime.now();
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
        return "[" + tokenNo + "] " + fullName + " (" + emergencyLevel + ", Arr: " + getFormattedArrivalTime() + ")";
    }
}
