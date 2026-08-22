package com.prasad.agentic_software_engineer.tool.repository;

import java.util.Objects;

public record RepositoryFile(
        String relativePath,
        long sizeBytes
) {

    public RepositoryFile {
        relativePath = Objects.requireNonNull(
                relativePath
        );

        if (relativePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Repository file path cannot be blank"
            );
        }

        if (sizeBytes < 0) {
            throw new IllegalArgumentException(
                    "Repository file size cannot be negative"
            );
        }
    }
}