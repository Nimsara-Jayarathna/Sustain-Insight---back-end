package com.news_aggregator.backend.email.exception;

/**
 * Provider-neutral exception raised when an outbound email cannot be delivered.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
