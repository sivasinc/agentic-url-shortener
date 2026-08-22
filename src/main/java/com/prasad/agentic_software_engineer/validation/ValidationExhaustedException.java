package com.prasad.agentic_software_engineer.validation;

public class ValidationExhaustedException
        extends RuntimeException {

    public ValidationExhaustedException(
            String message
    ) {
        super(message);
    }
}