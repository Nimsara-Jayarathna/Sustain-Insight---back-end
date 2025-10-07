package com.news_aggregator.backend.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Year;
import java.util.Properties;

@Service
public class EmailService {

    private static final String APPLICATION_NAME = "Sustain Insight";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    @Value("${google.sender.email}")
    private String senderEmail;

    @Value("${app.brand.name}")
    private String brandName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private Gmail getGmailService() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build();
        credential.setRefreshToken(refreshToken);
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            Gmail service = getGmailService();
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(senderEmail));
            email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            email.setSubject(subject);
            email.setContent(htmlBody, "text/html; charset=utf-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            String encodedEmail = Base64.encodeBase64URLSafeString(buffer.toByteArray());

            Message message = new Message();
            message.setRaw(encodedEmail);
            service.users().messages().send("me", message).execute();
            System.out.println("✅ Email sent successfully to: " + to + " | Subject: " + subject);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send email via Gmail API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // ===============  PASSWORD EMAILS  =======================
    // =========================================================

    /** Sends password reset email. */
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Reset Your Password – " + brandName;
        String title = "🔐 Reset Your Password";
        String message = String.format(
            "We received a request to reset your password for your <strong>%s</strong> account. " +
            "If you made this request, click the button below to set a new password. This link is valid for <strong>10 minutes</strong>. " +
            "If you didn’t request a password reset, no action is needed.",
            brandName
        );
        String buttonText = "Reset Password";

        String htmlBody = createStyledEmailHtml(title, "Hello,", message, buttonText, resetLink);
        sendEmail(to, subject, htmlBody);
    }

    /** Sends password change confirmation email. */
    public void sendPasswordChangeNotification(String to, String firstName) {
        String subject = "Security Alert: Your " + brandName + " Password Was Changed";
        String title = "🔒 Password Changed Successfully";
        String message = String.format(
            "This is to let you know that the password for your <strong>%s</strong> account was recently changed. " +
            "If you <strong>did not</strong> make this change, please reset your password immediately to protect your account.",
            brandName
        );
        String buttonText = "Secure Your Account";
        String forgotPasswordUrl = frontendUrl + "/forgot-password";

        String htmlBody = createStyledEmailHtml(title, "Hello " + firstName + ",", message, buttonText, forgotPasswordUrl);
        sendEmail(to, subject, htmlBody);
    }

    // =========================================================
    // ============  EMAIL CHANGE FLOW EMAILS  =================
    // =========================================================

    public void sendCurrentEmailVerificationOtp(String to, String otp) {
        String subject = "Verify Your Email Change Request";
        String title = "Verify Your Request";
        String message = "To proceed with changing your email address, please use the following verification code. This code is valid for 5 minutes.";

        String htmlBody = createStyledEmailHtml(title, "Hello,", message, otp);
        sendEmail(to, subject, htmlBody);
    }

    public void sendNewEmailConfirmationOtp(String to, String otp) {
        String subject = "Confirm Your New Email Address";
        String title = "Confirm Your New Email";
        String message = "To complete your email address change, please use the following verification code. This code is valid for 5 minutes.";

        String htmlBody = createStyledEmailHtml(title, "Hello,", message, otp);
        sendEmail(to, subject, htmlBody);
    }

    public void sendEmailChangeSuccessNotification(String to) {
        String subject = "Your " + brandName + " Email Has Been Changed";
        String title = "✅ Email Changed Successfully";
        String message = String.format(
            "This is a confirmation that the email address for your <strong>%s</strong> account has been successfully updated to <strong>%s</strong>.",
            brandName, to
        );

        String htmlBody = createStyledEmailHtml(title, "Hi there,", message, null, null);
        sendEmail(to, subject, htmlBody);
    }

    // Add this new public method inside your existing EmailService.java

    public void sendAccountVerificationEmail(String to, String firstName, String verificationLink) {
        String subject = "Verify Your Account – " + brandName;
        String title = "👋 Welcome! Please Verify Your Email";
        String message = "Thank you for signing up! To complete your registration and secure your account, please click the button below to verify your email address. This link is valid for 24 hours.";
        String buttonText = "Verify My Email";

        String htmlBody = createStyledEmailHtml(title, "Hello " + firstName + ",", message, buttonText, verificationLink);
        sendEmail(to, subject, htmlBody);
    }
    // =========================================================
    // ============  MASTER EMAIL TEMPLATE BUILDER  ============
    // =========================================================

    private String createStyledEmailHtml(String title, String greeting, String message, String callToActionText, String callToActionUrl) {
        String buttonHtml = "";
        if (callToActionText != null && callToActionUrl != null) {
            buttonHtml = String.format("""
                <div class="button-wrapper">
                  <a href="%s" class="button">%s</a>
                </div>
            """, callToActionUrl, callToActionText);
        }

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s</title>
              <style>
                body { background-color: #f2f4f7; font-family: 'Inter', sans-serif; margin: 0; padding: 0; color: #2d2d2d; }
                .wrapper { width: 100%%; padding: 30px 0; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06); overflow: hidden; }
                .header { background: linear-gradient(135deg, #16a34a, #10b981, #06b6d4); text-align: center; padding: 28px 20px; color: #fff; }
                .header h1 { margin: 0; font-size: 22px; font-weight: 600; }
                .content { padding: 32px; line-height: 1.7; font-size: 15px; }
                .content p { margin-bottom: 18px; }
                .button-wrapper { text-align: center; margin: 30px 0; }
                .button { display: inline-block; background: linear-gradient(135deg, #14b8a6, #0ea5e9); color: white !important; text-decoration: none; font-weight: 600; padding: 12px 28px; border-radius: 50px; letter-spacing: 0.4px; }
                .footer { background-color: #f9fafb; text-align: center; padding: 20px; font-size: 12px; color: #7a7a7a; }
                .footer a { color: #0ea5e9; text-decoration: none; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="email-container">
                  <div class="header"><h1>%s</h1></div>
                  <div class="content">
                    <p>%s</p>
                    <p>%s</p>
                    %s
                  </div>
                  <div class="footer">
                    &copy; %d %s &nbsp;|&nbsp; <a href="%s">Visit %s</a>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """,
            title, // for <title> tag
            title, // for header <h1>
            greeting,
            message,
            buttonHtml,
            Year.now().getValue(),
            brandName,
            frontendUrl,
            brandName
        );
    }

    /** Overloaded method for OTP and simple notification emails without a button. */
    private String createStyledEmailHtml(String title, String greeting, String message, String otp) {
         String otpBoxHtml = "";
         if (otp != null && !otp.isEmpty()) {
             otpBoxHtml = String.format("""
                 <div style="margin: 20px auto; font-size: 24px; font-weight: 700; letter-spacing: 4px; background: #ecfdf5; padding: 12px; border-radius: 8px; color: #065f46; text-align: center;">
                   %s
                 </div>
             """, otp);
         }

         // Re-use the main template builder but insert the OTP box instead of a button
         return createStyledEmailHtml(title, greeting, message + otpBoxHtml, null, null);
    }
}
