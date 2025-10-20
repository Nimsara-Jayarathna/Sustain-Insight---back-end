package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.exception.SessionExpiredException;
import com.news_aggregator.backend.payload.*;
import com.news_aggregator.backend.service.*;
import com.news_aggregator.backend.model.PasswordResetToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.PasswordResetTokenRepository;
import com.news_aggregator.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.frontend-url}", allowCredentials = "true")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ============================================================
    // 🔹 SIGNUP
    // ============================================================
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            authService.signup(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Signup successful! Please check your email to verify your account."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("EMAIL_EXISTS", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Failed to create account."));
        }
    }

    // ============================================================
    // 🔹 LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String userAgentHeader = Optional.ofNullable(request.getUserAgent())
                    .filter(s -> !s.isBlank())
                    .orElse(httpRequest.getHeader("User-Agent"));

            AuthResponse authResponse = authService.login(
                    request,
                    resolveClientIp(httpRequest),
                    request.getDeviceInfo(),
                    userAgentHeader,
                    request.getLocation()
            );

            // 🔐 Set refresh token as HttpOnly Secure cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth")
                    .sameSite("Strict")
                    .maxAge(Duration.ofDays(7))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of(
                            "id", authResponse.getId(),
                            "firstName", authResponse.getFirstName(),
                            "lastName", authResponse.getLastName(),
                            "email", authResponse.getEmail(),
                            "accessToken", authResponse.getAccessToken(),
                            "sessionId", authResponse.getSessionId(),
                            "message", "Login successful."
                    ));

        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("EMAIL_NOT_VERIFIED", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Login failed."));
        }
    }

    // ============================================================
    // 🔹 LOGOUT
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        try {
            if (refreshToken != null && !refreshToken.isBlank()) {
                refreshTokenService.deleteByToken(refreshToken);
            }

            // Delete the refresh cookie
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth")
                    .sameSite("Strict")
                    .maxAge(0)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(Map.of("message", "Logged out successfully."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Failed to logout."));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ============================================================
    // 🔹 VERIFY EMAIL
    // ============================================================
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully! You can now log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_TOKEN", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Verification failed."));
        }
    }

    // ============================================================
    // 🔹 RESEND VERIFICATION
    // ============================================================
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request) {
        try {
            authService.resendVerificationEmail(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "If an account with that email exists and is not verified, a new verification link has been sent."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Unable to resend verification email."));
        }
    }

    // ============================================================
// 🔹 FORGOT PASSWORD
// ============================================================
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
    try {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("MISSING_EMAIL", "Email is required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "No account found with this email address."));
        }

        User user = userOpt.get();

        // 🔹 Check last token request (by most recent createdAt)
        Optional<PasswordResetToken> existingTokenOpt =
                tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());

        if (existingTokenOpt.isPresent()) {
            PasswordResetToken existingToken = existingTokenOpt.get();

            // Enforce 2-minute cooldown before issuing new token
            if (existingToken.getCreatedAt() != null &&
                existingToken.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(2))) {

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("RATE_LIMIT",
                                "A reset link was already sent recently. Please wait 2 minutes before requesting another one."));
            }

            // Remove previous token if expired or cooldown window passed
            tokenRepository.delete(existingToken);
        }

        // 🔹 Create new token (10-minute validity)
        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
                resetToken,
                user,
                LocalDateTime.now().plusMinutes(10)
        );
        tokenRepository.save(token);

        // 🔹 Build reset link and send email
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return ResponseEntity.ok(Map.of(
                "message", "Password reset link sent successfully. Please check your email."
        ));

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR",
                        "Unable to process password reset request at the moment."));
    }
}


    // ============================================================
    // 🔹 RESET PASSWORD
    // ============================================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("password");

            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("MISSING_TOKEN", "Reset token is required."));
            }
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("MISSING_PASSWORD", "New password is required."));
            }

            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_TOKEN", "Invalid or expired reset link."));
            }

            PasswordResetToken resetToken = tokenOpt.get();
            if (resetToken.isExpired()) {
                tokenRepository.delete(resetToken);
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(new ErrorResponse("TOKEN_EXPIRED", "This reset link has expired."));
            }

            User user = resetToken.getUser();
            user.setPasswordHash(authService.encodePassword(newPassword));
            userRepository.save(user);
            tokenRepository.delete(resetToken);

            return ResponseEntity.ok(Map.of("message", "Password has been successfully updated."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Unable to reset password at this time."));
        }
    }

    // ============================================================
    // 🔹 REFRESH TOKEN (from HttpOnly cookie)
    // ============================================================
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("MISSING_TOKEN", "Missing refresh token cookie."));
        }

        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(Map.of(
                    "accessToken", response.getAccessToken(),
                    "sessionId", response.getSessionId()
            ));
        } catch (SessionExpiredException e) {
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth")
                    .sameSite("Strict")
                    .maxAge(0)
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorResponse("SESSION_EXPIRED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth")
                    .sameSite("Strict")
                    .maxAge(0)
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorResponse("INVALID_TOKEN", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Failed to refresh access token."));
        }
    }
}
