package com.growthloops.workflow.repository;

import com.growthloops.workflow.domain.Execution;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class ExecutionRepository {

    private final ConcurrentMap<String, Execution> store = new ConcurrentHashMap<>();

    public Execution save(Execution execution) {
        store.put(execution.getExecutionId(), execution);
        return execution;
    }

    public Optional<Execution> findById(String executionId) {
        return Optional.ofNullable(store.get(executionId));
    }

    public List<Execution> findByWorkflowId(String workflowId) {
        return store.values().stream()
                .filter(e -> e.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());
    }
}
