package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserId(Long userId);

    Optional<UserSession> findById(UUID sessionId);

    @Query("SELECT us FROM UserSession us LEFT JOIN FETCH us.refreshToken LEFT JOIN FETCH us.user WHERE us.id = :sessionId")
    Optional<UserSession> findDetailedById(@Param("sessionId") UUID sessionId);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActiveAt = :now, us.refreshToken = null, us.expiresAt = COALESCE(us.expiresAt, :now) WHERE us.id = :sessionId")
    int deactivateById(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.lastActiveAt = :now, us.refreshToken = null, us.expiresAt = COALESCE(us.expiresAt, :now) WHERE us.user.id = :userId AND us.active = true")
    int deactivateAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.lastActiveAt = :now WHERE us.id = :sessionId")
    int touch(@Param("sessionId") UUID sessionId, @Param("now") Instant now);
}
