package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record RequirementContext(
        ScenarioType scenarioType,
        String rawRequirement,
        List<String> clarificationHistory
) {

    public RequirementContext {
        scenarioType = Objects.requireNonNull(
                scenarioType
        );

        rawRequirement = Objects.requireNonNull(
                rawRequirement
        ).trim();

        clarificationHistory = List.copyOf(
                Objects.requireNonNull(
                        clarificationHistory
                )
        );

        if (rawRequirement.isBlank()) {
            throw new IllegalArgumentException(
                    "Requirement cannot be blank"
            );
        }
    }
}