package com.growthloops.workflow.engine;

import java.util.Map;

/**
 * Strategy interface for executing a named step.
 * <p>
 * A handler receives:
 * - the step's static input (from the workflow definition)
 * - the accumulated workflow context (outputs of prior steps)
 * <p>
 * and returns the step's output, which is merged into the context.
 */
public interface StepHandler {

    /**
     * @return the step name this handler is responsible for.
     */
    String stepName();

    /**
     * Execute the step.
     *
     * @param input   static input declared in the workflow definition
     * @param context accumulated outputs from previously executed steps
     * @return this step's output
     */
    Map<String, Object> execute(Map<String, Object> input, Map<String, Object> context);
}
