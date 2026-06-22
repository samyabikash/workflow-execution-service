package com.growthloops.workflow.engine.handlers;

import com.growthloops.workflow.engine.StepHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExecuteStepHandler implements StepHandler {

    @Override
    public String stepName() {
        return "execute";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context) {
        return Map.of("result", "success");
    }
}
