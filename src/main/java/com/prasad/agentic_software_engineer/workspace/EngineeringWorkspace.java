package com.prasad.agentic_software_engineer.workspace;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record EngineeringWorkspace(
        UUID workflowId,
        long revision,
        Path root,
        Path repository,
        Path baseline,
        Path artifacts,
        Path logs,
        Map<String, String> baselineHashes
) {

    public EngineeringWorkspace {
        workflowId = Objects.requireNonNull(workflowId);
        root = Objects.requireNonNull(root);
        repository = Objects.requireNonNull(repository);
        baseline = Objects.requireNonNull(baseline);
        artifacts = Objects.requireNonNull(artifacts);
        logs = Objects.requireNonNull(logs);
        baselineHashes = Map.copyOf(
                Objects.requireNonNull(baselineHashes)
        );

        if (revision < 1) {
            throw new IllegalArgumentException(
                    "Workspace revision must be positive"
            );
        }
    }
}