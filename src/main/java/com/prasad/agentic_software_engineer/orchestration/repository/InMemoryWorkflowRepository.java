package com.prasad.agentic_software_engineer.orchestration.repository;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryWorkflowRepository
        implements WorkflowRepository {

    private final ConcurrentMap<UUID, EngineeringWorkflow> workflows =
            new ConcurrentHashMap<>();

    @Override
    public EngineeringWorkflow save(
            EngineeringWorkflow workflow
    ) {
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public Optional<EngineeringWorkflow> findById(
            UUID workflowId
    ) {
        return Optional.ofNullable(workflows.get(workflowId));
    }
}