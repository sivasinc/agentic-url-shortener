package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record EngineeringTaskPlan(
        String id,
        String name,
        String description,
        List<String> dependencyIds,
        boolean parallelizable,
        boolean approvalRequired
) {

    public EngineeringTaskPlan {
        id = requireText(id, "Plan task ID");
        name = requireText(name, "Plan task name");
        description = requireText(
                description,
                "Plan task description"
        );

        dependencyIds = List.copyOf(
                Objects.requireNonNull(
                        dependencyIds
                )
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