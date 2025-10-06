package com.news_aggregator.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Year;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${mail.from}")
    private String senderEmail;

    @Value("${app.brand.name}")
    private String brandName;

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            // 🌿 Elegant, brand-themed HTML email
            String htmlBody = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Password Reset</title>
                    <style>
                        body {
                            background-color: #f2f4f7;
                            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                            margin: 0;
                            padding: 0;
                            color: #2d2d2d;
                        }
                        .wrapper {
                            width: 100%%;
                            padding: 30px 0;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: 0 auto;
                            background: #ffffff;
                            border-radius: 14px;
                            box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #16a34a, #10b981, #06b6d4);
                            text-align: center;
                            padding: 28px 20px;
                            color: #fff;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 22px;
                            font-weight: 600;
                            letter-spacing: 0.3px;
                        }
                        .content {
                            padding: 32px;
                            line-height: 1.7;
                            font-size: 15px;
                        }
                        .content p {
                            margin-bottom: 18px;
                        }
                        .button-wrapper {
                            text-align: center;
                            margin: 30px 0;
                        }
                        .button {
                            display: inline-block;
                            background: linear-gradient(135deg, #14b8a6, #0ea5e9);
                            color: white;
                            text-decoration: none;
                            font-weight: 600;
                            padding: 12px 28px;
                            border-radius: 50px;
                            letter-spacing: 0.4px;
                            transition: all 0.25s ease;
                            box-shadow: 0 3px 10px rgba(14,165,233,0.25);
                        }
                        .button:hover {
                            background: linear-gradient(135deg, #0ea5e9, #14b8a6);
                            box-shadow: 0 4px 14px rgba(14,165,233,0.35);
                        }
                        .footer {
                            background-color: #f9fafb;
                            text-align: center;
                            padding: 20px;
                            font-size: 12px;
                            color: #7a7a7a;
                        }
                        .footer a {
                            color: #0ea5e9;
                            text-decoration: none;
                        }
                        @media (prefers-color-scheme: dark) {
                            body { background-color: #0f172a; color: #e2e8f0; }
                            .email-container { background-color: #1e293b; }
                            .footer { background-color: #0f172a; color: #94a3b8; }
                        }
                    </style>
                </head>
                <body>
                    <div class="wrapper">
                        <div class="email-container">
                            <div class="header">
                                <h1>🔐 Reset Your Password</h1>
                            </div>
                            <div class="content">
                                <p>Hello,</p>
                                <p>We received a request to reset your password for your <strong>%s</strong> account.</p>
                                <p>If you made this request, click the button below to set a new password. This link is valid for <strong>10 minutes</strong>.</p>
                                <div class="button-wrapper">
                                    <a href="%s" class="button">Reset Password</a>
                                </div>
                                <p>If you didn’t request a password reset, no action is needed — your account is safe.</p>
                            </div>
                            <div class="footer">
                                &copy; %d %s &nbsp;|&nbsp; 
                                <a href="https://sustaininsight.com">Visit %s</a>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            """.formatted(brandName, resetLink, Year.now().getValue(), brandName, brandName)
              .replace("\"", "\\\"")
              .replace("\n", "")
              .replace("\r", "");

            // ✅ Build JSON payload
            String jsonPayload = """
                {
                  "from": "%s",
                  "to": "%s",
                  "subject": "Reset Your Password – %s",
                  "html": "%s"
                }
            """.formatted(senderEmail, to, brandName, htmlBody);

            // ✅ Send via Resend API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📧 Resend API → Status: " + response.statusCode());
            System.out.println("📧 Response: " + response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Password reset email sent successfully to: " + to);
            } else {
                System.err.println("❌ Resend API failed: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("⚠️ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
