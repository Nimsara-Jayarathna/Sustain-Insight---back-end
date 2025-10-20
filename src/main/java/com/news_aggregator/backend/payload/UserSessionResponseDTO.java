package com.news_aggregator.backend.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSessionResponseDTO {
    private UUID id;
    private DeviceInfo device;
    private SessionTimestamps timestamps;
    private SessionStatus status;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceInfo {
        private String name;
        private String rawUserAgent;
        private String ipAddress;
        private String location;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionTimestamps {
        private Instant createdAt;
        private Instant lastActiveAt;
        private Instant expiresAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionStatus {
        private boolean active;
        private boolean current;
    }
}
