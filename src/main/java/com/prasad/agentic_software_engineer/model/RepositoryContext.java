package com.prasad.agentic_software_engineer.model;

import com.prasad.agentic_software_engineer.agent.repository.RepositoryAssessment;

import java.util.Map;
import java.util.Objects;

public record RepositoryContext(
        ScenarioType scenarioType,
        RequirementAnalysis requirement,
        RepositoryAssessment assessment,
        Map<String, String> relevantFiles
) {

    public RepositoryContext {
        scenarioType = Objects.requireNonNull(
                scenarioType
        );

        requirement = Objects.requireNonNull(
                requirement
        );

        assessment = Objects.requireNonNull(
                assessment
        );

        relevantFiles = Map.copyOf(
                Objects.requireNonNull(relevantFiles)
        );
    }
}
