package com.news_aggregator.backend.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String accessToken;
    private String refreshToken;
}
