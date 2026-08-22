package com.prasad.agentic_software_engineer.agent.repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RepositoryAssessment(
        int totalFiles,
        Set<String> buildSystems,
        List<String> modules,
        List<String> sourceFiles,
        List<String> testFiles,
        List<String> migrations,
        List<String> configurationFiles,
        List<String> documentationFiles,
        List<String> impactedFiles
) {

    public RepositoryAssessment {
        buildSystems = Set.copyOf(
                Objects.requireNonNull(buildSystems)
        );

        modules = List.copyOf(
                Objects.requireNonNull(modules)
        );

        sourceFiles = List.copyOf(
                Objects.requireNonNull(sourceFiles)
        );

        testFiles = List.copyOf(
                Objects.requireNonNull(testFiles)
        );

        migrations = List.copyOf(
                Objects.requireNonNull(migrations)
        );

        configurationFiles = List.copyOf(
                Objects.requireNonNull(configurationFiles)
        );

        documentationFiles = List.copyOf(
                Objects.requireNonNull(documentationFiles)
        );

        impactedFiles = List.copyOf(
                Objects.requireNonNull(impactedFiles)
        );

        if (totalFiles < 0) {
            throw new IllegalArgumentException(
                    "Total files cannot be negative"
            );
        }
    }
}