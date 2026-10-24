package com.hospital.priority.exception;

/**
 * Thrown when an invalid patient status transition is attempted.
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
