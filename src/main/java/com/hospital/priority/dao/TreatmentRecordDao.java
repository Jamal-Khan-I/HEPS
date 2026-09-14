package com.hospital.priority.dao;

import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.TreatmentRecord;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Treatment Record operations.
 */
public interface TreatmentRecordDao {

    int insert(TreatmentRecord record) throws DatabaseOperationException;

    boolean completeTreatment(int recordId, String doctorName, String treatmentNotes) throws DatabaseOperationException;

    boolean completeTreatmentByPatientId(int patientId, String doctorName, String treatmentNotes) throws DatabaseOperationException;

    Optional<TreatmentRecord> findById(int recordId) throws DatabaseOperationException;

    Optional<TreatmentRecord> findByPatientId(int patientId) throws DatabaseOperationException;

    List<TreatmentRecord> findAll() throws DatabaseOperationException;
}
