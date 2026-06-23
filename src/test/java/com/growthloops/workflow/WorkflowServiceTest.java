package com.growthloops.workflow;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;
import com.growthloops.workflow.domain.StepDefinition;
import com.growthloops.workflow.dto.CreateWorkflowRequest;
import com.growthloops.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowServiceTest {

    @Autowired
    WorkflowService service;

    private String createWorkflow(List<StepDefinition> steps) {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setWorkflowId("wf-" + System.nanoTime());
        req.setSteps(steps);
        service.createWorkflow(req);
        return req.getWorkflowId();
    }

    @Test
    void executesContractReviewEndToEnd() {
        String id = createWorkflow(List.of(
                new StepDefinition("validate", Map.of("contractId", "123")),
                new StepDefinition("approve", null),
                new StepDefinition("execute", null)
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
        assertEquals(3, exec.getSteps().size());
        assertEquals(true, exec.getSteps().get(0).getOutput().get("valid"));
        assertEquals("system", exec.getSteps().get(1).getOutput().get("approvedBy"));
        assertEquals("success", exec.getSteps().get(2).getOutput().get("result"));
    }

    @Test
    void transformPropagatesResultIntoContext() {
        String id = createWorkflow(List.of(
                new StepDefinition("transform", Map.of(
                        "sourceKey", "name",
                        "name", "  hello  ",
                        "operation", "trim",
                        "targetKey", "cleanName"))
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
        assertEquals("hello", exec.getSteps().get(0).getOutput().get("cleanName"));
        assertEquals("hello", exec.getFinalContext().get("cleanName"));
    }

    @Test
    void delayStepExecutesAndRecordsDuration() {
        String id = createWorkflow(List.of(
                new StepDefinition("delay", Map.of("durationMillis", 50))
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
        assertEquals(50L, ((Number) exec.getSteps().get(0).getOutput().get("delayedMillis")).longValue());
    }

    @Test
    void negativeDelayFailsExecution() {
        String id = createWorkflow(List.of(
                new StepDefinition("delay", Map.of("durationMillis", -1))
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.FAILED, exec.getStatus());
        assertEquals(ExecutionStatus.FAILED, exec.getSteps().get(0).getStatus());
    }

    @Test
    void failureMarksRemainingStepsSkippedAndPreservesContext() {
        String id = createWorkflow(List.of(
                new StepDefinition("validate", Map.of()), // no contractId -> valid=false
                new StepDefinition("approve", null),      // throws on invalid contract
                new StepDefinition("execute", null)
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.FAILED, exec.getStatus());
        assertEquals(ExecutionStatus.COMPLETED, exec.getSteps().get(0).getStatus());
        assertEquals(ExecutionStatus.FAILED, exec.getSteps().get(1).getStatus());
        assertEquals(ExecutionStatus.SKIPPED, exec.getSteps().get(2).getStatus());
        // Final context is preserved on failure for troubleshooting.
        assertNotNull(exec.getFinalContext());
        assertEquals(false, exec.getFinalContext().get("valid"));
    }

    @Test
    void unknownStepTypeFailsExecution() {
        String id = createWorkflow(List.of(
                new StepDefinition("does-not-exist", null)
        ));

        Execution exec = service.executeSync(id);

        assertEquals(ExecutionStatus.FAILED, exec.getStatus());
        assertTrue(exec.getSteps().get(0).getError().contains("Unknown step type"));
    }

    @Test
    void duplicateWorkflowCreationConflicts() {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setWorkflowId("dup-" + System.nanoTime());
        req.setSteps(List.of(new StepDefinition("execute", null)));
        service.createWorkflow(req);

        assertThrows(IllegalArgumentException.class, () -> service.createWorkflow(req));
    }
}
