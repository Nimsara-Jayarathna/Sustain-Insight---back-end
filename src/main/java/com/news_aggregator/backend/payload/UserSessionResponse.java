package com.news_aggregator.backend.payload;

import java.time.Instant;
import java.util.UUID;

public record UserSessionResponse(
        UUID id,
        String deviceInfo,
        String ipAddress,
        String userAgent,
        String location,
        Instant createdAt,
        Instant lastActiveAt,
        Instant expiresAt,
        boolean active,
        boolean current
) {}
