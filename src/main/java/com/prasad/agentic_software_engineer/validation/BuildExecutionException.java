package com.prasad.agentic_software_engineer.validation;

public class BuildExecutionException
        extends RuntimeException {

    public BuildExecutionException(
            String message
    ) {
        super(message);
    }

    public BuildExecutionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}