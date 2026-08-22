package com.prasad.agentic_software_engineer.tool.repository;

import java.util.Objects;

public record RepositorySearchMatch(
        String relativePath,
        int lineNumber,
        String line
) {

    public RepositorySearchMatch {
        relativePath = Objects.requireNonNull(
                relativePath
        );

        line = Objects.requireNonNull(line);

        if (lineNumber < 1) {
            throw new IllegalArgumentException(
                    "Search match line number must be positive"
            );
        }
    }
}