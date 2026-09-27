package com.news_aggregator.backend.email.config;

import brevoApi.TransactionalEmailsApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider SDK configuration kept separate from business email logic.
 */
@Configuration
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo", matchIfMissing = true)
public class EmailConfig {

    @Bean
    public TransactionalEmailsApi transactionalEmailsApi(EmailProperties properties) {
        brevo.ApiClient client = brevo.Configuration.getDefaultApiClient();
        client.setApiKey(properties.getBrevo().getApiKey());
        return new TransactionalEmailsApi(client);
    }
}
