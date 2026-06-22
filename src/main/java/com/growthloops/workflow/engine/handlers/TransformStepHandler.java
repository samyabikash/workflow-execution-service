package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic transform step.
 * <p>
 * Reads a source key from the step input (or, if absent there, from the shared
 * workflow context), applies a simple transformation, and writes the result back
 * under a target key so later steps can consume it.
 * <p>
 * Supported input configuration:
 * - "sourceKey"   : name of the value to read (default "value")
 * - "targetKey"   : name to write the result under (default "transformed")
 * - "operation"   : one of upper | lower | trim | toString (default "toString")
 */
@Component
public class TransformStepHandler implements StepHandler {

    @Override
    public String stepName() {
        return "transform";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        String sourceKey = stringOrDefault(input.get("sourceKey"), "value");
        String targetKey = stringOrDefault(input.get("targetKey"), "transformed");
        String operation = stringOrDefault(input.get("operation"), "toString");

        // Prefer step input, then fall back to the accumulated context.
        Object source = input.containsKey(sourceKey) ? input.get(sourceKey) : context.get(sourceKey);
        if (source == null) {
            throw new IllegalArgumentException(
                    "transform: no value found for sourceKey '" + sourceKey + "'");
        }

        Object result = apply(operation, source);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(targetKey, result);
        return output;
    }

    private Object apply(String operation, Object source) {
        String text = String.valueOf(source);
        return switch (operation) {
            case "upper" -> text.toUpperCase();
            case "lower" -> text.toLowerCase();
            case "trim" -> text.trim();
            case "toString" -> text;
            default -> throw new IllegalArgumentException(
                    "transform: unsupported operation '" + operation + "'");
        };
    }

    private String stringOrDefault(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
