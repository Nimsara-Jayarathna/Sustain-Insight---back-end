package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.UserSession;
import com.news_aggregator.backend.payload.UserSessionResponseDTO;
import com.news_aggregator.backend.util.DeviceInfoExtractor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class UserSessionMapper {

    public UserSessionResponseDTO mapToDto(UserSession session, UUID currentSessionId) {
        if (session == null) {
            return null;
        }

        UserSessionResponseDTO.DeviceInfo deviceInfo = new UserSessionResponseDTO.DeviceInfo(
                safeString(session.getDeviceInfo()),
                safeString(session.getUserAgent()),
                DeviceInfoExtractor.normalizeIp(session.getIpAddress()),
                safeString(session.getLocation())
        );

        UserSessionResponseDTO.SessionTimestamps timestamps = new UserSessionResponseDTO.SessionTimestamps(
                session.getCreatedAt(),
                session.getLastActiveAt(),
                session.getExpiresAt()
        );

        boolean isCurrent = session.getId() != null && session.getId().equals(currentSessionId);
        UserSessionResponseDTO.SessionStatus status = new UserSessionResponseDTO.SessionStatus(
                session.isActive(),
                isCurrent
        );

        return new UserSessionResponseDTO(
                session.getId(),
                deviceInfo,
                timestamps,
                status
        );
    }

    private String safeString(String value) {
        return Objects.toString(value, "");
    }
}
