-- ============================================================================
-- HEPS 2.0 PRISTINE DATABASE CLEANUP & AUTHENTIC CLINICAL SEED SCRIPT
-- ============================================================================

USE hospital_emergency_db;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE treatment_records;
TRUNCATE TABLE clinical_notes;
TRUNCATE TABLE ai_audit_logs;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE patients;

SET FOREIGN_KEY_CHECKS = 1;

-- Reset Auto-Increment
ALTER TABLE patients AUTO_INCREMENT = 1;
ALTER TABLE treatment_records AUTO_INCREMENT = 1;
ALTER TABLE clinical_notes AUTO_INCREMENT = 1;
ALTER TABLE audit_log AUTO_INCREMENT = 1;

-- Insert 7 distinct realistic emergency cohort patients across 7 specialties
INSERT INTO patients (
    patient_id, token_no, uhid, full_name, age, gender, phone, symptoms,
    emergency_level, arrival_time, status, spo2, heart_rate, temperature,
    systolic_bp, diastolic_bp, respiratory_rate, assigned_specialty,
    assigned_doctor_name, assigned_bay, arrival_mode, mlc, gcs, pain_score,
    news2_score, allergies, known_conditions
) VALUES 
(1, 'TK-2026-0101', 'UH-2026-000101', 'Ramesh Kumar', 58, 'Male', '9876543210', 
 'Severe retrosternal chest pain radiating to left arm, diaphoresis, acute onset 40 mins ago', 
 'CRITICAL', DATE_SUB(NOW(), INTERVAL 25 MINUTE), 'IN_TREATMENT', 89, 118, 98.6, 165, 100, 24, 
 'CARDIOLOGY', 'Dr. Rajesh Sharma', 'Bay 1 Resus', 'Ambulance', 0, 15, 8, 8, 'Penicillin', 'Hypertension, Type 2 Diabetes'),

(2, 'TK-2026-0102', 'UH-2026-000102', 'Anita Joshi', 34, 'Female', '9876543211', 
 'Closed fracture right tibia with severe localized swelling and deformity after staircase fall', 
 'HIGH', DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'IN_TREATMENT', 98, 96, 98.4, 130, 84, 18, 
 'ORTHOPEDICS', 'Dr. Vikram Patel', 'Bay 3 Acute', 'Walk-in', 0, 15, 7, 3, 'None', 'None'),

(3, 'TK-2026-0103', 'UH-2026-000103', 'Meera Nair', 42, 'Female', '9876543212', 
 'Severe acute bronchial asthma exacerbation, audible expiratory wheeze, respiratory exhaustion', 
 'CRITICAL', DATE_SUB(NOW(), INTERVAL 18 MINUTE), 'IN_TREATMENT', 87, 126, 99.1, 142, 92, 30, 
 'PULMONOLOGY', 'Dr. Ananya Sen', 'Bay 2 Resus', 'Ambulance', 0, 14, 6, 9, 'Aspirin', 'Chronic Bronchial Asthma'),

(4, 'TK-2026-0104', 'UH-2026-000104', 'Robert D\'Souza', 67, 'Male', '9876543213', 
 'Acute neurological deficit, facial asymmetry, slurred speech and right-sided hemiparesis onset 45m', 
 'HIGH', DATE_SUB(NOW(), INTERVAL 14 MINUTE), 'WAITING', 95, 88, 98.6, 175, 105, 18, 
 'NEUROLOGY', 'Dr. Priya Nair', 'Bay 4 Acute', 'Brought by relatives', 0, 13, 4, 5, 'Sulfa drugs', 'Atrial Fibrillation'),

(5, 'TK-2026-0105', 'UH-2026-000105', 'Vikram Malhotra', 28, 'Male', '9876543214', 
 'Two-wheeler vehicular collision, 8cm deep laceration to right thigh with active venous bleeding', 
 'HIGH', DATE_SUB(NOW(), INTERVAL 10 MINUTE), 'WAITING', 97, 110, 98.2, 115, 75, 20, 
 'TRAUMA_SURGERY', 'Dr. Arjun Mehta', 'Bay 5 Acute', 'Ambulance', 1, 15, 8, 4, 'None', 'None'),

(6, 'TK-2026-0106', 'UH-2026-000106', 'Tanya Kapoor', 11, 'Female', '9876543215', 
 'High continuous fever (103.2 F) for 3 days with recurrent vomiting and abdominal tenderness', 
 'MEDIUM', DATE_SUB(NOW(), INTERVAL 6 MINUTE), 'WAITING', 98, 108, 103.2, 102, 68, 22, 
 'PEDIATRICS', 'Dr. Sneha Reddy', 'Bay 7 Monitored', 'Brought by relatives', 0, 15, 5, 4, 'None', 'None'),

(7, 'TK-2026-0107', 'UH-2026-000107', 'Deepak Sharma', 45, 'Male', '9876543216', 
 'Inversion ankle sprain during morning exercise, localized lateral malleolar tenderness, ambulatory', 
 'LOW', DATE_SUB(NOW(), INTERVAL 3 MINUTE), 'WAITING', 99, 74, 98.6, 122, 80, 16, 
 'GENERAL_EMERGENCY', 'Dr. David Miller', 'Bay 9 Ambulatory', 'Walk-in', 0, 15, 3, 0, 'None', 'None');

-- Initial Treatment Record for Active In-Treatment Patients
INSERT INTO treatment_records (patient_id, doctor_name, treatment_notes, admitted_at, disposition) VALUES
(1, 'Dr. Rajesh Sharma', 'Initiated STEMI protocol. Oxygen by nasal cannula 4L/min, dual antiplatelet therapy given, IV Heparin bolus. Cardiology catheterization lab activated.', DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'IN_TREATMENT'),
(2, 'Dr. Vikram Patel', 'Analgesia administered (IV Paracetamol 1g). Radiography ordered. Temporary posterior slab applied awaiting orthopedic alignment.', DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'IN_TREATMENT'),
(3, 'Dr. Ananya Sen', 'Continuous nebulization with Salbutamol and Ipratropium bromide. IV Hydrocortisone 100mg given. SpO2 monitored continuously.', DATE_SUB(NOW(), INTERVAL 12 MINUTE), 'IN_TREATMENT');

-- Initial Compliance Audit Trail
INSERT INTO audit_log (username, role, action, patient_token, uhid, old_value, new_value, details) VALUES
('system', 'SYSTEM', 'SYSTEM_INITIALIZED', 'SYS', 'SYS', 'N/A', 'READY', 'Emergency Department clinical database initialized with 7 diverse emergency encounters.'),
('dr.sharma', 'DOCTOR', 'PATIENT_ADMITTED', 'TK-2026-0101', 'UH-2026-000101', 'WAITING', 'IN_TREATMENT', 'Patient Ramesh Kumar admitted to Bay 1 Resus under Dr. Rajesh Sharma.'),
('dr.patel', 'DOCTOR', 'PATIENT_ADMITTED', 'TK-2026-0102', 'UH-2026-000102', 'WAITING', 'IN_TREATMENT', 'Patient Anita Joshi admitted to Bay 3 Acute under Dr. Vikram Patel.'),
('dr.sen', 'DOCTOR', 'PATIENT_ADMITTED', 'TK-2026-0103', 'UH-2026-000103', 'WAITING', 'IN_TREATMENT', 'Patient Meera Nair admitted to Bay 2 Resus under Dr. Ananya Sen.');
