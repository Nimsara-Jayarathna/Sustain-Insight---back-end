package com.news_aggregator.backend.payload;

public class AuthResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String token;

    public AuthResponse(Long id, String firstName, String lastName, String email, String token) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.token = token;
    }

    // Getters
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
}
