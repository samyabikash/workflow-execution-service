// src/main/java/com/growthloops/workflow/service/WorkflowService.java
package com.growthloops.workflow.service;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.StepDefinition;
import com.growthloops.workflow.domain.StepExecution;
import com.growthloops.workflow.domain.Workflow;
import com.growthloops.workflow.dto.CreateWorkflowRequest;
import com.growthloops.workflow.engine.WorkflowEngine;
import com.growthloops.workflow.repository.ExecutionRepository;
import com.growthloops.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final WorkflowEngine engine;
    private final AsyncExecutionRunner asyncRunner; // ← inject the dedicated runner

    public WorkflowService(WorkflowRepository workflowRepository,
                           ExecutionRepository executionRepository,
                           WorkflowEngine engine,
                           AsyncExecutionRunner asyncRunner) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.engine = engine;
        this.asyncRunner = asyncRunner;
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

    /**
     * Registers a new PENDING execution, persists it, fires it asynchronously
     * via the proxy-aware AsyncExecutionRunner, and immediately returns the
     * PENDING snapshot to the caller (202 Accepted).
     */
    public Execution startExecution(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);

        Execution execution = new Execution();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setSteps(buildStepExecutions(workflow.getSteps()));
        executionRepository.save(execution); // persist PENDING before launching

        // Called on a DIFFERENT bean → Spring proxy intercepts → truly async
        asyncRunner.run(workflow, execution);

        return execution.snapshot(); // returns PENDING to the HTTP caller
    }

    /**
     * Synchronous variant — retained for tests and callers that want to block.
     */
    public Execution executeSync(String workflowId) {
        Workflow workflow = getWorkflow(workflowId);

        Execution execution = new Execution();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowId(workflowId);
        execution.setSteps(buildStepExecutions(workflow.getSteps()));
        executionRepository.save(execution);

        return engine.run(workflow, execution);
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