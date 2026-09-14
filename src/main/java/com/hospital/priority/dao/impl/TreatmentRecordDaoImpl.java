package com.hospital.priority.dao.impl;

import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.TreatmentRecord;
import com.hospital.priority.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TreatmentRecordDao.
 */
public class TreatmentRecordDaoImpl implements TreatmentRecordDao {

    @Override
    public int insert(TreatmentRecord record) throws DatabaseOperationException {
        String sql = "INSERT INTO treatment_records (patient_id, doctor_name, treatment_notes, admitted_at, completed_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, record.getPatientId());
            stmt.setString(2, record.getDoctorName() != null ? record.getDoctorName() : "Attending ER Doctor");
            stmt.setString(3, record.getTreatmentNotes());
            stmt.setTimestamp(4, Timestamp.valueOf(record.getAdmittedAt() != null ? record.getAdmittedAt() : LocalDateTime.now()));
            if (record.getCompletedAt() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(record.getCompletedAt()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Inserting treatment record failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    record.setRecordId(id);
                    return id;
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error inserting treatment record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean completeTreatment(int recordId, String doctorName, String treatmentNotes) throws DatabaseOperationException {
        String sql = "UPDATE treatment_records SET doctor_name = ?, treatment_notes = ?, completed_at = ? WHERE record_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, doctorName);
            stmt.setString(2, treatmentNotes);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, recordId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error completing treatment record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean completeTreatmentByPatientId(int patientId, String doctorName, String treatmentNotes) throws DatabaseOperationException {
        String sql = "UPDATE treatment_records SET doctor_name = ?, treatment_notes = ?, completed_at = ? " +
                     "WHERE patient_id = ? AND completed_at IS NULL ORDER BY record_id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, doctorName);
            stmt.setString(2, treatmentNotes);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, patientId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return true;
            }

            // If no active record existed yet, create a completed one directly
            TreatmentRecord newRec = new TreatmentRecord(patientId, doctorName, treatmentNotes, LocalDateTime.now());
            newRec.setCompletedAt(LocalDateTime.now());
            insert(newRec);
            return true;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating treatment record by patient: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TreatmentRecord> findById(int recordId) throws DatabaseOperationException {
        String sql = "SELECT * FROM treatment_records WHERE record_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, recordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding treatment record by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TreatmentRecord> findByPatientId(int patientId) throws DatabaseOperationException {
        String sql = "SELECT * FROM treatment_records WHERE patient_id = ? ORDER BY record_id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding treatment record by patient ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TreatmentRecord> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM treatment_records ORDER BY admitted_at DESC";
        List<TreatmentRecord> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching treatment records: " + e.getMessage(), e);
        }
    }

    private TreatmentRecord mapResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("record_id");
        int patientId = rs.getInt("patient_id");
        String doctor = rs.getString("doctor_name");
        String notes = rs.getString("treatment_notes");

        Timestamp admTs = rs.getTimestamp("admitted_at");
        LocalDateTime admittedAt = admTs != null ? admTs.toLocalDateTime() : null;

        Timestamp compTs = rs.getTimestamp("completed_at");
        LocalDateTime completedAt = compTs != null ? compTs.toLocalDateTime() : null;

        return new TreatmentRecord(id, patientId, doctor, notes, admittedAt, completedAt);
    }
}
