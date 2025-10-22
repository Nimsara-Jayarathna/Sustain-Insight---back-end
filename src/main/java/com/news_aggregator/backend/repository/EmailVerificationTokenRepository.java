package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.EmailVerificationToken;
import com.news_aggregator.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    @Transactional
    void deleteAllByUser(User user);

    int deleteAllByExpiresAtBefore(Instant instant);
}
