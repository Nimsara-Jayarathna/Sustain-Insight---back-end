 package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RefreshTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                                      RefreshTokenService refreshTokenService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        List<RefreshToken> expiredTokens = refreshTokenRepository.findAllByExpiryDateBefore(now);

        if (expiredTokens.isEmpty()) {
            return;
        }

        expiredTokens.forEach(refreshTokenService::deleteTokenAndDeactivateSession);
        log.info("🧹 Removed {} expired refresh tokens at {}", expiredTokens.size(), now);
    }
}
