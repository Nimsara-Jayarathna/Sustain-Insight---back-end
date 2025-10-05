package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserId(Long userId);

    List<PasswordResetToken> findAllByExpiresAtBefore(LocalDateTime now);

    void deleteByUserId(Long userId);
}

