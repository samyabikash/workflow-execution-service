package com.growthloops.workflow.domain;

import java.util.List;

/**
 * Stored workflow definition.
 */
public class Workflow {

    private String workflowId;
    private List<StepDefinition> steps;

    public Workflow() {
    }

    public Workflow(String workflowId, List<StepDefinition> steps) {
        this.workflowId = workflowId;
        this.steps = steps;
    }

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
