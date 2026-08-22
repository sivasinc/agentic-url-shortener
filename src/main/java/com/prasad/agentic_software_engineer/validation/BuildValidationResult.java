package com.prasad.agentic_software_engineer.validation;

import com.prasad.agentic_software_engineer.model.ValidationFailure;

import java.time.Duration;
import java.util.Objects;

public record BuildValidationResult(
        boolean successful,
        String command,
        int exitCode,
        boolean timedOut,
        Duration duration,
        String output,
        String logArtifact
) {

    public BuildValidationResult {
        command = requireText(command, "Command");
        duration = Objects.requireNonNull(duration);
        output = Objects.requireNonNull(output);
        logArtifact = requireText(
                logArtifact,
                "Log artifact"
        );

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                    "Validation duration cannot be negative"
            );
        }

        if (successful &&
                (exitCode != 0 || timedOut)) {
            throw new IllegalArgumentException(
                    "Successful validation cannot have a failure exit status"
            );
        }
    }

    public ValidationFailure toFailure() {
        if (successful) {
            throw new IllegalStateException(
                    "Successful validation has no failure"
            );
        }

        String summary = timedOut
                ? "Maven validation exceeded its configured timeout"
                : "Maven validation exited with code " + exitCode;

        String excerpt = output.isBlank()
                ? "No build output was captured"
                : output;

        return new ValidationFailure(
                command,
                exitCode,
                summary,
                excerpt
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