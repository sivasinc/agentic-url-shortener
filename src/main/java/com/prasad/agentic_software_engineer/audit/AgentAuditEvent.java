package com.prasad.agentic_software_engineer.audit;

import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentAuditEvent(
        UUID id,
        UUID workflowId,
        long revision,
        AuditEventType type,
        String actor,
        UUID taskId,
        TaskType taskType,
        String detail,
        String evidenceArtifact,
        Instant occurredAt
) {
    public AgentAuditEvent {
        id = Objects.requireNonNull(id);
        workflowId = Objects.requireNonNull(workflowId);
        if (revision < 1) {
            throw new IllegalArgumentException("Revision must be positive");
        }
        type = Objects.requireNonNull(type);
        actor = requireText(actor, "Actor");
        detail = requireText(detail, "Detail");
        if (detail.length() > 4000) {
            detail = detail.substring(0, 4000);
        }
        occurredAt = Objects.requireNonNull(occurredAt);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }
}
