package com.hospital.priority.exception;

/**
 * Thrown when patient registration or update contains invalid medical or personal details.
 */
public class InvalidPatientDataException extends Exception {
    public InvalidPatientDataException(String message) {
        super(message);
    }

    public InvalidPatientDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
