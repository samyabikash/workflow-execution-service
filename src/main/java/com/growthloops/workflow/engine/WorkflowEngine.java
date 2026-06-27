package com.growthloops.workflow.engine;

import com.growthloops.workflow.domain.*;
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

@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final Map<String, StepHandler> handlers;
    private final ExecutionRepository executionRepository;
    private final CancellationRegistry cancellationRegistry;

    public WorkflowEngine(List<StepHandler> handlerBeans,
                          ExecutionRepository executionRepository,
                          CancellationRegistry cancellationRegistry) {
        this.handlers = handlerBeans.stream()
                .collect(Collectors.toMap(StepHandler::stepName, Function.identity()));
        this.executionRepository = executionRepository;
        this.cancellationRegistry = cancellationRegistry;
    }

    /**
     * Executes the workflow sequentially, checking for cancellation before
     * each step. Persists state after every transition for live poll visibility.
     */
    public Execution run(Workflow workflow, Execution execution, CancellationToken token) {
        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        Map<String, Object> context = new HashMap<>();
        List<StepDefinition> steps = workflow.getSteps();

        for (int i = 0; i < steps.size(); i++) {

            // Check cancellation BEFORE starting the next step
            if (token.isCancellationRequested()) {
                //log.info("Execution {} cancelled before step {}", execution.getExecutionId(), i);
                cancelRemainingSteps(execution, i);
                execution.setStatus(ExecutionStatus.CANCELLED);
                execution.setFinalContext(new HashMap<>(context));
                execution.setFinishedAt(Instant.now());
                executionRepository.save(execution);
                cancellationRegistry.deregister(execution.getExecutionId());
                return execution;
            }

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

            } catch (IllegalStateException ex) {
                // DelayStepHandler re-throws InterruptedException as IllegalStateException
                // when the thread is interrupted. Treat this as cancellation.
                if (token.isCancellationRequested()) {
                    log.info("Execution {} cancelled during step {}",
                            execution.getExecutionId(), def.getName());
                    stepExec.setStatus(ExecutionStatus.CANCELLED);
                    stepExec.setError("Step cancelled during execution");
                    stepExec.setFinishedAt(Instant.now());
                    cancelRemainingSteps(execution, i + 1);
                    execution.setStatus(ExecutionStatus.CANCELLED);
                    execution.setFinalContext(new HashMap<>(context));
                    execution.setFinishedAt(Instant.now());
                    executionRepository.save(execution);
                    cancellationRegistry.deregister(execution.getExecutionId());
                    return execution;
                }
                // Not a cancellation — treat as a real failure
                handleStepFailure(execution, def, stepExec, ex, context, i);
                cancellationRegistry.deregister(execution.getExecutionId());
                return execution;

            } catch (Exception ex) {
                log.warn("Step '{}' failed in execution {}: {}",
                        def.getName(), execution.getExecutionId(), ex.getMessage());
                handleStepFailure(execution, def, stepExec, ex, context, i);
                cancellationRegistry.deregister(execution.getExecutionId());
                return execution;
            }

            executionRepository.save(execution);
        }

        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCurrentStep(steps.size());
        execution.setFinalContext(new HashMap<>(context));
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);
        cancellationRegistry.deregister(execution.getExecutionId());
        return execution;
    }

    private void handleStepFailure(Execution execution, StepDefinition def,
                                   StepExecution stepExec, Exception ex,
                                   Map<String, Object> context, int stepIndex) {
        stepExec.setStatus(ExecutionStatus.FAILED);
        stepExec.setError(ex.getMessage());
        stepExec.setFinishedAt(Instant.now());
        skipRemainingSteps(execution, stepIndex + 1);
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFinalContext(new HashMap<>(context));
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);
    }

    private void cancelRemainingSteps(Execution execution, int fromIndex) {
        List<StepExecution> stepExecs = execution.getSteps();
        for (int j = fromIndex; j < stepExecs.size(); j++) {
            StepExecution remaining = stepExecs.get(j);
            if (remaining.getStatus() == ExecutionStatus.PENDING) {
                remaining.setStatus(ExecutionStatus.CANCELLED);
            }
        }
    }

    private void skipRemainingSteps(Execution execution, int fromIndex) {
        List<StepExecution> stepExecs = execution.getSteps();
        for (int j = fromIndex; j < stepExecs.size(); j++) {
            StepExecution remaining = stepExecs.get(j);
            if (remaining.getStatus() == ExecutionStatus.PENDING) {
                remaining.setStatus(ExecutionStatus.SKIPPED);
            }
        }
    }

    private Map<String, Object> dispatch(StepDefinition def, Map<String, Object> context) {
        StepHandler handler = handlers.get(def.getName());
        if (handler == null) {
            throw new IllegalArgumentException("Unknown step type: '" + def.getName() + "'");
        }
        return handler.execute(def.getInput(), context);
    }
}