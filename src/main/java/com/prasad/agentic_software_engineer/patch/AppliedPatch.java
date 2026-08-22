package com.prasad.agentic_software_engineer.patch;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AppliedPatch(
        List<String> changedFiles,
        String diff,
        Map<String, String> resultingHashes,
        String diffArtifact
) {

    public AppliedPatch {
        changedFiles = List.copyOf(
                Objects.requireNonNull(changedFiles)
        );

        diff = Objects.requireNonNull(diff);

        resultingHashes = Map.copyOf(
                Objects.requireNonNull(resultingHashes)
        );

        diffArtifact = Objects.requireNonNull(
                diffArtifact
        );

        if (changedFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Applied patch must change files"
            );
        }

        if (diff.isBlank()) {
            throw new IllegalArgumentException(
                    "Applied patch must produce a diff"
            );
        }
    }
}