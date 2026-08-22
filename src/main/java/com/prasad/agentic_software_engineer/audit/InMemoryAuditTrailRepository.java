package com.prasad.agentic_software_engineer.audit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAuditTrailRepository implements AuditTrailRepository {

    private final ConcurrentMap<UUID, List<AgentAuditEvent>> events =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AuditEvidence> evidence =
            new ConcurrentHashMap<>();

    @Override
    public void append(AgentAuditEvent event, AuditEvidence item) {
        events.computeIfAbsent(
                event.workflowId(),
                ignored -> java.util.Collections.synchronizedList(
                        new ArrayList<>()
                )
        ).add(event);

        if (item != null) {
            evidence.put(item.eventId(), item);
        }
    }

    @Override
    public List<AgentAuditEvent> findByWorkflowId(UUID workflowId) {
        List<AgentAuditEvent> workflowEvents = events.get(workflowId);
        if (workflowEvents == null) {
            return List.of();
        }
        synchronized (workflowEvents) {
            return workflowEvents.stream()
                    .sorted(Comparator.comparing(AgentAuditEvent::occurredAt))
                    .toList();
        }
    }

    @Override
    public List<AuditEvidence> findEvidenceByWorkflowId(UUID workflowId) {
        return findByWorkflowId(workflowId).stream()
                .map(AgentAuditEvent::id)
                .map(evidence::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<AuditEvidence> findEvidence(
            UUID workflowId,
            UUID eventId
    ) {
        return findByWorkflowId(workflowId).stream()
                .filter(event -> event.id().equals(eventId))
                .findFirst()
                .map(AgentAuditEvent::id)
                .map(evidence::get);
    }
}
