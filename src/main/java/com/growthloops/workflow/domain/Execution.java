package com.growthloops.workflow.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime state of a single workflow execution instance.
 */
public class Execution {

    private String executionId;
    private String workflowId;
    private ExecutionStatus status = ExecutionStatus.PENDING;
    private int currentStep = 0;
    private List<StepExecution> steps = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant finishedAt;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public List<StepExecution> getSteps() {
        return steps;
    }

    public void setSteps(List<StepExecution> steps) {
        this.steps = steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
