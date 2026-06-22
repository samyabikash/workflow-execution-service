package com.growthloops.workflow.engine.handlers;

import java.util.Map;

/**
 * Fallback used by the engine when no specific handler is registered
 * for a step name. Treated as a successful no-op that echoes its input.
 * <p>
 * Not a Spring bean: it is instantiated by the engine as a default.
 */
public class DefaultStepHandler {

    public Map<String, Object> execute(String stepName, Map<String, Object> input) {
        return Map.of("handled", true, "step", stepName);
    }
}
