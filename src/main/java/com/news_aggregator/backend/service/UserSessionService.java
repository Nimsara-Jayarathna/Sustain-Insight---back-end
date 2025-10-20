package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.model.UserSession;
import com.news_aggregator.backend.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public UserSession createSession(
            User user,
            RefreshToken refreshToken,
            String ip,
            String deviceInfo,
            String userAgent,
            String location
    ) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(ip);
        session.setDeviceInfo(deviceInfo);
        session.setUserAgent(userAgent);
        session.setLocation(location);
        if (refreshToken != null) {
            session.setExpiresAt(refreshToken.getExpiryDate());
            refreshToken.setSession(session);
        }
        return userSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> findById(UUID sessionId) {
        return userSessionRepository.findById(sessionId);
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> findByIdWithRefreshToken(UUID sessionId) {
        return userSessionRepository.findDetailedById(sessionId);
    }

    @Transactional(readOnly = true)
    public List<UserSession> findByUserId(Long userId) {
        return userSessionRepository.findByUserId(userId);
    }

    @Transactional
    public void deactivateSession(UUID sessionId) {
        userSessionRepository.deactivateById(sessionId, Instant.now());
    }

    @Transactional
    public void deactivateAllSessions(Long userId) {
        userSessionRepository.deactivateAllByUserId(userId, Instant.now());
    }

    @Transactional
    public void updateLastActive(UUID sessionId) {
        userSessionRepository.touch(sessionId, Instant.now());
    }
}
