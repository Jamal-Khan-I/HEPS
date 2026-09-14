package com.hospital.priority.exception;

/**
 * Thrown when a JDBC database persistence operation fails.
 */
public class DatabaseOperationException extends Exception {
    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
