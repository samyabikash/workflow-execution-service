package com.growthloops.workflow.exception;

/**
 * Raised when a caller tries to cancel an execution that is already in a terminal
 * state (COMPLETED, FAILED, or CANCELLED). Maps to HTTP 409 Conflict.
 */
public class ExecutionNotCancellableException extends RuntimeException {

    public ExecutionNotCancellableException(String message) {
        super(message);
    }
}
