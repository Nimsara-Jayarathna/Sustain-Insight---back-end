package com.news_aggregator.backend.email.provider;

import brevo.ApiException;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.news_aggregator.backend.email.config.EmailProperties;
import com.news_aggregator.backend.email.exception.EmailDeliveryException;
import com.news_aggregator.backend.email.model.EmailMessage;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Brevo adapter. This is the only class in the application that knows how to
 * translate the provider-neutral {@link EmailMessage} into Brevo SDK objects.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo", matchIfMissing = true)
public class BrevoEmailProvider implements EmailProvider {

    private final TransactionalEmailsApi api;
    private final EmailProperties properties;

    public BrevoEmailProvider(TransactionalEmailsApi api, EmailProperties properties) {
        this.api = api;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        validateConfiguration();

        SendSmtpEmail request = new SendSmtpEmail();
        request.setSubject(message.subject());
        request.setHtmlContent(message.htmlContent());

        SendSmtpEmailSender sender = new SendSmtpEmailSender();
        sender.setEmail(properties.getSenderEmail());
        sender.setName(properties.getSenderName());
        request.setSender(sender);

        SendSmtpEmailTo recipient = new SendSmtpEmailTo();
        recipient.setEmail(message.to());
        request.setTo(Collections.singletonList(recipient));

        try {
            api.sendTransacEmail(request);
            log.info("Transactional email sent successfully via Brevo to {}", message.to());
        } catch (ApiException ex) {
            log.error(
                    "Brevo rejected email delivery to {} (status={}): {}",
                    message.to(),
                    ex.getCode(),
                    ex.getResponseBody());
            throw new EmailDeliveryException(
                    "Email service is currently unavailable. Please try again later.", ex);
        } catch (RuntimeException ex) {
            log.error("Unexpected Brevo email delivery failure for {}", message.to(), ex);
            throw new EmailDeliveryException(
                    "Email service is currently unavailable. Please try again later.", ex);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getBrevo().getApiKey())) {
            throw new EmailDeliveryException("Email service is not configured: BREVO_API_KEY is missing.");
        }
        if (isBlank(properties.getSenderEmail())) {
            throw new EmailDeliveryException("Email service is not configured: BREVO_SENDER_EMAIL is missing.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
