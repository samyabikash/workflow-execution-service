package com.growthloops.workflow.api;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.Workflow;
import com.growthloops.workflow.dto.CreateWorkflowRequest;
import com.growthloops.workflow.dto.ExecutionHistoryResponse;
import com.growthloops.workflow.dto.ExecutionStateResponse;
import com.growthloops.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    // 1. Create Workflow
    @PostMapping("/workflows")
    public ResponseEntity<Workflow> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow created = service.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/workflows/{workflowId}")
    public Workflow getWorkflow(@PathVariable String workflowId) {
        return service.getWorkflow(workflowId);
    }

    // 2. Execute Workflow
    @PostMapping("/workflows/{workflowId}/executions")
    public ResponseEntity<ExecutionHistoryResponse> execute(@PathVariable String workflowId) {
        Execution execution = service.execute(workflowId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ExecutionHistoryResponse(execution));
    }

    // 3. Track Execution State (lightweight)
    @GetMapping("/executions/{executionId}/state")
    public ExecutionStateResponse getState(@PathVariable String executionId) {
        return new ExecutionStateResponse(service.getExecution(executionId));
    }

    // 4. Execution History (full, step-level)
    @GetMapping("/executions/{executionId}")
    public ExecutionHistoryResponse getExecution(@PathVariable String executionId) {
        return new ExecutionHistoryResponse(service.getExecution(executionId));
    }

    // All executions for a workflow
    @GetMapping("/workflows/{workflowId}/executions")
    public List<ExecutionHistoryResponse> getExecutionsForWorkflow(@PathVariable String workflowId) {
        return service.getExecutionsForWorkflow(workflowId).stream()
                .map(ExecutionHistoryResponse::new)
                .toList();
    }
}
