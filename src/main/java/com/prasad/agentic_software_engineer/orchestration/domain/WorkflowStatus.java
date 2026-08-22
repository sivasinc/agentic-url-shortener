package com.prasad.agentic_software_engineer.orchestration.domain;

public enum WorkflowStatus {
    CREATED,
    RUNNING,
    AWAITING_CLARIFICATION,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    SAFE_STOPPED
}