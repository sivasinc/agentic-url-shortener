package com.prasad.agentic_software_engineer.model;

import java.util.List;
import java.util.Objects;

public record DocumentationProposal(
        String readmeSection,
        String architectureSummary,
        List<String> limitations
) {

    public DocumentationProposal {
        readmeSection = Objects.requireNonNull(
                readmeSection
        ).trim();

        architectureSummary = Objects.requireNonNull(
                architectureSummary
        ).trim();

        limitations = List.copyOf(
                Objects.requireNonNull(limitations)
        );

        if (readmeSection.isBlank() ||
                architectureSummary.isBlank()) {
            throw new IllegalArgumentException(
                    "Documentation output cannot be blank"
            );
        }
    }
}