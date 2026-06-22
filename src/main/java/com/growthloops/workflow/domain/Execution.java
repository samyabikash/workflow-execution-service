package com.growthloops.workflow.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime state of a single workflow execution instance.
 */
public class Execution {

    private String executionId;
    private String workflowId;
    private ExecutionStatus status = ExecutionStatus.PENDING;
    private int currentStep = 0;
    private List<StepExecution> steps = new ArrayList<>();
    private Map<String, Object> finalContext;
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

    public Map<String, Object> getFinalContext() {
        return finalContext;
    }

    public void setFinalContext(Map<String, Object> finalContext) {
        this.finalContext = finalContext;
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

    /**
     * Returns an immutable snapshot of this execution so external consumers never
     * observe partially-updated state while a workflow is still running.
     */
    public Execution snapshot() {
        Execution copy = new Execution();
        copy.executionId = this.executionId;
        copy.workflowId = this.workflowId;
        copy.status = this.status;
        copy.currentStep = this.currentStep;
        copy.createdAt = this.createdAt;
        copy.finishedAt = this.finishedAt;

        List<StepExecution> stepCopies = new ArrayList<>(this.steps.size());
        for (StepExecution step : this.steps) {
            stepCopies.add(step.snapshot());
        }
        copy.steps = stepCopies;

        copy.finalContext = this.finalContext == null
                ? null
                : new HashMap<>(this.finalContext);
        return copy;
    }
}
