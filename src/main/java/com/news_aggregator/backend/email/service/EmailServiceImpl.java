package com.news_aggregator.backend.email.service;

import com.news_aggregator.backend.email.model.EmailMessage;
import com.news_aggregator.backend.email.provider.EmailProvider;
import com.news_aggregator.backend.email.template.EmailTemplateService;
import com.news_aggregator.backend.service.EmailService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Sustain Insight transactional email use-case implementation.
 *
 * <p>This layer selects subjects/templates and delegates physical delivery to the
 * provider abstraction. No Brevo SDK type is allowed here.</p>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final EmailProvider emailProvider;
    private final EmailTemplateService templateService;

    public EmailServiceImpl(EmailProvider emailProvider, EmailTemplateService templateService) {
        this.emailProvider = emailProvider;
        this.templateService = templateService;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        String html = templateService.render(
                "password-reset",
                Map.of("resetLink", resetLink));
        send(to, "Reset Your Password – " + templateService.getBrandName(), html);
    }

    @Override
    public void sendPasswordChangeNotification(String to, String firstName) {
        String html = templateService.render(
                "password-change",
                Map.of("firstName", safeName(firstName)));
        send(to, "Security Alert: Your " + templateService.getBrandName() + " Password Was Changed", html);
    }

    @Override
    public void sendCurrentEmailVerificationOtp(String to, String otp) {
        String html = templateService.render(
                "current-email-otp",
                Map.of("otp", otp));
        send(to, "Verify Your Email Change Request", html);
    }

    @Override
    public void sendNewEmailConfirmationOtp(String to, String otp) {
        String html = templateService.render(
                "new-email-otp",
                Map.of("otp", otp));
        send(to, "Confirm Your New Email Address", html);
    }

    @Override
    public void sendEmailChangeSuccessNotification(String to) {
        String html = templateService.render(
                "email-change-success",
                Map.of("newEmail", to));
        send(to, "Your " + templateService.getBrandName() + " Email Has Been Changed", html);
    }

    @Override
    public void sendAccountVerificationEmail(String to, String firstName, String verificationLink) {
        String html = templateService.render(
                "account-verification",
                Map.of(
                        "firstName", safeName(firstName),
                        "verificationLink", verificationLink));
        send(to, "Verify Your Account – " + templateService.getBrandName(), html);
    }

    private void send(String to, String subject, String html) {
        emailProvider.send(new EmailMessage(to, subject, html));
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "there" : name.trim();
    }
}
