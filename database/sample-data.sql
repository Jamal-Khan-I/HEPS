-- ==========================================================
-- Hospital Emergency Priority System (HEPS)
-- Sample Seed Data
-- ==========================================================

USE hospital_emergency_db;

-- Clear previous sample records
DELETE FROM treatment_records;
DELETE FROM patients;
DELETE FROM users;

-- 1. Default System Users (Passwords hashed with SHA-256)
-- admin / admin123
-- receptionist / recep123
-- doctor_sharma / doc123
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin', SHA2('admin123', 256), 'Admin (Demo)', 'ADMIN'),
('receptionist', SHA2('recep123', 256), 'Triage Staff (Demo)', 'RECEPTIONIST'),
('doctor_sharma', SHA2('doc123', 256), 'Duty Clinician (Demo)', 'DOCTOR');

-- 2. Sample Patients (Varied Emergency Levels and Statuses)
-- Clearly fictional demonstration records
-- PriorityQueue orders WAITING patients by:
-- 1. Emergency Level (CRITICAL > HIGH > MEDIUM > LOW)
-- 2. Arrival Time (earlier first)
-- 3. Token No (smaller first)

INSERT INTO patients (patient_id, token_no, full_name, age, gender, phone, symptoms, emergency_level, arrival_time, status) VALUES
(1, 'TK-2026-0001', 'Amitav Sengupta (Demo)', 62, 'Male', '+91 98000 00001', 'Severe acute chest pressure radiating to left arm, heavy diaphoresis', 'CRITICAL', '2026-09-14 08:30:00', 'COMPLETED'),
(2, 'TK-2026-0002', 'Pooja Bhatia (Demo)', 28, 'Female', '+91 98000 00002', 'Open right lower leg deformity following road incident, bleeding controlled', 'HIGH', '2026-09-14 08:45:00', 'COMPLETED'),
(3, 'TK-2026-0003', 'Rohan Mehra (Demo)', 19, 'Male', '+91 98000 00003', 'Deep forearm laceration, sterile compression dressing applied', 'MEDIUM', '2026-09-14 09:00:00', 'IN_TREATMENT'),
(4, 'TK-2026-0004', 'Kavita Sundaram (Demo)', 45, 'Female', '+91 98000 00004', 'Mild fever (100.2 F), sore throat, body ache for 2 days', 'LOW', '2026-09-14 09:10:00', 'WAITING'),
(5, 'TK-2026-0005', 'Vikramaditya Rao (Demo)', 54, 'Male', '+91 98000 00005', 'Sudden facial weakness, difficulty speaking, right-side mobility deficit', 'CRITICAL', '2026-09-14 09:15:00', 'WAITING'),
(6, 'TK-2026-0006', 'Meera Nair (Demo)', 34, 'Female', '+91 98000 00006', 'Acute respiratory distress, audible wheezing, low oxygen saturation', 'CRITICAL', '2026-09-14 09:25:00', 'WAITING'),
(7, 'TK-2026-0007', 'Harish Patel (Demo)', 71, 'Male', '+91 98000 00007', 'High grade fever with chills, severe dehydration, altered alertness', 'HIGH', '2026-09-14 09:30:00', 'WAITING'),
(8, 'TK-2026-0008', 'Siddharth Bose (Demo)', 22, 'Male', '+91 98000 00008', 'Second-degree thermal burn on hand, severe pain reported', 'HIGH', '2026-09-14 09:35:00', 'WAITING'),
(9, 'TK-2026-0009', 'Sunita Reddy (Demo)', 39, 'Female', '+91 98000 00009', 'Persistent lower right abdominal pain with localized tenderness', 'MEDIUM', '2026-09-14 09:40:00', 'WAITING'),
(10, 'TK-2026-0010', 'Tanya Kapoor (Demo)', 12, 'Female', '+91 98000 00010', 'Twisted ankle during sports, mild swelling, able to bear partial weight', 'LOW', '2026-09-14 09:50:00', 'WAITING'),
(11, 'TK-2026-0011', 'Gurpreet Singh (Demo)', 48, 'Male', '+91 98000 00011', 'Acute breathing difficulty with rapid swelling and widespread hives', 'CRITICAL', '2026-09-14 09:55:00', 'WAITING'),
(12, 'TK-2026-0012', 'Farhan Akhtar (Demo)', 26, 'Male', '+91 98000 00012', 'Chronic back discomfort flare-up, requests routine assessment', 'LOW', '2026-09-14 10:00:00', 'WAITING');

-- 3. Sample Treatment Records (For completed and in-treatment patients)
INSERT INTO treatment_records (record_id, patient_id, doctor_name, treatment_notes, admitted_at, completed_at) VALUES
(1, 1, 'Duty Clinician (Demo)', 'Emergency cardiac protocol initiated. Oxygen therapy and monitoring started. Transferred to cardiac unit in stable condition.', '2026-09-14 08:35:00', '2026-09-14 09:45:00'),
(2, 2, 'Duty Clinician (Demo)', 'Deformity stabilized with temporary splint, wound cleansed and dressed under sterile conditions. Admitted for orthopedic evaluation.', '2026-09-14 08:50:00', '2026-09-14 09:55:00'),
(3, 3, 'Duty Clinician (Demo)', 'Wound irrigation with normal saline, 6 nylon sutures placed under local anesthesia. Tetanus immunization administered.', '2026-09-14 09:05:00', NULL);
