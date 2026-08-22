package com.prasad.agentic_software_engineer.model;

public class ModelInvocationException
        extends RuntimeException {

    public ModelInvocationException(String message) {
        super(message);
    }

    public ModelInvocationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}