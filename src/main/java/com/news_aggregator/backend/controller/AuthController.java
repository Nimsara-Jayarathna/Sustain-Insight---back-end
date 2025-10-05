package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.model.PasswordResetToken;
import com.news_aggregator.backend.payload.AuthResponse;
import com.news_aggregator.backend.payload.ErrorResponse;
import com.news_aggregator.backend.payload.LoginRequest;
import com.news_aggregator.backend.payload.SignupRequest;
import com.news_aggregator.backend.repository.UserRepository;
import com.news_aggregator.backend.repository.PasswordResetTokenRepository;
import com.news_aggregator.backend.service.JwtService;
import com.news_aggregator.backend.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 🔹 SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ErrorResponse("EMAIL_EXISTS", "This email is already registered.")
                );
            }

            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);

            String token = jwtService.generateToken(user);
            System.out.println("DEBUG → Signup success for: " + user.getEmail());

            return ResponseEntity.ok(new AuthResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    token
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("SERVER_ERROR", "An error occurred while creating your account.")
            );
        }
    }

    // 🔹 LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ErrorResponse("USER_NOT_FOUND", "No account found for this email address.")
                );
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            System.out.println("DEBUG → Login success for: " + user.getEmail());
            return ResponseEntity.ok(new AuthResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    token
            ));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ErrorResponse("INVALID_PASSWORD", "Incorrect password.")
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("SERVER_ERROR", "An unexpected error occurred during login.")
            );
        }
    }

@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
    try {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse("MISSING_EMAIL", "Email is required.")
            );
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse("USER_NOT_FOUND", "No account found with this email.")
            );
        }

        User user = userOpt.get();

        // 🔍 Check if a valid token already exists for this user
        Optional<PasswordResetToken> existingTokenOpt = tokenRepository.findByUserId(user.getId());
        if (existingTokenOpt.isPresent()) {
            PasswordResetToken existingToken = existingTokenOpt.get();
            if (!existingToken.isExpired()) {
                // 🧠 Token still valid → prevent new requests
                long minutesLeft = java.time.Duration
                        .between(LocalDateTime.now(), existingToken.getExpiresAt())
                        .toMinutes();

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    new ErrorResponse(
                        "RESET_ALREADY_REQUESTED",
                        String.format("You already requested a password reset. Please check your inbox or try again in %d minutes.", minutesLeft)
                    )
                );
            } else {
                // 🧹 Clean up old expired token
                tokenRepository.delete(existingToken);
            }
        }

        // 🧩 Generate new token valid for 10 minutes
        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(resetToken, user, LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        // ✉️ Send email
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        System.out.println("📧 Reset email sent to: " + user.getEmail());
        System.out.println("🔗 Reset link: " + resetLink);

        return ResponseEntity.ok(Map.of(
            "message", "Password reset link sent successfully.",
            "resetLink", resetLink
        ));

    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse("SERVER_ERROR", "Unable to process password reset request.")
        );
    }
}

    // 🔹 RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("password");

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ErrorResponse("MISSING_TOKEN", "Reset token is required.")
                );
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ErrorResponse("MISSING_PASSWORD", "New password is required.")
                );
            }

            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ErrorResponse("INVALID_TOKEN", "Invalid or expired reset link.")
                );
            }

            PasswordResetToken resetToken = tokenOpt.get();
            if (resetToken.isExpired()) {
                tokenRepository.delete(resetToken);
                return ResponseEntity.status(HttpStatus.GONE).body(
                        new ErrorResponse("TOKEN_EXPIRED", "This reset link has expired.")
                );
            }

            User user = resetToken.getUser();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // 🧹 Delete used token
            tokenRepository.delete(resetToken);

            System.out.println("✅ Password updated for user: " + user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Password has been successfully updated."
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("SERVER_ERROR", "Unable to reset password at this time.")
            );
        }
    }
}
