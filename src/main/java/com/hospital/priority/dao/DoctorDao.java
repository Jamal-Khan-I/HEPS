package com.hospital.priority.dao;

import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Doctor;
import com.hospital.priority.model.DoctorStatus;
import com.hospital.priority.model.Specialty;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Doctor and Specialist entity operations.
 */
public interface DoctorDao {

    List<Doctor> findAll() throws DatabaseOperationException;

    Optional<Doctor> findById(int doctorId) throws DatabaseOperationException;

    List<Doctor> findBySpecialty(Specialty specialty) throws DatabaseOperationException;

    List<Doctor> findAvailableBySpecialty(Specialty specialty) throws DatabaseOperationException;

    List<Doctor> findAvailable() throws DatabaseOperationException;

    void updateStatus(int doctorId, DoctorStatus status) throws DatabaseOperationException;

    void updateWorkload(int doctorId, int workload) throws DatabaseOperationException;

    int insert(Doctor doctor) throws DatabaseOperationException;
}
