package com.growthloops.workflow.service;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.Workflow;
import com.growthloops.workflow.engine.CancellationToken;
import com.growthloops.workflow.engine.WorkflowEngine;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs a workflow on a background thread.
 * <p>
 * Exists as a separate bean so {@code @Async} is invoked through the Spring proxy
 * (i.e. from outside the bean). Calling an {@code @Async} method on {@code this}
 * inside {@link WorkflowService} would bypass the proxy and run synchronously —
 * which is why executions previously appeared COMPLETED immediately.
 * <p>
 * The {@link CancellationToken} is registered by the caller <em>before</em> dispatch
 * so a cancel request can never arrive before the token exists; it is passed in here
 * rather than looked up to keep that guarantee explicit.
 */
@Component
public class AsyncExecutionRunner {

    private final WorkflowEngine engine;

    public AsyncExecutionRunner(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Async
    public void run(Workflow workflow, Execution execution, CancellationToken token) {
        engine.run(workflow, execution, token);
    }
}
