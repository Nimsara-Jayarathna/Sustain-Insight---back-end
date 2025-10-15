package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.RefreshTokenRepository;
import com.news_aggregator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}") // Default: 7 days
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    // ============================================================
    // 🔹 CREATE TOKEN
    // ============================================================
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Optional: Remove existing token(s)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setCreatedAt(Instant.now());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        return refreshTokenRepository.save(token);
    }

    // ============================================================
    // 🔹 GET BY TOKEN STRING
    // ============================================================
    public RefreshToken getByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
    }

    // ============================================================
    // 🔹 VERIFY EXPIRATION
    // ============================================================
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token expired. Please login again.");
        }
        return token;
    }

    // ============================================================
    // 🔹 DELETE TOKEN
    // ============================================================
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    // ============================================================
    // 🔹 DELETE BY USER
    // ============================================================
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
