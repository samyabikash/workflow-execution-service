package com.growthloops.workflow.repository;

import com.growthloops.workflow.domain.Workflow;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class WorkflowRepository {

    private final ConcurrentMap<String, Workflow> store = new ConcurrentHashMap<>();

    public Workflow save(Workflow workflow) {
        store.put(workflow.getWorkflowId(), workflow);
        return workflow;
    }

    /**
     * Atomically stores the workflow only if no entry already exists for its id.
     *
     * @return the previously stored workflow if one existed, otherwise {@code null}.
     */
    public Workflow saveIfAbsent(Workflow workflow) {
        return store.putIfAbsent(workflow.getWorkflowId(), workflow);
    }

    public Optional<Workflow> findById(String workflowId) {
        return Optional.ofNullable(store.get(workflowId));
    }

    public boolean exists(String workflowId) {
        return store.containsKey(workflowId);
    }
}
