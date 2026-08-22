package com.prasad.agentic_software_engineer.orchestration.exception;

import java.util.UUID;

public class WorkflowNotFoundException
        extends RuntimeException {

    public WorkflowNotFoundException(
            UUID workflowId
    ) {
        super(
                "Engineering workflow not found: " +
                        workflowId
        );
    }
}