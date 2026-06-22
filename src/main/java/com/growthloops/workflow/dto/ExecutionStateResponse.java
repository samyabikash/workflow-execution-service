package com.growthloops.workflow.dto;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;

/**
 * Lightweight projection used by the "track execution state" endpoint.
 */
public class ExecutionStateResponse {

    private final String executionId;
    private final ExecutionStatus status;
    private final int currentStep;

    public ExecutionStateResponse(Execution execution) {
        this.executionId = execution.getExecutionId();
        this.status = execution.getStatus();
        this.currentStep = execution.getCurrentStep();
    }

    public String getExecutionId() {
        return executionId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }
}
