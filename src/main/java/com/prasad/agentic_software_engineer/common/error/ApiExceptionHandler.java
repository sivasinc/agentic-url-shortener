package com.prasad.agentic_software_engineer.common.error;

import com.prasad.agentic_software_engineer.model.ModelInvocationException;
import com.prasad.agentic_software_engineer.orchestration.exception.WorkflowNotFoundException;
import com.prasad.agentic_software_engineer.orchestration.exception.InvalidWorkflowTransitionException;
import com.prasad.agentic_software_engineer.patch.PatchValidationException;
import com.prasad.agentic_software_engineer.workspace.WorkspaceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    ProblemDetail notFound(
            WorkflowNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Workflow not found",
                exception.getMessage(),
                "workflow-not-found"
        );
    }

    @ExceptionHandler({
            WorkspaceException.class,
            PatchValidationException.class,
            IllegalArgumentException.class
    })
    ProblemDetail rejectedInput(
            RuntimeException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Engineering operation rejected",
                exception.getMessage(),
                "engineering-operation-rejected"
        );
    }

    @ExceptionHandler(ModelInvocationException.class)
    ProblemDetail modelFailure(
            ModelInvocationException exception
    ) {
        return problem(
                HttpStatus.BAD_GATEWAY,
                "Engineering model failed",
                exception.getMessage(),
                "model-invocation-failed"
        );
    }

    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    ProblemDetail invalidTransition(
            InvalidWorkflowTransitionException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid workflow transition",
                exception.getMessage(),
                "invalid-workflow-transition"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailure(
            MethodArgumentNotValidException exception
    ) {
        String detail = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(
                        error ->
                                error.getField() +
                                        ": " +
                                        error.getDefaultMessage()
                )
                .findFirst()
                .orElse("Request validation failed");

        return problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                detail,
                "request-validation-failed"
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String type
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);
        problem.setType(
                URI.create(
                        "https://agentic.local/problems/" +
                                type
                )
        );

        return problem;
    }
}
