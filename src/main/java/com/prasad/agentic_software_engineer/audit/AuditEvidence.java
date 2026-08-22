package com.prasad.agentic_software_engineer.audit;

import java.util.Objects;
import java.util.UUID;

public record AuditEvidence(
        UUID eventId,
        String artifact,
        String mediaType,
        String content
) {
    public AuditEvidence {
        eventId = Objects.requireNonNull(eventId);
        artifact = Objects.requireNonNull(artifact);
        mediaType = Objects.requireNonNull(mediaType);
        content = Objects.requireNonNull(content);
    }
}
