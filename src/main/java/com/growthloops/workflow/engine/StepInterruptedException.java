package com.growthloops.workflow.engine;

/**
 * Thrown by a {@link StepHandler} when its work is interrupted (e.g. a blocking
 * {@code delay} woken by {@link Thread#interrupt()}). The engine treats this as a
 * cancellation signal rather than a genuine step failure.
 */
public class StepInterruptedException extends RuntimeException {

    public StepInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
