package com.prasad.agentic_software_engineer.orchestration.dto;

import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;

import java.util.List;
import java.util.UUID;

public record EngineeringWorkflowResponse(
        UUID id,
        long revision,
        WorkflowStatus status,
        String modelProvider,
        RequirementAnalysis requirementAnalysis,
        EngineeringPlan engineeringPlan,
        List<String> changedFiles,
        String diff,
        List<String> clarificationQuestions,
        List<WorkflowTaskResponse> tasks,
        String failureMessage
) {
}