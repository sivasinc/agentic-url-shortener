package com.prasad.agentic_software_engineer.orchestration.exception;

public class WorkflowGateRejectedException
        extends RuntimeException {

    public WorkflowGateRejectedException(String message) {
        super(message);
    }
}