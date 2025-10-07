package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.EmailVerificationToken;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.payload.AuthResponse;
import com.news_aggregator.backend.payload.LoginRequest;
import com.news_aggregator.backend.payload.SignupRequest;
import com.news_aggregator.backend.repository.EmailVerificationTokenRepository;
import com.news_aggregator.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        // Send email verification
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

            User authenticatedUser = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(authenticatedUser);

            return new AuthResponse(
                    authenticatedUser.getId(),
                    authenticatedUser.getFirstName(),
                    authenticatedUser.getLastName(),
                    authenticatedUser.getEmail(),
                    token
            );

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }
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
    // 🔹 PASSWORD ENCODER HELPER (used in reset-password)
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
