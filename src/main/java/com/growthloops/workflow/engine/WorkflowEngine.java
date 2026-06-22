package com.growthloops.workflow.engine;

import com.growthloops.workflow.domain.*;
import com.growthloops.workflow.engine.handlers.DefaultStepHandler;
import com.growthloops.workflow.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core sequential workflow engine.
 * <p>
 * Responsibilities:
 * - Iterate steps in order
 * - Maintain a shared context of accumulated step outputs
 * - Update execution + step-level state as it progresses
 * - Stop and mark FAILED if a step throws
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final Map<String, StepHandler> handlers;
    private final DefaultStepHandler defaultHandler = new DefaultStepHandler();
    private final ExecutionRepository executionRepository;

    public WorkflowEngine(List<StepHandler> handlerBeans, ExecutionRepository executionRepository) {
        this.handlers = handlerBeans.stream()
                .collect(Collectors.toMap(StepHandler::stepName, Function.identity()));
        this.executionRepository = executionRepository;
    }

    /**
     * Executes the workflow synchronously and returns the final execution state.
     * Persisting after each step gives "live" visibility for the state endpoint.
     */
    public Execution run(Workflow workflow, Execution execution) {
        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        Map<String, Object> context = new HashMap<>();
        List<StepDefinition> steps = workflow.getSteps();

        for (int i = 0; i < steps.size(); i++) {
            StepDefinition def = steps.get(i);
            StepExecution stepExec = execution.getSteps().get(i);

            execution.setCurrentStep(i);
            stepExec.setStatus(ExecutionStatus.RUNNING);
            stepExec.setStartedAt(Instant.now());
            executionRepository.save(execution);

            try {
                Map<String, Object> output = dispatch(def, context);
                stepExec.setOutput(output);
                stepExec.setStatus(ExecutionStatus.COMPLETED);
                stepExec.setFinishedAt(Instant.now());

                if (output != null) {
                    context.putAll(output);
                }
            } catch (Exception ex) {
                log.warn("Step '{}' failed in execution {}: {}",
                        def.getName(), execution.getExecutionId(), ex.getMessage());
                stepExec.setStatus(ExecutionStatus.FAILED);
                stepExec.setError(ex.getMessage());
                stepExec.setFinishedAt(Instant.now());

                execution.setStatus(ExecutionStatus.FAILED);
                execution.setFinishedAt(Instant.now());
                executionRepository.save(execution);
                return execution;
            }

            executionRepository.save(execution);
        }

        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCurrentStep(steps.size());
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);
        return execution;
    }

    private Map<String, Object> dispatch(StepDefinition def, Map<String, Object> context) {
        StepHandler handler = handlers.get(def.getName());
        if (handler != null) {
            return handler.execute(def.getInput(), context);
        }
        return defaultHandler.execute(def.getName(), def.getInput());
    }
}
