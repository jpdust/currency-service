package com.unstampedpages.currencyservice.exception;

/**
 * Thrown when the upstream currency rates API returns an error response.
 */
public class ExternalApiException extends RuntimeException {

    private final int statusCode;

    public ExternalApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
