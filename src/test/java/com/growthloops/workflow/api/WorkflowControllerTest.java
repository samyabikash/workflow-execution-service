package com.growthloops.workflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthloops.workflow.domain.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-level coverage of the external contract using MockMvc.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WorkflowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createExecuteAndPollHappyPath() throws Exception {
        String workflowId = "api-wf-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(Map.of(
                "workflowId", workflowId,
                "steps", new Object[]{
                        Map.of("name", "validate", "input", Map.of("contractId", "123")),
                        Map.of("name", "delay", "input", Map.of("durationMillis", 50)),
                        Map.of("name", "execute")
                }
        ));

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        post("/api/v1/workflows/" + workflowId + "/executions"))
                .andExpect(status().isAccepted())
                .andReturn();

        String execId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("executionId").asText();

        await().atMost(5, SECONDS).until(() -> {
            MvcResult state = mockMvc.perform(
                    get("/api/v1/executions/" + execId + "/state")).andReturn();
            return objectMapper.readTree(state.getResponse().getContentAsString())
                    .get("status").asText().equals(ExecutionStatus.COMPLETED.name());
        });

        mockMvc.perform(get("/api/v1/executions/" + execId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.finalContext.valid", is(true)));
    }

    @Test
    void blankStepNameIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "workflowId", "bad-wf-" + System.nanoTime(),
                "steps", new Object[]{Map.of("name", "")}
        ));

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateWorkflowReturnsConflict() throws Exception {
        String workflowId = "conflict-wf-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(Map.of(
                "workflowId", workflowId,
                "steps", new Object[]{Map.of("name", "execute")}
        ));

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
