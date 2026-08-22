package com.prasad.agentic_software_engineer.orchestration.engine;

import java.util.Map;
import java.util.Objects;

public record TaskExecutionResult(
        Map<String, Object> outputs
) {

    public TaskExecutionResult {
        outputs = Map.copyOf(
                Objects.requireNonNull(outputs)
        );
    }

    public static TaskExecutionResult empty() {
        return new TaskExecutionResult(Map.of());
    }

    public static TaskExecutionResult of(
            String key,
            Object value
    ) {
        return new TaskExecutionResult(
                Map.of(
                        Objects.requireNonNull(key),
                        Objects.requireNonNull(value)
                )
        );
    }
}