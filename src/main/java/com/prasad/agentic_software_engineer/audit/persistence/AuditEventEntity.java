package com.prasad.agentic_software_engineer.audit.persistence;

import com.prasad.agentic_software_engineer.audit.AgentAuditEvent;
import com.prasad.agentic_software_engineer.audit.AuditEventType;
import com.prasad.agentic_software_engineer.audit.AuditEvidence;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private long revision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType type;

    @Column(nullable = false, length = 200)
    private String actor;

    @Column(name = "task_id")
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 50)
    private TaskType taskType;

    @Column(nullable = false, length = 4000)
    private String detail;

    @Column(name = "evidence_artifact", length = 1000)
    private String evidenceArtifact;

    @Column(name = "evidence_media_type", length = 100)
    private String evidenceMediaType;

    @Column(name = "evidence_content", columnDefinition = "TEXT")
    private String evidenceContent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AuditEventEntity(
            AgentAuditEvent event,
            AuditEvidence evidence
    ) {
        this.id = event.id();
        this.workflowId = event.workflowId();
        this.revision = event.revision();
        this.type = event.type();
        this.actor = event.actor();
        this.taskId = event.taskId();
        this.taskType = event.taskType();
        this.detail = event.detail();
        this.evidenceArtifact = event.evidenceArtifact();
        this.evidenceMediaType = evidence == null
                ? null
                : evidence.mediaType();
        this.evidenceContent = evidence == null
                ? null
                : evidence.content();
        this.occurredAt = event.occurredAt();
    }

    public AgentAuditEvent toEvent() {
        return new AgentAuditEvent(
                id,
                workflowId,
                revision,
                type,
                actor,
                taskId,
                taskType,
                detail,
                evidenceArtifact,
                occurredAt
        );
    }

    public AuditEvidence toEvidence() {
        if (evidenceContent == null) {
            return null;
        }
        return new AuditEvidence(
                id,
                evidenceArtifact,
                evidenceMediaType,
                evidenceContent
        );
    }
}
