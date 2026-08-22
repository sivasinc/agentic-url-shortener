CREATE TABLE agent_audit_events (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    revision BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    actor VARCHAR(200) NOT NULL,
    task_id UUID,
    task_type VARCHAR(50),
    detail VARCHAR(4000) NOT NULL,
    evidence_artifact VARCHAR(1000),
    evidence_media_type VARCHAR(100),
    evidence_content TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agent_audit_workflow_time
    ON agent_audit_events (workflow_id, occurred_at);
