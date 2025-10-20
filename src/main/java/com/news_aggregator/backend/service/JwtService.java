package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms:900000}")      // 15 min
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")  // 7 days
    private long refreshExpirationMs;

    @PostConstruct
    public void init() {
        System.out.println("✅ JwtService initialized | access=" + accessExpirationMs +
                " ms | refresh=" + refreshExpirationMs + " ms");
    }

    // ----------------------------------------------------------------
    // 🔹 Access / Refresh Token Generation
    // ----------------------------------------------------------------
    public String generateAccessToken(User user, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("type", "access");
        if (sessionId != null) {
            claims.put("sid", sessionId.toString());
        }
        return createToken(claims, user.getEmail(), accessExpirationMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, user.getEmail(), refreshExpirationMs);
    }

    // ----------------------------------------------------------------
    // 🔹 Core Token Builder
    // ----------------------------------------------------------------
    private String createToken(Map<String, Object> claims, String subject, long validityMs) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // ----------------------------------------------------------------
    // 🔹 Extract / Validate
    // ----------------------------------------------------------------
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    public UUID extractSessionId(String token) {
        return extractClaim(token, claims -> {
            Object sid = claims.get("sid");
            if (sid instanceof String s) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        });
    }
}
