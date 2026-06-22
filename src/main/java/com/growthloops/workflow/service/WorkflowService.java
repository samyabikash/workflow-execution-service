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

    public WorkflowService(WorkflowRepository workflowRepository,
                           ExecutionRepository executionRepository,
                           WorkflowEngine engine) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.engine = engine;
    }

    public Workflow createWorkflow(CreateWorkflowRequest request) {
        if (workflowRepository.exists(request.getWorkflowId())) {
            throw new IllegalArgumentException(
                    "Workflow already exists: " + request.getWorkflowId());
        }
        Workflow workflow = new Workflow(request.getWorkflowId(), request.getSteps());
        return workflowRepository.save(workflow);
    }

    public Workflow getWorkflow(String workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Workflow not found: " + workflowId));
    }

    /**
     * Executes synchronously. For the scope of this exercise a synchronous
     * model keeps the API and state transitions easy to reason about.
     */
    public Execution execute(String workflowId) {
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
        // Validate the workflow exists for a clearer 404.
        getWorkflow(workflowId);
        return executionRepository.findByWorkflowId(workflowId);
    }

    private List<StepExecution> buildStepExecutions(List<StepDefinition> defs) {
        return defs.stream()
                .map(d -> new StepExecution(d.getName()))
                .collect(Collectors.toList());
    }
}
