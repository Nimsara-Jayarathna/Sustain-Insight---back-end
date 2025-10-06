package com.news_aggregator.backend.service; // Use your actual package name

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// *** IMPORT STATEMENTS HAVE BEEN UPDATED ***
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
// ******************************************

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

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            Gmail service = getGmailService();
            MimeMessage email = createEmail(to, resetLink);
            String encodedEmail = createMessageWithEmail(email);
            Message message = new Message();
            message.setRaw(encodedEmail);
            service.users().messages().send("me", message).execute();
            System.out.println("✅ Password reset email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send email via Gmail API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MimeMessage createEmail(String to, String resetLink) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(senderEmail));
        // *** UPDATED RECIPIENT TYPE ***
        email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
        // ******************************
        email.setSubject("Reset Your Password – " + brandName);

        String htmlBody = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset</title>
                <style>
                    body { background-color: #f2f4f7; font-family: 'Inter', sans-serif; margin: 0; padding: 0; color: #2d2d2d; }
                    .wrapper { width: 100%%; padding: 30px 0; }
                    .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06); overflow: hidden; }
                    .header { background: linear-gradient(135deg, #16a34a, #10b981, #06b6d4); text-align: center; padding: 28px 20px; color: #fff; }
                    .header h1 { margin: 0; font-size: 22px; font-weight: 600; }
                    .content { padding: 32px; line-height: 1.7; font-size: 15px; }
                    .content p { margin-bottom: 18px; }
                    .button-wrapper { text-align: center; margin: 30px 0; }
                    .button { display: inline-block; background: linear-gradient(135deg, #14b8a6, #0ea5e9); color: white; text-decoration: none; font-weight: 600; padding: 12px 28px; border-radius: 50px; letter-spacing: 0.4px; }
                    .footer { background-color: #f9fafb; text-align: center; padding: 20px; font-size: 12px; color: #7a7a7a; }
                    .footer a { color: #0ea5e9; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <div class="email-container">
                        <div class="header"><h1>🔐 Reset Your Password</h1></div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>We received a request to reset your password for your <strong>%s</strong> account.</p>
                            <p>If you made this request, click the button below to set a new password. This link is valid for <strong>10 minutes</strong>.</p>
                            <div class="button-wrapper"><a href="%s" class="button">Reset Password</a></div>
                            <p>If you didn’t request a password reset, no action is needed — your account is safe.</p>
                        </div>
                        <div class="footer">
                            &copy; %d %s &nbsp;|&nbsp; <a href="https://sustaininsight.com">Visit %s</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.formatted(brandName, resetLink, Year.now().getValue(), brandName, brandName);

        email.setContent(htmlBody, "text/html; charset=utf-8");
        return email;
    }

    private String createMessageWithEmail(MimeMessage email) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        return Base64.encodeBase64URLSafeString(bytes);
    }
}