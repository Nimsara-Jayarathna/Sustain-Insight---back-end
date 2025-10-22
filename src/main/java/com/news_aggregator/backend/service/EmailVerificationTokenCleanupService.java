package com.news_aggregator.backend.service;

import com.news_aggregator.backend.repository.EmailVerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EmailVerificationTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationTokenCleanupService.class);

    private final EmailVerificationTokenRepository tokenRepository;

    public EmailVerificationTokenCleanupService(EmailVerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    @Scheduled(cron = "0 30 * * * *")
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int deleted = tokenRepository.deleteAllByExpiresAtBefore(now);

        if (deleted > 0) {
            log.info("🧹 Removed {} expired email verification tokens at {}", deleted, now);
        }
    }
}
