package com.unstampedpages.currencyservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExternalApiException.class)
    public ProblemDetail handleExternalApiException(ExternalApiException ex) {
        log.error("Upstream currency API error (HTTP {}): {}", ex.getStatusCode(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Upstream API Error");
        problem.setDetail("The currency rates provider returned an error. Please retry shortly.");
        problem.setType(URI.create("urn:currency-service:upstream-error"));
        return problem;
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleNetworkException(ResourceAccessException ex) {
        log.error("Network error reaching currency API: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Upstream Unreachable");
        problem.setDetail("Unable to connect to the currency rates provider. Please retry shortly.");
        problem.setType(URI.create("urn:currency-service:network-error"));
        return problem;
    }
}
