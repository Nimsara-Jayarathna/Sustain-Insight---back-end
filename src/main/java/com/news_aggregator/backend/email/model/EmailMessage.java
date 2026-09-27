package com.news_aggregator.backend.email.model;

import java.util.Objects;

/**
 * Provider-neutral representation of an outbound HTML email.
 */
public record EmailMessage(String to, String subject, String htmlContent) {

    public EmailMessage {
        to = requireText(to, "to");
        subject = requireText(subject, "subject");
        htmlContent = requireText(htmlContent, "htmlContent");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
