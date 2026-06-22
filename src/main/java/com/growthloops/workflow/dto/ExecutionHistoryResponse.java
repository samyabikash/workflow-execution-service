package com.growthloops.workflow.dto;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;
import com.growthloops.workflow.domain.StepExecution;

import java.time.Instant;
import java.util.List;

/**
 * Full execution history including step-level details.
 */
public class ExecutionHistoryResponse {

    private final String executionId;
    private final String workflowId;
    private final ExecutionStatus status;
    private final Instant createdAt;
    private final Instant finishedAt;
    private final List<StepExecution> steps;

    public ExecutionHistoryResponse(Execution execution) {
        this.executionId = execution.getExecutionId();
        this.workflowId = execution.getWorkflowId();
        this.status = execution.getStatus();
        this.createdAt = execution.getCreatedAt();
        this.finishedAt = execution.getFinishedAt();
        this.steps = execution.getSteps();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public List<StepExecution> getSteps() {
        return steps;
    }
}
