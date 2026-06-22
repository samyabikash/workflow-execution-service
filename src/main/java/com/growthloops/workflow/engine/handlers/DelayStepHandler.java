package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pauses execution for a configured duration.
 * <p>
 * Supported input configuration:
 * - "durationMillis" : number of milliseconds to pause (required, must be &gt;= 0)
 */
@Component
public class DelayStepHandler implements StepHandler {

    @Override
    public String stepName() {
        return "delay";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        long durationMillis = parseDuration(input.get("durationMillis"));
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("delay: interrupted while waiting", e);
        }
        return Map.of("delayedMillis", durationMillis);
    }

    private long parseDuration(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("delay: 'durationMillis' is required");
        }
        long value;
        if (raw instanceof Number n) {
            value = n.longValue();
        } else {
            try {
                value = Long.parseLong(String.valueOf(raw));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "delay: 'durationMillis' must be a number, got: " + raw);
            }
        }
        if (value < 0) {
            throw new IllegalArgumentException(
                    "delay: 'durationMillis' must be non-negative, got: " + value);
        }
        return value;
    }
}
