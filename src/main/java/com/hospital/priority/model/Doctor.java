package com.hospital.priority.model;

import java.io.Serializable;

/**
 * ============================================================================
 * HEPS 2.0 DOMAIN MODEL: DOCTOR & CLINICAL SPECIALIST
 * ============================================================================
 * Represents attending physicians and specialists in the hospital emergency department.
 * Managed by DoctorRoutingService for intelligent automated assignment.
 */
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    private int doctorId;
    private String name;
    private Specialty specialty;
    private DoctorStatus status;
    private int currentWorkload; // Count of active IN_TREATMENT patients
    private String roomNumber;
    private String phone;

    public Doctor() {
        this.status = DoctorStatus.AVAILABLE;
        this.currentWorkload = 0;
    }

    public Doctor(int doctorId, String name, Specialty specialty, DoctorStatus status,
                  int currentWorkload, String roomNumber, String phone) {
        this.doctorId = doctorId;
        this.name = name;
        this.specialty = specialty;
        this.status = status != null ? status : DoctorStatus.AVAILABLE;
        this.currentWorkload = currentWorkload;
        this.roomNumber = roomNumber;
        this.phone = phone;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public DoctorStatus getStatus() {
        return status;
    }

    public void setStatus(DoctorStatus status) {
        this.status = status;
    }

    public int getCurrentWorkload() {
        return currentWorkload;
    }

    public void setCurrentWorkload(int currentWorkload) {
        this.currentWorkload = currentWorkload;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAvailable() {
        return this.status == DoctorStatus.AVAILABLE;
    }

    public void incrementWorkload() {
        this.currentWorkload++;
        if (this.currentWorkload >= 3) {
            this.status = DoctorStatus.BUSY;
        }
    }

    public void decrementWorkload() {
        if (this.currentWorkload > 0) {
            this.currentWorkload--;
        }
        if (this.currentWorkload < 3 && this.status == DoctorStatus.BUSY) {
            this.status = DoctorStatus.AVAILABLE;
        }
    }

    @Override
    public String toString() {
        return name + " (" + specialty.getDisplayName() + " - " + status.getDisplayName() + ")";
    }
}
