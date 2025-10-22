package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.model.UserSession;
import com.news_aggregator.backend.repository.RefreshTokenRepository;
import com.news_aggregator.backend.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}") // Default: 7 days
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserSessionRepository userSessionRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userSessionRepository = userSessionRepository;
    }

    // ============================================================
    // 🔹 CREATE TOKEN
    // ============================================================
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.debug("Creating refresh token for user {}", user.getId());
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
        log.trace("Fetching refresh token by token string");
        return refreshTokenRepository.findDetailedByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
    }

    // ============================================================
    // 🔹 VERIFY EXPIRATION
    // ============================================================
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.info("Refresh token {} expired at {}", token.getId(), token.getExpiryDate());
            deleteTokenAndDeactivateSession(token);
            throw new IllegalArgumentException("Refresh token expired. Please login again.");
        }
        return token;
    }

    // ============================================================
    // 🔹 DELETE TOKEN
    // ============================================================
    @Transactional
    public void deleteByToken(String token) {
        log.debug("Deleting refresh token by token value");
        refreshTokenRepository.findDetailedByToken(token).ifPresent(this::deleteTokenAndDeactivateSession);
    }

    // ============================================================
    // 🔹 DELETE BY USER
    // ============================================================
    @Transactional
    public void deleteByUser(User user) {
        log.info("Revoking all sessions for user {}", user.getId());
        userSessionRepository.deactivateAllByUserId(user.getId(), Instant.now());
        refreshTokenRepository.deleteByUser(user);
    }

    // ============================================================
    // 🔹 REVOKE SINGLE SESSION
    // ============================================================
    @Transactional
    public void revokeSession(UUID sessionId) {
        log.debug("Revoking single session {}", sessionId);
        UserSession session = userSessionRepository.findDetailedById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        RefreshToken refreshToken = session.getRefreshToken();
        Instant now = Instant.now();

        session.setActive(false);
        session.setLastActiveAt(now);
        if (session.getExpiresAt() == null) {
            session.setExpiresAt(now);
        }
        session.setRefreshToken(null);
        userSessionRepository.save(session);
        log.trace("Session {} marked inactive and detached from refresh token", sessionId);

        if (refreshToken != null) {
            refreshToken.setSession(null);
            refreshTokenRepository.delete(refreshToken);
            log.trace("Refresh token {} deleted for session {}", refreshToken.getId(), sessionId);
        }
    }

    @Transactional
    protected void deleteTokenAndDeactivateSession(RefreshToken refreshToken) {
        UserSession session = refreshToken.getSession();
        if (session != null) {
            UUID sessionId = session.getId();
            Instant now = Instant.now();
            log.trace("Deactivating session {} while deleting refresh token {}", sessionId, refreshToken.getId());

            session.setActive(false);
            session.setLastActiveAt(now);
            if (session.getExpiresAt() == null) {
                session.setExpiresAt(now);
            }
            session.setRefreshToken(null);
            userSessionRepository.save(session);

            refreshToken.setSession(null);
            log.trace("Session {} marked inactive and detached from refresh token {}", sessionId, refreshToken.getId());
        }
        log.trace("Deleting refresh token {}", refreshToken.getId());
        refreshTokenRepository.delete(refreshToken);
    }
}
