package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Query("SELECT rt FROM RefreshToken rt LEFT JOIN FETCH rt.session WHERE rt.token = :token")
    Optional<RefreshToken> findDetailedByToken(@Param("token") String token);
    void deleteByUser(User user);
}
