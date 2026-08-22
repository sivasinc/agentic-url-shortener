package com.prasad.agentic_software_engineer.orchestration.domain;

public enum GateType {
    NONE,
    DEPENDENCIES_SUCCEEDED,
    CONTEXT_KEYS_PRESENT,
    HUMAN_APPROVAL
}