package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ApproveStepHandler implements StepHandler {

    @Override
    public String stepName() {
        return "approve";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        // A step can consume prior step output from the context.
        Object valid = context.get("valid");
        if (Boolean.FALSE.equals(valid)) {
            throw new IllegalStateException("Cannot approve an invalid contract");
        }
        return Map.of("approvedBy", "system");
    }
}
