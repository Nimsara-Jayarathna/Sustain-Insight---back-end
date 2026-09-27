package com.news_aggregator.backend.service;

/**
 * High-level transactional email use cases used by Sustain Insight business services.
 *
 * <p>This interface intentionally contains no provider-specific types. Callers do not
 * know whether delivery is performed by Brevo, SES, Mailgun, or another provider.</p>
 */
public interface EmailService {

    void sendPasswordResetEmail(String to, String resetLink);

    void sendPasswordChangeNotification(String to, String firstName);

    void sendCurrentEmailVerificationOtp(String to, String otp);

    void sendNewEmailConfirmationOtp(String to, String otp);

    void sendEmailChangeSuccessNotification(String to);

    void sendAccountVerificationEmail(String to, String firstName, String verificationLink);
}
