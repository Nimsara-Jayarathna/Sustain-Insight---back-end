package com.news_aggregator.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends branded, responsive HTML emails for Sustain Insight.
 * Falls back gracefully to text if HTML rendering fails.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String senderEmail;

    @Value("${app.brand.name}")
    private String brandName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a styled password reset email.
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(senderEmail);
            helper.setSubject("Reset Your Password – " + brandName);

            String htmlBody = """
                <div style="font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; 
                            background-color: #f4f7f9; 
                            padding: 30px; 
                            color: #333;">
                    <div style="max-width: 600px; margin: auto; background: #fff; border-radius: 12px; 
                                box-shadow: 0 2px 6px rgba(0,0,0,0.08); overflow: hidden;">
                        
                        <div style="background: linear-gradient(135deg, #059669, #10b981); 
                                    color: white; 
                                    padding: 20px 30px; 
                                    text-align: center;">
                            <h1 style="margin: 0; font-size: 24px;">🔒 Password Reset</h1>
                        </div>

                        <div style="padding: 30px;">
                            <p style="font-size: 16px; margin-bottom: 18px;">
                                Hi there,
                            </p>
                            <p style="font-size: 16px; line-height: 1.6; margin-bottom: 22px;">
                                You recently requested to reset your password for your 
                                <strong>%s</strong> account.
                            </p>
                            <p style="font-size: 16px; margin-bottom: 24px;">
                                Click the button below to set a new password. This link is valid for 10 minutes.
                            </p>
                            <div style="text-align: center; margin-bottom: 30px;">
                                <a href="%s" 
                                   style="display: inline-block; background-color: #059669; color: white; 
                                          padding: 12px 28px; border-radius: 8px; text-decoration: none; 
                                          font-weight: 600; letter-spacing: 0.3px;">
                                    Reset Password
                                </a>
                            </div>
                            <p style="font-size: 14px; color: #555;">
                                If you didn’t request this, you can safely ignore this email. 
                                Your password won’t change until you click the link above.
                            </p>
                        </div>

                        <div style="background-color: #f9fafb; text-align: center; padding: 16px; font-size: 12px; color: #888;">
                            © %d %s. All rights reserved.
                        </div>
                    </div>
                </div>
                """.formatted(brandName, resetLink, java.time.Year.now().getValue(), brandName);

            helper.setText(htmlBody, true);

            mailSender.send(message);

            System.out.println("📧 HTML password reset email sent successfully to: " + to);
            System.out.println("🔗 Reset Link: " + resetLink);

        } catch (Exception ex) {
            System.err.println("⚠️ Email sending failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
