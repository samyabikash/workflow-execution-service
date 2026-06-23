package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DelayStepHandler implements StepHandler {

    /**
     * When set to a positive value, this overrides whatever durationMillis
     * the workflow definition supplies. Useful for local demos where you want
     * to observe the RUNNING state without editing the Postman collection.
     * Set to 0 in application.yml (or leave absent) to use the workflow-defined value.
     */
    @Value("${workflow.demo.delay-millis:0}")
    private long demoDelayMillis;

    @Override
    public String stepName() {
        return "delay";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        long durationMillis = demoDelayMillis > 0
                ? demoDelayMillis
                : parseDuration(input.get("durationMillis"));

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