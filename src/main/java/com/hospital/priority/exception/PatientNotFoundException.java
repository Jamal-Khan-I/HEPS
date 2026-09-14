package com.hospital.priority.exception;

/**
 * Thrown when a patient record or token cannot be found in the queue or database.
 */
public class PatientNotFoundException extends Exception {
    public PatientNotFoundException(String message) {
        super(message);
    }

    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
