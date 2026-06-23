package com.growthloops.workflow.service;

import com.growthloops.workflow.domain.Execution;
import com.growthloops.workflow.domain.Workflow;
import com.growthloops.workflow.engine.WorkflowEngine;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @ClassName AsyncExecutionRunner
 * @Description Exists solely so that @Async is called through a Spring proxy (i.e., from
 * outside the bean), which is required for the annotation to take effect.
 * Calling a @Async method on "this" inside WorkflowService bypasses the proxy
 * and runs synchronously — that is exactly why every execution appeared as
 * COMPLETED immediately.
 * @Author samya
 * @Date 23/06/26
 */
@Component
public class AsyncExecutionRunner {

    private final WorkflowEngine engine;

    public AsyncExecutionRunner(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Async
    public void run(Workflow workflow, Execution execution) {
        engine.run(workflow, execution);
    }
}