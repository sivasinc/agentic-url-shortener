package com.prasad.agentic_software_engineer.orchestration.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WorkflowContext {

    private final ConcurrentMap<String, WorkflowContextEntry> entries =
            new ConcurrentHashMap<>();

    public void put(
            String key,
            Object value,
            UUID producingTaskId,
            long workflowRevision,
            Instant createdAt
    ) {
        WorkflowContextEntry entry =
                new WorkflowContextEntry(
                        key,
                        value,
                        producingTaskId,
                        workflowRevision,
                        createdAt
                );

        entries.put(key, entry);
    }

    public boolean contains(String key) {
        return entries.containsKey(key);
    }

    public Optional<WorkflowContextEntry> find(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    public Object requireValue(String key) {
        WorkflowContextEntry entry = entries.get(key);

        if (entry == null) {
            throw new IllegalArgumentException(
                    "Workflow context does not contain key: " + key
            );
        }

        return entry.value();
    }

    public Map<String, WorkflowContextEntry> snapshot() {
        return Map.copyOf(
                new LinkedHashMap<>(entries)
        );
    }
}