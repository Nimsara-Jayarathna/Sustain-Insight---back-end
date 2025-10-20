package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.model.UserSession;
import com.news_aggregator.backend.payload.ErrorResponse;
import com.news_aggregator.backend.payload.UserSessionResponseDTO;
import com.news_aggregator.backend.service.RefreshTokenService;
import com.news_aggregator.backend.service.UserSessionMapper;
import com.news_aggregator.backend.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final UserSessionService userSessionService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionMapper userSessionMapper;

    public SessionController(UserSessionService userSessionService,
                             RefreshTokenService refreshTokenService,
                             UserSessionMapper userSessionMapper) {
        this.userSessionService = userSessionService;
        this.refreshTokenService = refreshTokenService;
        this.userSessionMapper = userSessionMapper;
    }

    @GetMapping
    public ResponseEntity<List<UserSessionResponseDTO>> listActiveSessions(@AuthenticationPrincipal User user,
                                                                           HttpServletRequest request) {
        UUID currentSessionId = (UUID) request.getAttribute("sessionId");
        List<UserSessionResponseDTO> sessions = userSessionService.findByUserId(user.getId()).stream()
                .filter(UserSession::isActive)
                .map(session -> userSessionMapper.mapToDto(session, currentSessionId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeSession(@PathVariable("id") UUID sessionId,
                                           @AuthenticationPrincipal User user,
                                           HttpServletRequest request) {
        UUID currentSessionId = (UUID) request.getAttribute("sessionId");
        log.debug("Request to revoke session {} for user {}", sessionId, user.getId());

        Optional<UserSession> sessionOpt = userSessionService.findByIdWithRefreshToken(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getUser().getId().equals(user.getId())) {
            log.warn("Session {} not found or does not belong to user {}", sessionId, user.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("SESSION_NOT_FOUND", "Session not found."));
        }

        return handleSessionRevocation(sessionOpt.get(), currentSessionId);
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> revokeAllSessions(@AuthenticationPrincipal User user) {
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString())
                .body(Map.of(
                        "message", "All sessions revoked successfully.",
                        "timestamp", Instant.now()
                ));
    }

    private ResponseEntity<?> handleSessionRevocation(UserSession session, UUID currentSessionId) {
        try {
            boolean isCurrent = currentSessionId != null && session.getId().equals(currentSessionId);
            log.debug("Revoking session {} (currentSession={}, refreshTokenPresent={})",
                    session.getId(), isCurrent, session.getRefreshToken() != null);

            if (!session.isActive()) {
                log.info("Session {} already inactive", session.getId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("SESSION_NOT_FOUND", "Session already inactive."));
            }

            refreshTokenService.revokeSession(session.getId());

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            if (isCurrent) {
                builder.header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString());
            }

            Map<String, Object> response = Map.of(
                    "message", "Session revoked successfully.",
                    "revokedSessionId", session.getId(),
                    "currentSessionRevoked", isCurrent
            );
            log.debug("Session {} revoked successfully", session.getId());
            return builder.body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Revocation failed for session {}: {}", session.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("SESSION_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to revoke session {}", session.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Failed to revoke session."));
        }
    }

    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }
}
