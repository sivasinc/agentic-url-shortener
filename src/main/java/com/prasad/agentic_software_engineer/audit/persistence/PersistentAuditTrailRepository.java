package com.prasad.agentic_software_engineer.audit.persistence;

import com.prasad.agentic_software_engineer.audit.AgentAuditEvent;
import com.prasad.agentic_software_engineer.audit.AuditEvidence;
import com.prasad.agentic_software_engineer.audit.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PersistentAuditTrailRepository
        implements AuditTrailRepository {

    private final JpaAuditEventRepository repository;

    @Override
    @Transactional
    public void append(AgentAuditEvent event, AuditEvidence evidence) {
        repository.save(new AuditEventEntity(event, evidence));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentAuditEvent> findByWorkflowId(UUID workflowId) {
        return repository.findAllByWorkflowIdOrderByOccurredAtAsc(workflowId)
                .stream()
                .map(AuditEventEntity::toEvent)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvidence> findEvidenceByWorkflowId(UUID workflowId) {
        return repository
                .findAllByWorkflowIdAndEvidenceContentIsNotNullOrderByOccurredAtAsc(
                        workflowId
                )
                .stream()
                .map(AuditEventEntity::toEvidence)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditEvidence> findEvidence(
            UUID workflowId,
            UUID eventId
    ) {
        return repository.findByWorkflowIdAndId(workflowId, eventId)
                .map(AuditEventEntity::toEvidence);
    }
}
