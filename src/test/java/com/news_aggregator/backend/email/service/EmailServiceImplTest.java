package com.news_aggregator.backend.email.service;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.news_aggregator.backend.email.model.EmailMessage;
import com.news_aggregator.backend.email.provider.EmailProvider;
import com.news_aggregator.backend.email.template.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EmailServiceImplTest {

    @Mock private EmailProvider provider;
    @Mock private EmailTemplateService templates;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(templates.getBrandName()).thenReturn("Sustain Insight");
        service = new EmailServiceImpl(provider, templates);
    }

    @Test
    void passwordResetUsesTemplateAndProviderBoundary() {
        when(templates.render(eq("password-reset"), anyMap())).thenReturn("<html>reset</html>");

        service.sendPasswordResetEmail("user@example.com", "https://example.test/reset");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(provider).send(captor.capture());
        EmailMessage message = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(message.to()).isEqualTo("user@example.com");
        org.assertj.core.api.Assertions.assertThat(message.subject()).isEqualTo("Reset Your Password – Sustain Insight");
        org.assertj.core.api.Assertions.assertThat(message.htmlContent()).isEqualTo("<html>reset</html>");
    }

    @Test
    void accountVerificationUsesProviderWithoutBrevoTypes() {
        when(templates.render(eq("account-verification"), anyMap())).thenReturn("<html>verify</html>");

        service.sendAccountVerificationEmail("user@example.com", "Nimal", "https://example.test/verify");

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(provider).send(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().subject())
                .isEqualTo("Verify Your Account – Sustain Insight");
    }
}
