package com.prasad.agentic_software_engineer.orchestration.repository;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository {

    EngineeringWorkflow save(
            EngineeringWorkflow workflow
    );

    Optional<EngineeringWorkflow> findById(
            UUID workflowId
    );
}