package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.CategoryDto;
import com.news_aggregator.backend.dto.SourceDto;
import com.news_aggregator.backend.dto.UserDto;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.payload.ChangePasswordRequest;
import com.news_aggregator.backend.payload.ErrorResponse;
import com.news_aggregator.backend.payload.PreferenceUpdateRequest;
import com.news_aggregator.backend.payload.VerifyPasswordRequest;
import com.news_aggregator.backend.repository.CategoryRepository;
import com.news_aggregator.backend.repository.SourceRepository;
import com.news_aggregator.backend.repository.UserRepository;
import com.news_aggregator.backend.service.EmailChangeService;
import com.news_aggregator.backend.service.EmailService;
import com.news_aggregator.backend.service.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired private EmailService emailService;
    @Autowired private EmailChangeService emailChangeService;

    public AccountController(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            SourceRepository sourceRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.sourceRepository = sourceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ------------------------------------------------------
    // ✅ Get Current User Info
    // ------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User account could not be located."));

        return ResponseEntity.ok(mapToDto(user));
    }

    // ------------------------------------------------------
    // ✅ Update Preferences
    // ------------------------------------------------------
    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<?> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PreferenceUpdateRequest request
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found."));

        try {
            if (request.getFirstName() != null && !request.getFirstName().isBlank())
                user.setFirstName(request.getFirstName());
            if (request.getLastName() != null && !request.getLastName().isBlank())
                user.setLastName(request.getLastName());
            if (request.getJobTitle() != null && !request.getJobTitle().isBlank())
                user.setJobTitle(request.getJobTitle());

            if (request.getCategoryIds() != null)
                user.setPreferredCategories(new HashSet<>(categoryRepository.findAllById(request.getCategoryIds())));
            if (request.getSourceIds() != null)
                user.setPreferredSources(new HashSet<>(sourceRepository.findAllById(request.getSourceIds())));

            userRepository.save(user);
            return ResponseEntity.ok(mapToDto(user));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UPDATE_FAILED", "Failed to update user preferences."));
        }
    }

    // ------------------------------------------------------
    // ✅ Verify Current Password
    // ------------------------------------------------------
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyCurrentPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VerifyPasswordRequest request
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found during password verification."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_PASSWORD", "Incorrect password."));

        return ResponseEntity.ok(Map.of("message", "Password verified successfully."));
    }

    // ------------------------------------------------------
    // ✅ Change Password
    // ------------------------------------------------------
    @PutMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found during password change."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_PASSWORD", "Incorrect current password."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getFirstName());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    // ------------------------------------------------------
    // ✅ Step 1: Request OTP to current email
    // ------------------------------------------------------
    @PostMapping("/email-change/request")
    public ResponseEntity<?> requestEmailChangeOtp(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found."));

        emailChangeService.sendOtpToCurrentEmail(user);
        return ResponseEntity.ok(Map.of("message", "OTP sent to current email."));
    }

    // ------------------------------------------------------
    // ✅ Step 2: Verify current email OTP
    // ------------------------------------------------------
    @PostMapping("/email-change/verify-current")
    public ResponseEntity<?> verifyCurrentEmailOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found."));

        boolean valid = emailChangeService.verifyCurrentEmailOtp(user, body.get("otp"));
        if (!valid)
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_OTP", "Invalid or expired OTP."));

        return ResponseEntity.ok(Map.of("message", "Current email verified."));
    }

    // ------------------------------------------------------
    // ✅ Step 3: Send OTP to new email
    // ------------------------------------------------------
    @PostMapping("/email-change/send-new-otp")
    public ResponseEntity<?> sendNewEmailOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found."));

        String newEmail = body.get("newEmail");
        if (newEmail == null || newEmail.isBlank())
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_EMAIL", "New email cannot be empty."));

        if (newEmail.equalsIgnoreCase(user.getEmail()))
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("SAME_EMAIL", "New email cannot be the same as the current email."));

        if (userRepository.existsByEmail(newEmail))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("EMAIL_EXISTS", "This email is already associated with another account."));

        emailChangeService.sendOtpToNewEmail(user, newEmail);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully to the new email."));
    }

    // ------------------------------------------------------
    // ✅ Step 4: Confirm new email & finalize change
    // ------------------------------------------------------
    @PostMapping("/email-change/confirm")
    public ResponseEntity<?> confirmNewEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found."));

        String newEmail = body.get("newEmail");
        String otp = body.get("otp");

        boolean success = emailChangeService.confirmEmailChange(user, newEmail, otp);
        if (!success)
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_OTP", "Invalid or expired OTP."));

        String newToken = jwtService.generateAccessToken(user);
        return ResponseEntity.ok(Map.of(
                "message", "Email updated successfully.",
                "token", newToken,
                "email", user.getEmail()
        ));
    }

    // ------------------------------------------------------
    // ✅ Mapper
    // ------------------------------------------------------
    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getJobTitle(),
                user.getPreferredCategories().stream()
                        .map(c -> new CategoryDto(c.getId(), c.getName()))
                        .collect(Collectors.toList()),
                user.getPreferredSources().stream()
                        .map(s -> new SourceDto(s.getId(), s.getName()))
                        .collect(Collectors.toList())
        );
    }
}
