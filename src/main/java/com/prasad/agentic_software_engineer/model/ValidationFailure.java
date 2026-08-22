package com.prasad.agentic_software_engineer.model;

import java.util.Objects;

public record ValidationFailure(
        String command,
        int exitCode,
        String summary,
        String logExcerpt
) {

    public ValidationFailure {
        command = requireText(command, "Command");
        summary = requireText(summary, "Failure summary");
        logExcerpt = requireText(
                logExcerpt,
                "Failure log excerpt"
        );
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