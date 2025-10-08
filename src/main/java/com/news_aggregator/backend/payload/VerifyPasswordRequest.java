package com.news_aggregator.backend.payload;

public class VerifyPasswordRequest {
    private String currentPassword;

    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
