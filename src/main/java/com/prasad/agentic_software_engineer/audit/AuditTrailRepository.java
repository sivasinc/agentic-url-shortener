package com.prasad.agentic_software_engineer.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditTrailRepository {

    void append(AgentAuditEvent event, AuditEvidence evidence);

    List<AgentAuditEvent> findByWorkflowId(UUID workflowId);

    List<AuditEvidence> findEvidenceByWorkflowId(UUID workflowId);

    Optional<AuditEvidence> findEvidence(UUID workflowId, UUID eventId);
}
