package com.news_aggregator.backend.payload;

import com.news_aggregator.backend.model.User;

public class AuthResponse {
    private String token;
    private User user;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    // Getters
    public String getToken() { return token; }
    public User getUser() { return user; }
}
