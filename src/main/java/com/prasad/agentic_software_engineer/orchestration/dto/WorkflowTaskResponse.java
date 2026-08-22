package com.prasad.agentic_software_engineer.orchestration.dto;

import com.prasad.agentic_software_engineer.orchestration.domain.TaskStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;

import java.util.Set;
import java.util.UUID;

public record WorkflowTaskResponse(
        UUID id,
        String name,
        TaskType type,
        TaskStatus status,
        Set<UUID> dependencyIds,
        int attempt,
        String failureMessage
) {
}