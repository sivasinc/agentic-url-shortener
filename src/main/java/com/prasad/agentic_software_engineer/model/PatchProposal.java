package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record PatchProposal(
        String summary,
        List<ProposedFileChange> changes,
        List<String> assumptions,
        List<String> risks
) {

    public PatchProposal {
        summary = Objects.requireNonNull(
                summary
        ).trim();

        changes = List.copyOf(
                Objects.requireNonNull(changes)
        );

        assumptions = List.copyOf(
                Objects.requireNonNull(assumptions)
        );

        risks = List.copyOf(
                Objects.requireNonNull(risks)
        );

        if (summary.isBlank()) {
            throw new IllegalArgumentException(
                    "Patch summary cannot be blank"
            );
        }

        if (changes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Patch must contain at least one file change"
            );
        }
    }
}