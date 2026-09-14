-- ==========================================================
-- Hospital Emergency Priority System (HEPS)
-- Database Schema for MySQL 8+
-- Course: OOP using Java (CSE203) Mini Project
-- ==========================================================

CREATE DATABASE IF NOT EXISTS hospital_emergency_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hospital_emergency_db;

-- Drop existing tables if re-initializing (child tables first)
DROP TABLE IF EXISTS treatment_records;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;

-- 1. Patients Table
CREATE TABLE patients (
    patient_id INT AUTO_INCREMENT PRIMARY KEY,
    token_no VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    symptoms TEXT NOT NULL,
    emergency_level VARCHAR(20) NOT NULL COMMENT 'CRITICAL, HIGH, MEDIUM, LOW',
    arrival_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING, IN_TREATMENT, COMPLETED, CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patients_status (status),
    INDEX idx_patients_priority (emergency_level, arrival_time),
    INDEX idx_patients_token (token_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Treatment Records Table
CREATE TABLE treatment_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    treatment_notes TEXT,
    admitted_at DATETIME NOT NULL,
    completed_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_treatment_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_treatment_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Users Table (for optional role-based authentication)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL COMMENT 'ADMIN, RECEPTIONIST, DOCTOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
