package com.growthloops.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a {@link CancellationToken} per in-flight execution so a cancel request
 * arriving on an HTTP thread can signal the worker thread running the workflow.
 * Tokens are registered when an execution starts and removed when it reaches a
 * terminal state.
 */
@Component
public class CancellationRegistry {

    private final ConcurrentHashMap<String, CancellationToken> tokens = new ConcurrentHashMap<>();

    public CancellationToken register(String executionId) {
        CancellationToken token = new CancellationToken();
        tokens.put(executionId, token);
        return token;
    }

    /**
     * Signals cancellation for the given execution.
     *
     * @return {@code true} if a token was found and signalled, {@code false} if no
     *         token exists (execution already finished or was never registered).
     */
    public boolean requestCancellation(String executionId) {
        CancellationToken token = tokens.get(executionId);
        if (token == null) {
            return false;
        }
        token.cancel();
        return true;
    }

    public void deregister(String executionId) {
        tokens.remove(executionId);
    }
}
