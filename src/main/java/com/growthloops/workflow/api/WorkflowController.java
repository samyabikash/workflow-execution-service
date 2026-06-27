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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping("/workflows")
    public ResponseEntity<Workflow> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow created = service.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/workflows/{workflowId}")
    public Workflow getWorkflow(@PathVariable String workflowId) {
        return service.getWorkflow(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/executions")
    public ResponseEntity<ExecutionHistoryResponse> execute(@PathVariable String workflowId,
                                                            UriComponentsBuilder uriBuilder) {
        Execution execution = service.startExecution(workflowId);
        URI stateUri = uriBuilder
                .path("/api/v1/executions/{executionId}/state")
                .buildAndExpand(execution.getExecutionId())
                .toUri();
        return ResponseEntity.accepted()
                .location(stateUri)
                .body(new ExecutionHistoryResponse(execution));
    }

    @GetMapping("/executions/{executionId}/state")
    public ExecutionStateResponse getState(@PathVariable String executionId) {
        return new ExecutionStateResponse(service.getExecution(executionId));
    }

    @GetMapping("/executions/{executionId}")
    public ExecutionHistoryResponse getExecution(@PathVariable String executionId) {
        return new ExecutionHistoryResponse(service.getExecution(executionId));
    }

    @GetMapping("/workflows/{workflowId}/executions")
    public List<ExecutionHistoryResponse> getExecutionsForWorkflow(@PathVariable String workflowId) {
        return service.getExecutionsForWorkflow(workflowId).stream()
                .map(ExecutionHistoryResponse::new)
                .toList();
    }

    /**
     * Cancel a running or pending execution.
     * Returns 200 with the current execution snapshot immediately after signalling.
     * The engine acts on the signal asynchronously, so status may still show
     * RUNNING for a brief moment — poll /state to confirm CANCELLED.
     * Returns 409 if the execution is already in a terminal state.
     */
    @DeleteMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionHistoryResponse> cancelExecution(
            @PathVariable String executionId) {
        Execution execution = service.cancelExecution(executionId);
        return ResponseEntity.ok(new ExecutionHistoryResponse(execution));
    }
}
