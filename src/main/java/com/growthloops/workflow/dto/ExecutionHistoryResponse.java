package com.growthloops.workflow.dto;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;
import com.growthloops.workflow.domain.StepExecution;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Full execution history including step-level details and the final context.
 */
public class ExecutionHistoryResponse {

    private final String executionId;
    private final String workflowId;
    private final ExecutionStatus status;
    private final int currentStep;
    private final Instant createdAt;
    private final Instant finishedAt;
    private final List<StepExecution> steps;
    private final Map<String, Object> finalContext;

    public ExecutionHistoryResponse(Execution execution) {
        this.executionId = execution.getExecutionId();
        this.workflowId = execution.getWorkflowId();
        this.status = execution.getStatus();
        this.currentStep = execution.getCurrentStep();
        this.createdAt = execution.getCreatedAt();
        this.finishedAt = execution.getFinishedAt();
        this.steps = execution.getSteps();
        this.finalContext = execution.getFinalContext();
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public List<StepExecution> getSteps() {
        return steps;
    }

    public Map<String, Object> getFinalContext() {
        return finalContext;
    }
}
