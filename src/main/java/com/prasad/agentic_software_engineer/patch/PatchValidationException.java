package com.prasad.agentic_software_engineer.patch;

public class PatchValidationException
        extends RuntimeException {

    public PatchValidationException(String message) {
        super(message);
    }

    public PatchValidationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}