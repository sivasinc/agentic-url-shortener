package com.prasad.agentic_software_engineer.orchestration.dto;

import com.prasad.agentic_software_engineer.model.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEngineeringWorkflowRequest(
        @NotNull
        ScenarioType scenarioType,

        @NotBlank
        @Size(max = 4000)
        String requirement,

        @NotBlank
        @Size(max = 500)
        String repositoryPath
) {
}