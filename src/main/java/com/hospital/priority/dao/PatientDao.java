package com.hospital.priority.dao;

import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Patient operations.
 * Demonstrates OOP Abstraction by isolating database persistence mechanisms.
 */
public interface PatientDao {

    int insert(Patient patient) throws DatabaseOperationException;

    boolean update(Patient patient) throws DatabaseOperationException;

    boolean updateStatus(int patientId, PatientStatus newStatus) throws DatabaseOperationException;

    Optional<Patient> findById(int patientId) throws DatabaseOperationException;

    Optional<Patient> findByToken(String tokenNo) throws DatabaseOperationException;

    Optional<Patient> findByPhone(String phone) throws DatabaseOperationException;

    List<Patient> findByStatus(PatientStatus status) throws DatabaseOperationException;

    List<Patient> findAll() throws DatabaseOperationException;

    List<Patient> search(String searchTerm, String emergencyLevel, String status) throws DatabaseOperationException;

    int countByStatus(PatientStatus status) throws DatabaseOperationException;

    int countCriticalWaiting() throws DatabaseOperationException;

    int countCompletedToday() throws DatabaseOperationException;

    int getMaxTokenSequence() throws DatabaseOperationException;
}
