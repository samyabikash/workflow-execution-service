package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidateStepHandler implements StepHandler {

    @Override
    public String stepName() {
        return "validate";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        // Demonstration logic: a contract is "valid" if a contractId was supplied.
        boolean valid = input.get("contractId") != null;
        return Map.of("valid", valid);
    }
}
