package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final String secret = "KEHIA+WRCLzEB7OGXHDswrRfa8rj+B7QcFfjIs/lpWZhJGpprBdePalNWnQI1WTuGl8X7T7xiLul3mmg3aWPhw=="; // replace with env variable in production
    private final long expirationMs = 86400000; // 1 day

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
}
