package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
