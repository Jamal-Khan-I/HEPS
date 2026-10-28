package com.hospital.priority.dao.impl;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.*;
import com.hospital.priority.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * HEPS 2.0 JDBC IMPLEMENTATION: PATIENT DAO
 * ============================================================================
 * Implements full persistence for patients, physiological vitals, AI triage metadata,
 * assigned specialists, and automated schema self-healing.
 */
public class PatientDaoImpl implements PatientDao {

    private static boolean schemaChecked = false;

    public PatientDaoImpl() {
        ensureSchemaUpgraded();
    }

    /**
     * Auto-migrates database schema if upgrading from HEPS 1.0 to HEPS 2.0.
     * Prevents SQL "Unknown column" errors seamlessly.
     */
    private synchronized void ensureSchemaUpgraded() {
        if (schemaChecked) return;
        schemaChecked = true;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if doctors table exists
            try {
                stmt.executeQuery("SELECT 1 FROM doctors LIMIT 1");
            } catch (SQLException e) {
                // Create doctors table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS doctors (" +
                        "doctor_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "specialty VARCHAR(50) NOT NULL, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', " +
                        "current_workload INT NOT NULL DEFAULT 0, " +
                        "room_number VARCHAR(50), " +
                        "phone VARCHAR(20), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

                // Seed initial doctors
                stmt.executeUpdate("INSERT INTO doctors (name, specialty, status, current_workload, room_number, phone) VALUES " +
                        "('Dr. Rajesh Sharma', 'CARDIOLOGY', 'AVAILABLE', 0, 'Cardio Suite 101', '+91 98111 22233'), " +
                        "('Dr. Priya Nair', 'NEUROLOGY', 'AVAILABLE', 0, 'Neuro ICU 102', '+91 98222 33344'), " +
                        "('Dr. Vikram Patel', 'ORTHOPEDICS', 'AVAILABLE', 0, 'Ortho Room 103', '+91 98333 44455'), " +
                        "('Dr. Ananya Sen', 'PULMONOLOGY', 'AVAILABLE', 0, 'Pulmo Bay 104', '+91 98444 55566'), " +
                        "('Dr. Sneha Reddy', 'PEDIATRICS', 'AVAILABLE', 0, 'Pedia Care 105', '+91 98555 66677'), " +
                        "('Dr. Arjun Mehta', 'TRAUMA_SURGERY', 'AVAILABLE', 0, 'Trauma OT 1', '+91 98666 77788'), " +
                        "('Dr. David Miller', 'GENERAL_EMERGENCY', 'AVAILABLE', 0, 'ER Station A', '+91 98777 88899'), " +
                        "('Dr. Kavita Verma', 'GENERAL_EMERGENCY', 'AVAILABLE', 0, 'ER Station B', '+91 98888 99900')");
            }

            // Check if ai_audit_logs table exists
            try {
                stmt.executeQuery("SELECT 1 FROM ai_audit_logs LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ai_audit_logs (" +
                        "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "patient_id INT DEFAULT NULL, " +
                        "token_no VARCHAR(30) DEFAULT NULL, " +
                        "agent_name VARCHAR(50) NOT NULL, " +
                        "ai_output TEXT, " +
                        "rule_output TEXT, " +
                        "human_action VARCHAR(30) DEFAULT 'ACCEPTED', " +
                        "override_reason TEXT, " +
                        "latency_ms BIGINT DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }

            // Ensure columns in patients table exist
            safeAddColumn(stmt, "patients", "spo2", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "heart_rate", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "temperature", "DECIMAL(4,1) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "systolic_bp", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "diastolic_bp", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "respiratory_rate", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "assigned_specialty", "VARCHAR(50) DEFAULT 'GENERAL_EMERGENCY'");
            safeAddColumn(stmt, "patients", "assigned_doctor_id", "INT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "assigned_doctor_name", "VARCHAR(100) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "ai_suggested_level", "VARCHAR(20) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "rule_suggested_level", "VARCHAR(20) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "override_reason", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "escalated", "BOOLEAN DEFAULT FALSE");
            safeAddColumn(stmt, "patients", "escalation_reason", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "assigned_bay", "VARCHAR(50) DEFAULT 'Bay 4 [Observation - Yellow]'");
            safeAddColumn(stmt, "treatment_records", "disposition", "VARCHAR(50) DEFAULT 'DISCHARGED_HOME'");

            // Enterprise clinical fields (Pages 1 - 18)
            safeAddColumn(stmt, "patients", "uhid", "VARCHAR(50) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "dob", "VARCHAR(50) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "address", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "blood_group", "VARCHAR(10) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "emergency_contact_name", "VARCHAR(100) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "emergency_contact_relation", "VARCHAR(50) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "emergency_contact_phone", "VARCHAR(20) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "arrival_mode", "VARCHAR(50) DEFAULT 'Walk-in'");
            safeAddColumn(stmt, "patients", "mlc", "BOOLEAN DEFAULT FALSE");
            safeAddColumn(stmt, "patients", "gcs", "INT DEFAULT 15");
            safeAddColumn(stmt, "patients", "pain_score", "INT DEFAULT 0");
            safeAddColumn(stmt, "patients", "news2_score", "INT DEFAULT 0");
            safeAddColumn(stmt, "patients", "allergies", "VARCHAR(255) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "known_conditions", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "triage_time", "DATETIME DEFAULT NULL");
            safeAddColumn(stmt, "patients", "doctor_seen_time", "DATETIME DEFAULT NULL");
            safeAddColumn(stmt, "patients", "disposition_time", "DATETIME DEFAULT NULL");
            safeAddColumn(stmt, "patients", "discharge_diagnosis", "VARCHAR(255) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "discharge_medications", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "discharge_follow_up", "VARCHAR(100) DEFAULT NULL");
            safeAddColumn(stmt, "patients", "discharge_advice", "TEXT DEFAULT NULL");
            safeAddColumn(stmt, "patients", "discharge_outcome", "VARCHAR(50) DEFAULT NULL");

            // Audit log table
            try {
                stmt.executeQuery("SELECT 1 FROM audit_log LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audit_log (" +
                        "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(50) NOT NULL, " +
                        "role VARCHAR(30) NOT NULL, " +
                        "action VARCHAR(100) NOT NULL, " +
                        "patient_token VARCHAR(30), " +
                        "uhid VARCHAR(50), " +
                        "old_value TEXT, " +
                        "new_value TEXT, " +
                        "details TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }

            // Clinical notes table
            try {
                stmt.executeQuery("SELECT 1 FROM clinical_notes LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clinical_notes (" +
                        "note_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "patient_id INT NOT NULL, " +
                        "token_no VARCHAR(30) NOT NULL, " +
                        "doctor_name VARCHAR(100) NOT NULL, " +
                        "subjective TEXT, " +
                        "objective TEXT, " +
                        "assessment TEXT, " +
                        "plan TEXT, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
        } catch (Exception e) {
            System.err.println("Notice: Schema self-check completed with note: " + e.getMessage());
        }
    }

    private void safeAddColumn(Statement stmt, String table, String column, String colDefinition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + colDefinition);
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    @Override
    public int insert(Patient patient) throws DatabaseOperationException {
        String sql = "INSERT INTO patients (token_no, full_name, age, gender, phone, symptoms, " +
                "emergency_level, arrival_time, status, spo2, heart_rate, temperature, systolic_bp, " +
                "diastolic_bp, respiratory_rate, assigned_specialty, assigned_doctor_id, assigned_doctor_name, " +
                "ai_suggested_level, rule_suggested_level, override_reason, escalated, escalation_reason, assigned_bay, " +
                "uhid, dob, address, blood_group, emergency_contact_name, emergency_contact_relation, emergency_contact_phone, " +
                "arrival_mode, mlc, gcs, pain_score, news2_score, allergies, known_conditions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, patient.getTokenNo());
            stmt.setString(2, patient.getFullName());
            stmt.setInt(3, patient.getAge());
            stmt.setString(4, patient.getGender());
            stmt.setString(5, patient.getPhone());
            stmt.setString(6, patient.getSymptoms());
            stmt.setString(7, patient.getEmergencyLevel().name());
            stmt.setTimestamp(8, Timestamp.valueOf(patient.getArrivalTime() != null ? patient.getArrivalTime() : LocalDateTime.now()));
            stmt.setString(9, patient.getStatus().name());

            // Vitals
            Vitals v = patient.getVitals();
            if (v != null && v.getSpo2() != null) stmt.setInt(10, v.getSpo2()); else stmt.setNull(10, Types.INTEGER);
            if (v != null && v.getHeartRate() != null) stmt.setInt(11, v.getHeartRate()); else stmt.setNull(11, Types.INTEGER);
            if (v != null && v.getTemperature() != null) stmt.setDouble(12, v.getTemperature()); else stmt.setNull(12, Types.DOUBLE);
            if (v != null && v.getSystolicBp() != null) stmt.setInt(13, v.getSystolicBp()); else stmt.setNull(13, Types.INTEGER);
            if (v != null && v.getDiastolicBp() != null) stmt.setInt(14, v.getDiastolicBp()); else stmt.setNull(14, Types.INTEGER);
            if (v != null && v.getRespiratoryRate() != null) stmt.setInt(15, v.getRespiratoryRate()); else stmt.setNull(15, Types.INTEGER);

            // Routing & AI
            stmt.setString(16, patient.getAssignedSpecialty() != null ? patient.getAssignedSpecialty().name() : Specialty.GENERAL_EMERGENCY.name());
            if (patient.getAssignedDoctorId() != null) stmt.setInt(17, patient.getAssignedDoctorId()); else stmt.setNull(17, Types.INTEGER);
            stmt.setString(18, patient.getAssignedDoctorName());
            stmt.setString(19, patient.getAiSuggestedLevel() != null ? patient.getAiSuggestedLevel().name() : null);
            stmt.setString(20, patient.getRuleSuggestedLevel() != null ? patient.getRuleSuggestedLevel().name() : null);
            stmt.setString(21, patient.getOverrideReason());
            stmt.setBoolean(22, patient.isEscalated());
            stmt.setString(23, patient.getEscalationReason());
            stmt.setString(24, patient.getAssignedBay());

            // Enterprise Clinical Fields (Pages 1-18)
            stmt.setString(25, patient.getUhid());
            stmt.setString(26, patient.getDateOfBirth());
            stmt.setString(27, patient.getAddress());
            stmt.setString(28, patient.getBloodGroup());
            stmt.setString(29, patient.getEmergencyContactName());
            stmt.setString(30, patient.getEmergencyContactRelation());
            stmt.setString(31, patient.getEmergencyContactPhone());
            stmt.setString(32, patient.getArrivalMode());
            stmt.setBoolean(33, patient.isMlc());
            stmt.setInt(34, patient.getGcs() != null ? patient.getGcs() : 15);
            stmt.setInt(35, patient.getPainScore() != null ? patient.getPainScore() : 0);
            stmt.setInt(36, patient.getNews2Score() != null ? patient.getNews2Score() : 0);
            stmt.setString(37, patient.getAllergies());
            stmt.setString(38, patient.getKnownConditions());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating patient failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    patient.setPatientId(id);
                    return id;
                } else {
                    throw new DatabaseOperationException("Creating patient failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error inserting patient record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Patient patient) throws DatabaseOperationException {
        String sql = "UPDATE patients SET full_name = ?, age = ?, gender = ?, phone = ?, symptoms = ?, " +
                "emergency_level = ?, status = ?, spo2 = ?, heart_rate = ?, temperature = ?, systolic_bp = ?, " +
                "diastolic_bp = ?, respiratory_rate = ?, assigned_specialty = ?, assigned_doctor_id = ?, " +
                "assigned_doctor_name = ?, ai_suggested_level = ?, rule_suggested_level = ?, override_reason = ?, " +
                "escalated = ?, escalation_reason = ?, assigned_bay = ? WHERE patient_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patient.getFullName());
            stmt.setInt(2, patient.getAge());
            stmt.setString(3, patient.getGender());
            stmt.setString(4, patient.getPhone());
            stmt.setString(5, patient.getSymptoms());
            stmt.setString(6, patient.getEmergencyLevel().name());
            stmt.setString(7, patient.getStatus().name());

            Vitals v = patient.getVitals();
            if (v != null && v.getSpo2() != null) stmt.setInt(8, v.getSpo2()); else stmt.setNull(8, Types.INTEGER);
            if (v != null && v.getHeartRate() != null) stmt.setInt(9, v.getHeartRate()); else stmt.setNull(9, Types.INTEGER);
            if (v != null && v.getTemperature() != null) stmt.setDouble(10, v.getTemperature()); else stmt.setNull(10, Types.DOUBLE);
            if (v != null && v.getSystolicBp() != null) stmt.setInt(11, v.getSystolicBp()); else stmt.setNull(11, Types.INTEGER);
            if (v != null && v.getDiastolicBp() != null) stmt.setInt(12, v.getDiastolicBp()); else stmt.setNull(12, Types.INTEGER);
            if (v != null && v.getRespiratoryRate() != null) stmt.setInt(13, v.getRespiratoryRate()); else stmt.setNull(13, Types.INTEGER);

            stmt.setString(14, patient.getAssignedSpecialty() != null ? patient.getAssignedSpecialty().name() : Specialty.GENERAL_EMERGENCY.name());
            if (patient.getAssignedDoctorId() != null) stmt.setInt(15, patient.getAssignedDoctorId()); else stmt.setNull(15, Types.INTEGER);
            stmt.setString(16, patient.getAssignedDoctorName());
            stmt.setString(17, patient.getAiSuggestedLevel() != null ? patient.getAiSuggestedLevel().name() : null);
            stmt.setString(18, patient.getRuleSuggestedLevel() != null ? patient.getRuleSuggestedLevel().name() : null);
            stmt.setString(19, patient.getOverrideReason());
            stmt.setBoolean(20, patient.isEscalated());
            stmt.setString(21, patient.getEscalationReason());
            stmt.setString(22, patient.getAssignedBay());
            stmt.setInt(23, patient.getPatientId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating patient record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(int patientId, PatientStatus newStatus) throws DatabaseOperationException {
        String sql = "UPDATE patients SET status = ? WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setInt(2, patientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating patient status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Patient> findById(int patientId) throws DatabaseOperationException {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding patient by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByToken(String tokenNo) throws DatabaseOperationException {
        String sql = "SELECT * FROM patients WHERE token_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tokenNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding patient by token: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Patient> findByStatus(PatientStatus status) throws DatabaseOperationException {
        String sql = "SELECT * FROM patients WHERE status = ? ORDER BY arrival_time ASC";
        List<Patient> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding patients by status: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Patient> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM patients ORDER BY arrival_time DESC";
        List<Patient> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToPatient(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all patients: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Patient> search(String searchTerm, String emergencyLevel, String status) throws DatabaseOperationException {
        StringBuilder sql = new StringBuilder("SELECT * FROM patients WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            sql.append("AND (LOWER(full_name) LIKE ? OR LOWER(token_no) LIKE ? OR phone LIKE ?) ");
            String term = "%" + searchTerm.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (emergencyLevel != null && !emergencyLevel.equalsIgnoreCase("ALL") && !emergencyLevel.trim().isEmpty()) {
            sql.append("AND emergency_level = ? ");
            params.add(emergencyLevel.trim().toUpperCase());
        }

        if (status != null && !status.equalsIgnoreCase("ALL") && !status.trim().isEmpty()) {
            sql.append("AND status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY arrival_time DESC");

        List<Patient> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error searching patient records: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int countByStatus(PatientStatus status) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting patients by status: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int countCriticalWaiting() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = 'WAITING' AND emergency_level = 'CRITICAL'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting critical patients: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int countCompletedToday() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = 'COMPLETED' AND DATE(updated_at) = CURRENT_DATE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting completed patients: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int getMaxTokenSequence() throws DatabaseOperationException {
        String sql = "SELECT token_no FROM patients ORDER BY patient_id DESC LIMIT 50";
        int maxSeq = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String token = rs.getString("token_no");
                if (token != null && token.contains("-")) {
                    String[] parts = token.split("-");
                    if (parts.length >= 2) {
                        try {
                            int val = Integer.parseInt(parts[parts.length - 1]);
                            if (val > maxSeq) {
                                maxSeq = val;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            return maxSeq;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving max token sequence: " + e.getMessage(), e);
        }
    }

    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        int id = rs.getInt("patient_id");
        String token = rs.getString("token_no");
        String name = rs.getString("full_name");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        String phone = rs.getString("phone");
        String symptoms = rs.getString("symptoms");
        EmergencyLevel level = EmergencyLevel.fromString(rs.getString("emergency_level"));
        Timestamp arrTs = rs.getTimestamp("arrival_time");
        LocalDateTime arrTime = arrTs != null ? arrTs.toLocalDateTime() : LocalDateTime.now();
        PatientStatus status = PatientStatus.fromString(rs.getString("status"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;

        Patient p = new Patient(id, token, name, age, gender, phone, symptoms, level, arrTime, status, createdAt, updatedAt);

        // Safe extraction of HEPS 2.0 fields
        Vitals v = new Vitals();
        if (hasColumn(rs, "spo2")) {
            int spo2 = rs.getInt("spo2");
            if (!rs.wasNull()) v.setSpo2(spo2);
        }
        if (hasColumn(rs, "heart_rate")) {
            int hr = rs.getInt("heart_rate");
            if (!rs.wasNull()) v.setHeartRate(hr);
        }
        if (hasColumn(rs, "temperature")) {
            double temp = rs.getDouble("temperature");
            if (!rs.wasNull()) v.setTemperature(temp);
        }
        if (hasColumn(rs, "systolic_bp")) {
            int sys = rs.getInt("systolic_bp");
            if (!rs.wasNull()) v.setSystolicBp(sys);
        }
        if (hasColumn(rs, "diastolic_bp")) {
            int dia = rs.getInt("diastolic_bp");
            if (!rs.wasNull()) v.setDiastolicBp(dia);
        }
        if (hasColumn(rs, "respiratory_rate")) {
            int rr = rs.getInt("respiratory_rate");
            if (!rs.wasNull()) v.setRespiratoryRate(rr);
        }
        p.setVitals(v);

        if (hasColumn(rs, "assigned_specialty")) {
            String spec = rs.getString("assigned_specialty");
            if (spec != null) p.setAssignedSpecialty(Specialty.fromString(spec));
        }
        if (hasColumn(rs, "assigned_doctor_id")) {
            int docId = rs.getInt("assigned_doctor_id");
            if (!rs.wasNull()) p.setAssignedDoctorId(docId);
        }
        if (hasColumn(rs, "assigned_doctor_name")) {
            p.setAssignedDoctorName(rs.getString("assigned_doctor_name"));
        }
        if (hasColumn(rs, "ai_suggested_level")) {
            String aiLvl = rs.getString("ai_suggested_level");
            if (aiLvl != null) p.setAiSuggestedLevel(EmergencyLevel.fromString(aiLvl));
        }
        if (hasColumn(rs, "rule_suggested_level")) {
            String ruleLvl = rs.getString("rule_suggested_level");
            if (ruleLvl != null) p.setRuleSuggestedLevel(EmergencyLevel.fromString(ruleLvl));
        }
        if (hasColumn(rs, "override_reason")) {
            p.setOverrideReason(rs.getString("override_reason"));
        }
        if (hasColumn(rs, "escalated")) {
            p.setEscalated(rs.getBoolean("escalated"));
        }
        if (hasColumn(rs, "escalation_reason")) {
            p.setEscalationReason(rs.getString("escalation_reason"));
        }
        if (hasColumn(rs, "assigned_bay")) {
            String bay = rs.getString("assigned_bay");
            if (bay != null && !bay.trim().isEmpty()) {
                p.setAssignedBay(bay);
            }
        }

        if (hasColumn(rs, "uhid")) {
            String uhid = rs.getString("uhid");
            if (uhid != null && !uhid.trim().isEmpty()) p.setUhid(uhid);
        }
        if (hasColumn(rs, "dob")) p.setDateOfBirth(rs.getString("dob"));
        if (hasColumn(rs, "address")) p.setAddress(rs.getString("address"));
        if (hasColumn(rs, "blood_group")) p.setBloodGroup(rs.getString("blood_group"));
        if (hasColumn(rs, "emergency_contact_name")) p.setEmergencyContactName(rs.getString("emergency_contact_name"));
        if (hasColumn(rs, "emergency_contact_relation")) p.setEmergencyContactRelation(rs.getString("emergency_contact_relation"));
        if (hasColumn(rs, "emergency_contact_phone")) p.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        if (hasColumn(rs, "arrival_mode")) p.setArrivalMode(rs.getString("arrival_mode"));
        if (hasColumn(rs, "mlc")) p.setMlc(rs.getBoolean("mlc"));
        if (hasColumn(rs, "gcs")) {
            int gcs = rs.getInt("gcs");
            if (!rs.wasNull()) p.setGcs(gcs);
        }
        if (hasColumn(rs, "pain_score")) {
            int pain = rs.getInt("pain_score");
            if (!rs.wasNull()) p.setPainScore(pain);
        }
        if (hasColumn(rs, "news2_score")) {
            int news2 = rs.getInt("news2_score");
            if (!rs.wasNull()) p.setNews2Score(news2);
        }
        if (hasColumn(rs, "allergies")) p.setAllergies(rs.getString("allergies"));
        if (hasColumn(rs, "known_conditions")) p.setKnownConditions(rs.getString("known_conditions"));
        if (hasColumn(rs, "discharge_diagnosis")) p.setDischargeDiagnosis(rs.getString("discharge_diagnosis"));
        if (hasColumn(rs, "discharge_medications")) p.setDischargeMedications(rs.getString("discharge_medications"));
        if (hasColumn(rs, "discharge_follow_up")) p.setDischargeFollowUpDate(rs.getString("discharge_follow_up"));
        if (hasColumn(rs, "discharge_advice")) p.setDischargeAdvice(rs.getString("discharge_advice"));
        if (hasColumn(rs, "discharge_outcome")) p.setDischargeOutcome(rs.getString("discharge_outcome"));

        return p;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            for (int x = 1; x <= columns; x++) {
                if (columnName.equalsIgnoreCase(rsmd.getColumnName(x)) ||
                    columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
                    return true;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }
}
