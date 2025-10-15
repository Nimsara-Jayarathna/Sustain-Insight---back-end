package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.EmailVerificationToken;
import com.news_aggregator.backend.model.RefreshToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.payload.AuthResponse;
import com.news_aggregator.backend.payload.LoginRequest;
import com.news_aggregator.backend.payload.SignupRequest;
import com.news_aggregator.backend.repository.EmailVerificationTokenRepository;
import com.news_aggregator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EmailService emailService;
    @Autowired private RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ============================================================
    // 🔹 SIGNUP
    // ============================================================
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setJobTitle(request.getJobTitle());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        createAndSendVerificationToken(savedUser);
    }

    // ============================================================
    // 🔹 LOGIN
    // ============================================================
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!user.isEmailVerified()) {
            throw new LockedException("Your email has not been verified. Please check your inbox.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    accessToken,
                    refreshToken.getToken()
            );

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    // ============================================================
    // 🔹 REFRESH TOKEN
    // ============================================================
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenService.getByToken(refreshTokenStr);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                newAccessToken,
                refreshTokenStr
        );
    }

    // ============================================================
    // 🔹 VERIFY EMAIL
    // ============================================================
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Expired verification token.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
    }

    // ============================================================
    // 🔹 RESEND VERIFICATION EMAIL
    // ============================================================
    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                tokenRepository.deleteAllByUser(user);
                createAndSendVerificationToken(user);
            }
        });
    }

    // ============================================================
    // 🔹 PASSWORD ENCODER HELPER
    // ============================================================
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // ============================================================
    // 🔹 INTERNAL UTILITY
    // ============================================================
    private void createAndSendVerificationToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(tokenValue);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        tokenRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + tokenValue;
        emailService.sendAccountVerificationEmail(user.getEmail(), user.getFirstName(), verificationLink);
    }
}
