package com.growthloops.workflow.service;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;
import com.growthloops.workflow.domain.StepDefinition;
import com.growthloops.workflow.domain.StepExecution;
import com.growthloops.workflow.domain.Workflow;
import com.growthloops.workflow.dto.CreateWorkflowRequest;
import com.growthloops.workflow.engine.CancellationRegistry;
import com.growthloops.workflow.engine.CancellationToken;
import com.growthloops.workflow.engine.WorkflowEngine;
import com.growthloops.workflow.exception.ExecutionNotCancellableException;
import com.growthloops.workflow.repository.ExecutionRepository;
import com.growthloops.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private static final Set<ExecutionStatus> TERMINAL_STATUSES =
            Set.of(ExecutionStatus.COMPLETED, ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final WorkflowEngine engine;
    private final AsyncExecutionRunner asyncRunner;
    private final CancellationRegistry cancellationRegistry;

    public WorkflowService(WorkflowRepository workflowRepository,
                           ExecutionRepository executionRepository,
                           WorkflowEngine engine,
                           AsyncExecutionRunner asyncRunner,
                           CancellationRegistry cancellationRegistry) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.engine = engine;
        this.asyncRunner = asyncRunner;
        this.cancellationRegistry = cancellationRegistry;
    }

    public Workflow createWorkflow(CreateWorkflowRequest request) {
        Workflow workflow = new Workflow(request.getWorkflowId(), request.getSteps());
        Workflow existing = workflowRepository.saveIfAbsent(workflow);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Workflow already exists: " + request.getWorkflowId());
        }
        return workflow;
    }

    public Workflow getWorkflow(String workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Workflow not found: " + workflowId));
    }

    public Execution startExecution(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);

        Execution execution = new Execution();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setSteps(buildStepExecutions(workflow.getSteps()));
        executionRepository.save(execution);

        // Register the token BEFORE dispatching so a cancel arriving immediately
        // after this method returns can never be lost to a not-yet-registered token.
        CancellationToken token = cancellationRegistry.register(execution.getExecutionId());
        asyncRunner.run(workflow, execution, token);

        return execution.snapshot();
    }

    public Execution executeSync(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);

        Execution execution = new Execution();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setSteps(buildStepExecutions(workflow.getSteps()));
        executionRepository.save(execution);

        return engine.run(workflow, execution, cancellationRegistry.register(execution.getExecutionId()));
    }

    /**
     * Signals cancellation for a PENDING or RUNNING execution and returns the latest
     * snapshot. The engine may not have acted on the signal yet, so the status may
     * still show RUNNING briefly — poll the state endpoint to observe CANCELLED.
     * <p>
     * Throws {@link ExecutionNotCancellableException} (→ 409) if the execution is
     * already in a terminal state. If the token is gone (the execution finished
     * between the status check and the signal), the latest terminal snapshot is
     * returned without error.
     */
    public Execution cancelExecution(String executionId) {
        Execution execution = getExecution(executionId);

        if (TERMINAL_STATUSES.contains(execution.getStatus())) {
            throw new ExecutionNotCancellableException(
                    "Cannot cancel execution " + executionId
                            + " — already in terminal state: " + execution.getStatus());
        }

        cancellationRegistry.requestCancellation(executionId);
        return getExecution(executionId);
    }

    public Execution getExecution(String executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Execution not found: " + executionId));
    }

    public List<Execution> getExecutionsForWorkflow(String workflowId) {
        getWorkflow(workflowId);
        return executionRepository.findByWorkflowId(workflowId);
    }

    private List<StepExecution> buildStepExecutions(List<StepDefinition> defs) {
        return defs.stream()
                .map(d -> new StepExecution(d.getName()))
                .collect(Collectors.toList());
    }
}