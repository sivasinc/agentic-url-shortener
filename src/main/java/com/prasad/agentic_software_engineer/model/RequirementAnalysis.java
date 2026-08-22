package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record RequirementAnalysis(
        String normalizedRequirement,
        List<String> acceptanceCriteria,
        List<String> ambiguities,
        List<String> assumptions,
        List<String> risks,
        boolean requiresClarification
) {

    public RequirementAnalysis {
        normalizedRequirement = Objects.requireNonNull(
                normalizedRequirement
        ).trim();

        acceptanceCriteria = List.copyOf(
                Objects.requireNonNull(
                        acceptanceCriteria
                )
        );

        ambiguities = List.copyOf(
                Objects.requireNonNull(ambiguities)
        );

        assumptions = List.copyOf(
                Objects.requireNonNull(assumptions)
        );

        risks = List.copyOf(
                Objects.requireNonNull(risks)
        );

        if (normalizedRequirement.isBlank()) {
            throw new IllegalArgumentException(
                    "Normalized requirement cannot be blank"
            );
        }

        if (requiresClarification &&
                ambiguities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Clarification requires at least one ambiguity"
            );
        }
    }
}