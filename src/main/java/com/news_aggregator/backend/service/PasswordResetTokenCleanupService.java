package com.news_aggregator.backend.service;

import com.news_aggregator.backend.repository.PasswordResetTokenRepository;
import com.news_aggregator.backend.model.PasswordResetToken;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordResetTokenCleanupService {

    private final PasswordResetTokenRepository tokenRepo;

    public PasswordResetTokenCleanupService(PasswordResetTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @Scheduled(cron = "0 * * * * *")
    public void cleanupExpiredTokens() {
        List<PasswordResetToken> expired = tokenRepo.findAllByExpiresAtBefore(LocalDateTime.now());
        System.out.println("🕒 Cleanup job triggered at: " + LocalDateTime.now());

        if (!expired.isEmpty()) {
            tokenRepo.deleteAll(expired);
            System.out.println("🧹 Deleted " + expired.size() + " expired password reset tokens.");
        }
    }
}
