package com.hospital.priority.dao.impl;

import com.hospital.priority.dao.DoctorDao;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Doctor;
import com.hospital.priority.model.DoctorStatus;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JDBC Implementation of DoctorDao with fail-safe in-memory cache.
 * Ensures the emergency hospital routing system always has active doctors
 * even during initial offline database setup.
 */
public class DoctorDaoImpl implements DoctorDao {

    // In-memory fallback roster for high resilience
    private static final List<Doctor> IN_MEMORY_DOCTORS = new CopyOnWriteArrayList<>();

    static {
        IN_MEMORY_DOCTORS.add(new Doctor(1, "Dr. Rajesh Sharma", Specialty.CARDIOLOGY, DoctorStatus.AVAILABLE, 1, "Cardio Lab 1", "+91 98111 22233"));
        IN_MEMORY_DOCTORS.add(new Doctor(2, "Dr. Priya Nair", Specialty.NEUROLOGY, DoctorStatus.AVAILABLE, 0, "Neuro ICU 2", "+91 98222 33344"));
        IN_MEMORY_DOCTORS.add(new Doctor(3, "Dr. Vikram Patel", Specialty.ORTHOPEDICS, DoctorStatus.AVAILABLE, 2, "Ortho Suite 3", "+91 98333 44455"));
        IN_MEMORY_DOCTORS.add(new Doctor(4, "Dr. Ananya Sen", Specialty.PULMONOLOGY, DoctorStatus.AVAILABLE, 1, "Pulmo Bay 4", "+91 98444 55566"));
        IN_MEMORY_DOCTORS.add(new Doctor(5, "Dr. Sneha Reddy", Specialty.PEDIATRICS, DoctorStatus.AVAILABLE, 0, "Pedia Unit 5", "+91 98555 66677"));
        IN_MEMORY_DOCTORS.add(new Doctor(6, "Dr. Arjun Mehta", Specialty.TRAUMA_SURGERY, DoctorStatus.AVAILABLE, 1, "Trauma OT 1", "+91 98666 77788"));
        IN_MEMORY_DOCTORS.add(new Doctor(7, "Dr. David Miller", Specialty.GENERAL_EMERGENCY, DoctorStatus.AVAILABLE, 1, "ER Station A", "+91 98777 88899"));
        IN_MEMORY_DOCTORS.add(new Doctor(8, "Dr. Kavita Verma", Specialty.GENERAL_EMERGENCY, DoctorStatus.AVAILABLE, 0, "ER Station B", "+91 98888 99900"));
    }

    @Override
    public List<Doctor> findAll() throws DatabaseOperationException {
        String sql = "SELECT doctor_id, name, specialty, status, current_workload, room_number, phone FROM doctors ORDER BY name ASC";
        List<Doctor> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDoctor(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            System.err.println("Notice: doctors table query failed (" + e.getMessage() + "), using in-memory doctors.");
        }

        return new ArrayList<>(IN_MEMORY_DOCTORS);
    }

    @Override
    public Optional<Doctor> findById(int doctorId) throws DatabaseOperationException {
        String sql = "SELECT doctor_id, name, specialty, status, current_workload, room_number, phone FROM doctors WHERE doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDoctor(rs));
                }
            }
        } catch (Exception e) {
            // Fallback to in-memory
        }

        return IN_MEMORY_DOCTORS.stream()
                .filter(d -> d.getDoctorId() == doctorId)
                .findFirst();
    }

    @Override
    public List<Doctor> findBySpecialty(Specialty specialty) throws DatabaseOperationException {
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if (d.getSpecialty() == specialty) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public List<Doctor> findAvailableBySpecialty(Specialty specialty) throws DatabaseOperationException {
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if (d.getSpecialty() == specialty && d.getStatus() == DoctorStatus.AVAILABLE) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public List<Doctor> findAvailable() throws DatabaseOperationException {
        List<Doctor> all = findAll();
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : all) {
            if (d.getStatus() == DoctorStatus.AVAILABLE) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    @Override
    public void updateStatus(int doctorId, DoctorStatus status) throws DatabaseOperationException {
        String sql = "UPDATE doctors SET status = ? WHERE doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, doctorId);
            stmt.executeUpdate();
        } catch (Exception e) {
            // update in-memory
        }

        for (Doctor d : IN_MEMORY_DOCTORS) {
            if (d.getDoctorId() == doctorId) {
                d.setStatus(status);
                break;
            }
        }
    }

    @Override
    public void updateWorkload(int doctorId, int workload) throws DatabaseOperationException {
        String sql = "UPDATE doctors SET current_workload = ? WHERE doctor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workload);
            stmt.setInt(2, doctorId);
            stmt.executeUpdate();
        } catch (Exception e) {
            // update in-memory
        }

        for (Doctor d : IN_MEMORY_DOCTORS) {
            if (d.getDoctorId() == doctorId) {
                d.setCurrentWorkload(workload);
                break;
            }
        }
    }

    @Override
    public int insert(Doctor doctor) throws DatabaseOperationException {
        String sql = "INSERT INTO doctors (name, specialty, status, current_workload, room_number, phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, doctor.getName());
            stmt.setString(2, doctor.getSpecialty().name());
            stmt.setString(3, doctor.getStatus().name());
            stmt.setInt(4, doctor.getCurrentWorkload());
            stmt.setString(5, doctor.getRoomNumber());
            stmt.setString(6, doctor.getPhone());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    doctor.setDoctorId(id);
                    IN_MEMORY_DOCTORS.add(doctor);
                    return id;
                }
            }
        } catch (Exception e) {
            // fallback in-memory insert
        }

        int nextId = IN_MEMORY_DOCTORS.size() + 1;
        doctor.setDoctorId(nextId);
        IN_MEMORY_DOCTORS.add(doctor);
        return nextId;
    }

    private Doctor mapResultSetToDoctor(ResultSet rs) throws SQLException {
        int id = rs.getInt("doctor_id");
        String name = rs.getString("name");
        Specialty specialty = Specialty.fromString(rs.getString("specialty"));
        DoctorStatus status = DoctorStatus.fromString(rs.getString("status"));
        int workload = rs.getInt("current_workload");
        String room = rs.getString("room_number");
        String phone = rs.getString("phone");

        return new Doctor(id, name, specialty, status, workload, room, phone);
    }
}
