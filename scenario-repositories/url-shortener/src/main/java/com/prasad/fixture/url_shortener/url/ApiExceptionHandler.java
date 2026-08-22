package com.prasad.fixture.url_shortener.url;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ShortUrlNotFoundException.class)
    ProblemDetail handleNotFound(
            ShortUrlNotFoundException exception
    ) {
        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        detail.setType(
                URI.create(
                        "https://example.com/problems/short-url-not-found"
                )
        );
        detail.setTitle("Short URL not found");

        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed"
                );

        detail.setType(
                URI.create(
                        "https://example.com/problems/validation"
                )
        );
        detail.setTitle("Invalid request");

        return detail;
    }
}