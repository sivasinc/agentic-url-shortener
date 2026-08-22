package com.prasad.agentic_software_engineer.audit.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAuditEventRepository
        extends JpaRepository<AuditEventEntity, UUID> {

    List<AuditEventEntity> findAllByWorkflowIdOrderByOccurredAtAsc(
            UUID workflowId
    );

    List<AuditEventEntity>
    findAllByWorkflowIdAndEvidenceContentIsNotNullOrderByOccurredAtAsc(
            UUID workflowId
    );

    Optional<AuditEventEntity> findByWorkflowIdAndId(
            UUID workflowId,
            UUID id
    );
}
