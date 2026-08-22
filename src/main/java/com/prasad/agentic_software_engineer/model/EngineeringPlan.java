package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record EngineeringPlan(
        String rationale,
        List<EngineeringTaskPlan> tasks,
        List<String> risks,
        List<String> tradeOffs
) {

    public EngineeringPlan {
        rationale = Objects.requireNonNull(
                rationale
        ).trim();

        tasks = List.copyOf(
                Objects.requireNonNull(tasks)
        );

        risks = List.copyOf(
                Objects.requireNonNull(risks)
        );

        tradeOffs = List.copyOf(
                Objects.requireNonNull(tradeOffs)
        );

        if (rationale.isBlank()) {
            throw new IllegalArgumentException(
                    "Plan rationale cannot be blank"
            );
        }

        if (tasks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Engineering plan must contain tasks"
            );
        }
    }
}