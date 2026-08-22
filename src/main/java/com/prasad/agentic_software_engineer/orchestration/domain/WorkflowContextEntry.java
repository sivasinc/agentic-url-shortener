package com.prasad.agentic_software_engineer.orchestration.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkflowContextEntry(
        String key,
        Object value,
        UUID producingTaskId,
        long workflowRevision,
        Instant createdAt
) {

    public WorkflowContextEntry {
        key = Objects.requireNonNull(key);
        value = Objects.requireNonNull(value);
        producingTaskId = Objects.requireNonNull(producingTaskId);
        createdAt = Objects.requireNonNull(createdAt);

        if (key.isBlank()) {
            throw new IllegalArgumentException(
                    "Context key cannot be blank"
            );
        }

        if (workflowRevision < 1) {
            throw new IllegalArgumentException(
                    "Workflow revision must be positive"
            );
        }
    }
}