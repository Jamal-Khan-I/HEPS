-- ==========================================================
-- Hospital Emergency Priority System (HEPS 2.0)
-- Upgraded Database Schema for MySQL 8+
-- Features: Structured Vitals, AI Audit Logs, Doctor Routing
-- ==========================================================

CREATE DATABASE IF NOT EXISTS hospital_emergency_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hospital_emergency_db;

-- Drop existing tables if re-initializing (child tables first)
DROP TABLE IF EXISTS ai_audit_logs;
DROP TABLE IF EXISTS treatment_records;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS users;

-- 1. Doctors & Specialists Table (HEPS 2.0)
CREATE TABLE doctors (
    doctor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialty VARCHAR(50) NOT NULL COMMENT 'CARDIOLOGY, NEUROLOGY, ORTHOPEDICS, PULMONOLOGY, PEDIATRICS, TRAUMA_SURGERY, GENERAL_EMERGENCY',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, BUSY, OFF_DUTY',
    current_workload INT NOT NULL DEFAULT 0,
    room_number VARCHAR(50),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doctors_specialty_status (specialty, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Patients Table (HEPS 2.0 Enhanced with Vitals & AI/Routing)
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
    
    -- HEPS 2.0 Structured Physiological Vitals
    spo2 INT DEFAULT NULL COMMENT 'Oxygen Saturation %',
    heart_rate INT DEFAULT NULL COMMENT 'Beats per minute',
    temperature DECIMAL(4,1) DEFAULT NULL COMMENT 'Body temperature Fahrenheit',
    systolic_bp INT DEFAULT NULL COMMENT 'Systolic Blood Pressure mmHg',
    diastolic_bp INT DEFAULT NULL COMMENT 'Diastolic Blood Pressure mmHg',
    respiratory_rate INT DEFAULT NULL COMMENT 'Breaths per minute',

    -- HEPS 2.0 Doctor Routing & AI Triage Integration
    assigned_specialty VARCHAR(50) DEFAULT 'GENERAL_EMERGENCY',
    assigned_doctor_id INT DEFAULT NULL,
    assigned_doctor_name VARCHAR(100) DEFAULT NULL,
    assigned_bay VARCHAR(50) DEFAULT 'Bay 4 [Observation - Yellow]',
    ai_suggested_level VARCHAR(20) DEFAULT NULL,
    rule_suggested_level VARCHAR(20) DEFAULT NULL,
    override_reason TEXT DEFAULT NULL,
    escalated BOOLEAN DEFAULT FALSE,
    escalation_reason TEXT DEFAULT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_patients_status (status),
    INDEX idx_patients_priority (emergency_level, arrival_time),
    INDEX idx_patients_token (token_no),
    INDEX idx_patients_specialty (assigned_specialty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Treatment Records Table
CREATE TABLE treatment_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    treatment_notes TEXT,
    disposition VARCHAR(50) DEFAULT 'DISCHARGED_HOME' COMMENT 'DISCHARGED_HOME, ADMITTED_INPATIENT, TRANSFERRED_ICU',
    admitted_at DATETIME NOT NULL,
    completed_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_treatment_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_treatment_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. AI & Decision Audit Logs Table (HEPS 2.0)
CREATE TABLE ai_audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT DEFAULT NULL,
    token_no VARCHAR(30) DEFAULT NULL,
    agent_name VARCHAR(50) NOT NULL COMMENT 'INTAKE, TRIAGE, SPECIALTY, RISK, ORCHESTRATOR',
    ai_output TEXT,
    rule_output TEXT,
    human_action VARCHAR(30) DEFAULT 'ACCEPTED' COMMENT 'ACCEPTED, OVERRIDDEN, FALLBACK',
    override_reason TEXT,
    latency_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_token (token_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL COMMENT 'ADMIN, RECEPTIONIST, DOCTOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Initial Doctors Roster for HEPS 2.0
INSERT INTO doctors (name, specialty, status, current_workload, room_number, phone) VALUES
('Dr. Rajesh Sharma', 'CARDIOLOGY', 'AVAILABLE', 0, 'Cardio Suite 101', '+91 98111 22233'),
('Dr. Priya Nair', 'NEUROLOGY', 'AVAILABLE', 0, 'Neuro ICU 102', '+91 98222 33344'),
('Dr. Vikram Patel', 'ORTHOPEDICS', 'AVAILABLE', 0, 'Ortho Room 103', '+91 98333 44455'),
('Dr. Ananya Sen', 'PULMONOLOGY', 'AVAILABLE', 0, 'Pulmo Bay 104', '+91 98444 55566'),
('Dr. Sneha Reddy', 'PEDIATRICS', 'AVAILABLE', 0, 'Pedia Care 105', '+91 98555 66677'),
('Dr. Arjun Mehta', 'TRAUMA_SURGERY', 'AVAILABLE', 0, 'Trauma OT 1', '+91 98666 77788'),
('Dr. David Miller', 'GENERAL_EMERGENCY', 'AVAILABLE', 0, 'ER Station A', '+91 98777 88899'),
('Dr. Kavita Verma', 'GENERAL_EMERGENCY', 'AVAILABLE', 0, 'ER Station B', '+91 98888 99900');
