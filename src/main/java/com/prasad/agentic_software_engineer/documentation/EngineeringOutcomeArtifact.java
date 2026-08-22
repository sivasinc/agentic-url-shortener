package com.prasad.agentic_software_engineer.documentation;

import java.util.Objects;

public record EngineeringOutcomeArtifact(
        String relativePath,
        String content
) {
    public EngineeringOutcomeArtifact {
        relativePath = Objects.requireNonNull(relativePath).trim();
        content = Objects.requireNonNull(content).trim();
        if (relativePath.isBlank() || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Engineering outcome artifact cannot be blank"
            );
        }
    }
}
