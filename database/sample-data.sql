-- ==========================================================
-- Hospital Emergency Priority System (HEPS)
-- Upgraded Seed Data with Dynamic Relative Timestamps
-- Matches Target UI Design (PDF Pages 1 - 18)
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

-- 2. Sample Patients matching PDF Pages 2, 8, 9, 10, 12, 13
DELETE FROM treatment_records;
DELETE FROM patients;

INSERT INTO patients (
    patient_id, token_no, uhid, full_name, age, gender, phone, symptoms, emergency_level,
    arrival_time, status, spo2, heart_rate, temperature, systolic_bp, diastolic_bp,
    respiratory_rate, assigned_specialty, assigned_doctor_name, assigned_bay,
    ai_suggested_level, rule_suggested_level, escalated, escalation_reason, allergies, news2_score
) VALUES
-- 1. Ramesh Kumar (Critical, Front of heap, Target Page 9: In treatment 14m)
(1, 'TK-2026-0142', 'UH-2026-000891', 'Ramesh Kumar', 58, 'Male', '9876543210',
 'Chest pain radiating to left arm, sweating, acute onset 30 minutes ago', 'CRITICAL',
 NOW() - INTERVAL 14 MINUTE, 'IN_TREATMENT', 94, 108, 98.6, 140, 90, 20,
 'CARDIOLOGY', 'Dr. Arjun Mehta', 'Bay 1 Resus', 'HIGH', 'CRITICAL', FALSE, NULL, 'penicillin', 6),

-- 2. Anita Joshi (High, Leg fracture)
(2, 'TK-0143', 'UH-2026-000892', 'Anita Joshi', 34, 'Female', '9876511223',
 'Open right lower leg deformity following road incident, bleeding controlled', 'HIGH',
 NOW() - INTERVAL 18 MINUTE, 'IN_TREATMENT', 97, 102, 98.4, 130, 85, 20,
 'ORTHOPEDICS', 'Dr. Vikram Patel', 'Bay 3 Acute', 'HIGH', 'HIGH', FALSE, NULL, 'None', 2),

-- 3. Harish Patel (Escalated High due to 52m wait)
(3, 'TK-0139', 'UH-2026-000893', 'Harish Patel', 71, 'Male', '9876533445',
 'Persistent lower abdominal pain with fever and altered alertness', 'HIGH',
 NOW() - INTERVAL 22 MINUTE, 'IN_TREATMENT', 94, 98, 101.2, 115, 75, 22,
 'GENERAL_EMERGENCY', 'Dr. David Miller', 'Bay 3b Acute', 'MEDIUM', 'MEDIUM', FALSE, NULL, 'Sulfa', 5),

-- 4. Kavita Sundaram (Medium, Fever & body ache)
(4, 'TK-0140', 'UH-2026-000894', 'Kavita Sundaram', 45, 'Female', '9876555667',
 'Moderate grade fever (101.4 F), sore throat, body ache for 2 days', 'MEDIUM',
 NOW() - INTERVAL 11 MINUTE, 'WAITING', 98, 86, 101.4, 120, 80, 18,
 'GENERAL_EMERGENCY', 'Unassigned', 'Bay 4 Obs', 'MEDIUM', 'MEDIUM', FALSE, NULL, 'None', 1),

-- 5. Farhan Akhtar (Medium, Back pain)
(5, 'TK-0144', 'UH-2026-000895', 'Farhan Akhtar', 26, 'Male', '9876577889',
 'Acute lumbar spasm and lower back stiffness, unable to sit comfortably', 'MEDIUM',
 NOW() - INTERVAL 8 MINUTE, 'WAITING', 99, 78, 98.2, 122, 78, 16,
 'ORTHOPEDICS', 'Unassigned', 'Bay 4b Obs', 'MEDIUM', 'LOW', FALSE, NULL, 'None', 0),

-- 6. Tanya Kapoor (Low, Ankle sprain)
(6, 'TK-0145', 'UH-2026-000896', 'Tanya Kapoor', 12, 'Female', '9876599001',
 'Twisted right ankle during sports, mild localized swelling, able to bear partial weight', 'LOW',
 NOW() - INTERVAL 4 MINUTE, 'WAITING', 99, 82, 98.4, 105, 68, 18,
 'PEDIATRICS', 'Unassigned', 'Bay 5 Fast track', 'LOW', 'LOW', FALSE, NULL, 'None', 0),

-- 7. Meera Nair (In Treatment, Resus bay 2)
(7, 'TK-0136', 'UH-2026-000897', 'Meera Nair', 34, 'Female', '9876522334',
 'Acute respiratory distress, audible wheezing, low oxygen saturation', 'CRITICAL',
 NOW() - INTERVAL 10 MINUTE, 'IN_TREATMENT', 89, 138, 99.1, 145, 92, 32,
 'PULMONOLOGY', 'Dr. Sneha Reddy', 'Bay 2 Trauma', 'CRITICAL', 'CRITICAL', FALSE, NULL, 'Aspirin', 9),

-- 8. Deepak Sharma (Low, Mild head cold)
(8, 'TK-0146', 'UH-2026-000898', 'Deepak Sharma', 52, 'Male', '9876522119',
 'Mild headache and nasal congestion, no breathing difficulty', 'LOW',
 NOW() - INTERVAL 2 MINUTE, 'WAITING', 98, 74, 98.6, 124, 80, 16,
 'GENERAL_EMERGENCY', 'Unassigned', 'Bay 6 Ambulatory', 'LOW', 'LOW', FALSE, NULL, 'None', 0),

-- 9. Sunita Rao (Medium, Dizziness)
(9, 'TK-0147', 'UH-2026-000899', 'Sunita Rao', 63, 'Female', '9876532221',
 'Postural dizziness and mild dehydration, history of hypertension', 'MEDIUM',
 NOW() - INTERVAL 14 MINUTE, 'WAITING', 96, 90, 98.8, 138, 88, 18,
 'CARDIOLOGY', 'Unassigned', 'Bay 7 Obs', 'MEDIUM', 'MEDIUM', FALSE, NULL, 'None', 1),

-- 10. Vikram Malhotra (High, Laceration)
(10, 'TK-0148', 'UH-2026-000900', 'Vikram Malhotra', 41, 'Male', '9876544332',
 'Deep laceration to left forearm with arterial bleeding controlled by pressure dressing', 'HIGH',
 NOW() - INTERVAL 15 MINUTE, 'IN_TREATMENT', 96, 104, 99.0, 132, 84, 20,
 'TRAUMA_SURGERY', 'Dr. Arjun Mehta', 'Bay 8 Acute', 'HIGH', 'HIGH', FALSE, NULL, 'None', 3),

-- 11. Ananya Ghosh (Low, Gastric acidity)
(11, 'TK-0149', 'UH-2026-000901', 'Ananya Ghosh', 29, 'Female', '9876555443',
 'Epigastric burning sensation after meals, no vomiting or hematemesis', 'LOW',
 NOW() - INTERVAL 6 MINUTE, 'WAITING', 99, 72, 98.4, 118, 76, 16,
 'GENERAL_EMERGENCY', 'Unassigned', 'Bay 9 Ambulatory', 'LOW', 'LOW', FALSE, NULL, 'None', 0),

-- 12. Robert D''Souza (Critical, Cardiogenic shock)
(12, 'TK-0138', 'UH-2026-000902', 'Robert D''Souza', 67, 'Male', '9876566554',
 'Severe hypotension, cold clammy extremities, marked sinus tachycardia', 'CRITICAL',
 NOW() - INTERVAL 6 MINUTE, 'IN_TREATMENT', 86, 142, 98.2, 88, 56, 28,
 'CARDIOLOGY', 'Dr. Rajesh Sharma', 'Bay 1 Resus', 'CRITICAL', 'CRITICAL', FALSE, NULL, 'Iodine contrast', 8),

-- 13. Gurpreet Singh (Completed / Discharged)
(13, 'TK-0131', 'UH-2026-000880', 'Gurpreet Singh', 48, 'Male', '9876544556',
 'Unstable angina, retrosternal heaviness', 'MEDIUM',
 NOW() - INTERVAL 180 MINUTE, 'COMPLETED', 96, 92, 98.6, 135, 88, 18,
 'CARDIOLOGY', 'Dr. David Miller', 'Bay 4 Obs', 'MEDIUM', 'MEDIUM', FALSE, NULL, 'None', 0);

-- 3. Initial Treatment Records
INSERT INTO treatment_records (patient_id, doctor_name, treatment_notes, disposition, admitted_at, completed_at) VALUES
(1, 'Dr. Arjun Mehta', 'Aspirin loading dose given. Telemetry active. STAT troponin and 12-lead ECG ordered.', 'ADMITTED_INPATIENT', NOW() - INTERVAL 14 MINUTE, NULL),
(7, 'Dr. Sneha Reddy', 'Nebulization initiated with Salbutamol and Ipratropium. IV Hydrocortisone administered. Monitoring SpO2.', 'TRANSFERRED_ICU', NOW() - INTERVAL 22 MINUTE, NULL),
(13, 'Dr. David Miller', '12-lead ECG showed T-wave inversion. Aspirin loading and nitrates given. Symptoms fully resolved. Discharged with outpatient cardiology follow-up.', 'DISCHARGED_HOME', NOW() - INTERVAL 180 MINUTE, NOW() - INTERVAL 30 MINUTE);
