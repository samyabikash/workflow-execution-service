package com.growthloops.workflow.domain;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable definition of a single step within a workflow.
 * "input" is optional static input declared at definition time.
 */
public class StepDefinition {

    @NotBlank(message = "step name is required")
    private String name;

    private Map<String, Object> input;

    public StepDefinition() {
    }

    public StepDefinition(String name, Map<String, Object> input) {
        this.name = name;
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input == null ? Collections.emptyMap() : input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
