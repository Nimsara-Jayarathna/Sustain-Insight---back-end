package com.news_aggregator.backend.email.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import com.news_aggregator.backend.email.config.EmailProperties;
import com.news_aggregator.backend.email.exception.EmailDeliveryException;
import com.news_aggregator.backend.email.model.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BrevoEmailProviderTest {

    @Mock private TransactionalEmailsApi api;
    private EmailProperties properties;
    private BrevoEmailProvider provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new EmailProperties();
        properties.setSenderEmail("no-reply@example.com");
        properties.setSenderName("Sustain Insight");
        properties.getBrevo().setApiKey("test-key");
        provider = new BrevoEmailProvider(api, properties);
    }

    @Test
    void sendDelegatesToBrevo() throws Exception {
        provider.send(new EmailMessage("user@example.com", "Subject", "<html>Hello</html>"));
        verify(api).sendTransacEmail(any());
    }

    @Test
    void sendWrapsBrevoErrorsInProviderNeutralException() throws Exception {
        when(api.sendTransacEmail(any())).thenThrow(new ApiException("boom"));

        assertThatThrownBy(() -> provider.send(
                new EmailMessage("user@example.com", "Subject", "<html>Hello</html>")))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void sendFailsFastWhenApiKeyIsMissing() {
        properties.getBrevo().setApiKey("");

        assertThatThrownBy(() -> provider.send(
                new EmailMessage("user@example.com", "Subject", "<html>Hello</html>")))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }
}
