-- ============================================================================
-- HEPS 2.0 PRISTINE DATABASE CLEANUP SCRIPT (ALL PATIENT DATA REMOVED)
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

-- Initial Compliance Audit Trail
INSERT INTO audit_log (username, role, action, patient_token, uhid, old_value, new_value, details) VALUES
('system', 'SYSTEM', 'SYSTEM_INITIALIZED', 'SYS', 'SYS', 'N/A', 'READY', 'Emergency Department clinical database initialized clean. All bays free, ready for live admissions.');
