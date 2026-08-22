package com.prasad.agentic_software_engineer.orchestration.governance;

import java.time.Instant;
import java.util.Objects;

public record GovernanceDecision(
        String actor,
        String action,
        String reason,
        Instant occurredAt
) {
    public GovernanceDecision {
        actor = requireText(actor, "Actor");
        action = requireText(action, "Action");
        reason = requireText(reason, "Reason");
        occurredAt = Objects.requireNonNull(occurredAt);
    }

    private static String requireText(
            String value,
            String field
    ) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be blank"
            );
        }
        return value.trim();
    }
}
