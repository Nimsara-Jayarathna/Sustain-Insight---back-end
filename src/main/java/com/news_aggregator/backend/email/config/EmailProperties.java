package com.news_aggregator.backend.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Central configuration contract for transactional email.
 */
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private String provider = "brevo";
    private String senderEmail = "";
    private String senderName = "Sustain Insight";
    private final Brevo brevo = new Brevo();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Brevo getBrevo() {
        return brevo;
    }

    public static class Brevo {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
