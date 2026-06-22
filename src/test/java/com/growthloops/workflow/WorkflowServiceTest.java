package com.growthloops.workflow;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.ExecutionStatus;
import com.growthloops.workflow.domain.StepDefinition;
import com.growthloops.workflow.dto.CreateWorkflowRequest;
import com.growthloops.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class WorkflowServiceTest {

    @Autowired
    WorkflowService service;

    @Test
    void executesContractReviewEndToEnd() {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setWorkflowId("contract-review-" + System.nanoTime());
        req.setSteps(List.of(
                new StepDefinition("validate", Map.of("contractId", "123")),
                new StepDefinition("approve", null),
                new StepDefinition("execute", null)
        ));
        service.createWorkflow(req);

        Execution exec = service.execute(req.getWorkflowId());

        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
        assertEquals(3, exec.getSteps().size());
        assertEquals(true, exec.getSteps().get(0).getOutput().get("valid"));
        assertEquals("system", exec.getSteps().get(1).getOutput().get("approvedBy"));
        assertEquals("success", exec.getSteps().get(2).getOutput().get("result"));
    }
}
