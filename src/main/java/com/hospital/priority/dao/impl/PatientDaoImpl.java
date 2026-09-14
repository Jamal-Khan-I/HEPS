package com.hospital.priority.dao.impl;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of PatientDao.
 * Demonstrates:
 * - OOP Polymorphism via interface implementation
 * - Robust PreparedStatements preventing SQL injection
 * - Resource cleanup via try-with-resources
 */
public class PatientDaoImpl implements PatientDao {

    @Override
    public int insert(Patient patient) throws DatabaseOperationException {
        String sql = "INSERT INTO patients (token_no, full_name, age, gender, phone, symptoms, " +
                     "emergency_level, arrival_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                     "emergency_level = ?, status = ? WHERE patient_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patient.getFullName());
            stmt.setInt(2, patient.getAge());
            stmt.setString(3, patient.getGender());
            stmt.setString(4, patient.getPhone());
            stmt.setString(5, patient.getSymptoms());
            stmt.setString(6, patient.getEmergencyLevel().name());
            stmt.setString(7, patient.getStatus().name());
            stmt.setInt(8, patient.getPatientId());

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
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding patient by ID: " + e.getMessage(), e);
        }
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
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding patient by token: " + e.getMessage(), e);
        }
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
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching patients by status: " + e.getMessage(), e);
        }
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
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all patients: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Patient> search(String searchTerm, String emergencyLevel, String status) throws DatabaseOperationException {
        StringBuilder sql = new StringBuilder("SELECT * FROM patients WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            sql.append(" AND (LOWER(token_no) LIKE ? OR LOWER(full_name) LIKE ? OR phone LIKE ?)");
            String likeTerm = "%" + searchTerm.trim().toLowerCase() + "%";
            params.add(likeTerm);
            params.add(likeTerm);
            params.add(likeTerm);
        }

        if (emergencyLevel != null && !emergencyLevel.trim().isEmpty() && !"ALL".equalsIgnoreCase(emergencyLevel.trim())) {
            sql.append(" AND emergency_level = ?");
            params.add(emergencyLevel.trim().toUpperCase());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append(" AND status = ?");
            params.add(status.trim().toUpperCase());
        }

        sql.append(" ORDER BY arrival_time DESC");
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
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error searching patient records: " + e.getMessage(), e);
        }
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
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting patients by status: " + e.getMessage(), e);
        }
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
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting critical waiting patients: " + e.getMessage(), e);
        }
    }

    @Override
    public int countCompletedToday() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = 'COMPLETED' AND DATE(updated_at) = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting completed patients today: " + e.getMessage(), e);
        }
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
                    if (parts.length == 3) {
                        try {
                            int val = Integer.parseInt(parts[2]);
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

        return new Patient(id, token, name, age, gender, phone, symptoms, level, arrTime, status, createdAt, updatedAt);
    }
}
