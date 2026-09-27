package com.news_aggregator.backend.email.provider;

import com.news_aggregator.backend.email.model.EmailMessage;

/**
 * Low-level delivery boundary for transactional email.
 *
 * <p>New delivery integrations should implement this interface instead of leaking
 * provider SDK types into business services.</p>
 */
public interface EmailProvider {
    void send(EmailMessage message);
}
