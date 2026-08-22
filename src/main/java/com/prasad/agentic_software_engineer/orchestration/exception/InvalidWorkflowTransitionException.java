package com.prasad.agentic_software_engineer.orchestration.exception;

public class InvalidWorkflowTransitionException
        extends RuntimeException {

    public InvalidWorkflowTransitionException(
            String message
    ) {
        super(message);
    }
}
