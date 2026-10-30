-- ==========================================================
-- Hospital Emergency Priority System (HEPS)
-- Pristine Database Initialization (All Bays & Queues Clean)
-- ==========================================================

USE hospital_emergency_db;

-- 1. Default System Users (Passwords hashed with BCrypt)
-- Demo credentials:
-- receptionist / recep123
-- nurse.priya  / triage123
-- dr.mehta     / doc123
-- charge.anita / charge123
-- admin        / admin123

DELETE FROM users;
INSERT INTO users (username, password_hash, full_name, role) VALUES
('nurse.priya', '$2a$10$w09ZqW/H4Q3rXoXvX8a4heB4aG7Y0yJzP9E8.hK0o8x.2q1y.eZ1a', 'Priya S.', 'TRIAGE_NURSE'),
('dr.mehta', '$2a$10$w09ZqW/H4Q3rXoXvX8a4heB4aG7Y0yJzP9E8.hK0o8x.2q1y.eZ1a', 'Dr. Arjun Mehta', 'DOCTOR'),
('desk.ravi', '$2a$10$w09ZqW/H4Q3rXoXvX8a4heB4aG7Y0yJzP9E8.hK0o8x.2q1y.eZ1a', 'Ravi V.', 'RECEPTIONIST'),
('charge.anita', '$2a$10$w09ZqW/H4Q3rXoXvX8a4heB4aG7Y0yJzP9E8.hK0o8x.2q1y.eZ1a', 'Anita N.', 'CHARGE_NURSE'),
('admin', '$2a$10$w09ZqW/H4Q3rXoXvX8a4heB4aG7Y0yJzP9E8.hK0o8x.2q1y.eZ1a', 'Administrator', 'ADMIN');

-- 2. Clean State for Real Hospital Live Admissions (Zero Dummy Patients)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE treatment_records;
TRUNCATE TABLE clinical_notes;
TRUNCATE TABLE ai_audit_logs;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE patients;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE patients AUTO_INCREMENT = 1;
ALTER TABLE treatment_records AUTO_INCREMENT = 1;
ALTER TABLE clinical_notes AUTO_INCREMENT = 1;
ALTER TABLE audit_log AUTO_INCREMENT = 1;
