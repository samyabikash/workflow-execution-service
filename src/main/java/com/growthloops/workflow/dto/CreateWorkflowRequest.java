package com.growthloops.workflow.dto;

import com.growthloops.workflow.domain.StepDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CreateWorkflowRequest {

    @NotBlank(message = "workflowId is required")
    private String workflowId;

    @Valid
    @NotEmpty(message = "steps must contain at least one step")
    private List<StepDefinition> steps;

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<StepDefinition> getSteps() {
        return steps;
    }

    public void setSteps(List<StepDefinition> steps) {
        this.steps = steps;
    }
}
