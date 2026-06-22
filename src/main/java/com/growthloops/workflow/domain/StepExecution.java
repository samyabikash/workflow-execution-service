package com.growthloops.workflow.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Runtime state of a single step during an execution.
 */
public class StepExecution {

    private String name;
    private ExecutionStatus status = ExecutionStatus.PENDING;
    private Map<String, Object> output;
    private String error;
    private Instant startedAt;
    private Instant finishedAt;

    public StepExecution() {
    }

    public StepExecution(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
