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

    public AccountController(UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             SourceRepository sourceRepository,
                             PasswordEncoder passwordEncoder) { 
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.sourceRepository = sourceRepository;
        this.passwordEncoder = passwordEncoder; 
    }

    @GetMapping("/me")
    public UserDto getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("No authenticated user");

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDto(user);
    }

    @PutMapping("/preferences")
    @Transactional
    public UserDto updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PreferenceUpdateRequest request
    ) {
        if (userDetails == null) throw new RuntimeException("No authenticated user");

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update name if provided
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        if (request.getCategoryIds() != null) {
            user.setPreferredCategories(
                new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()))
            );
        }
        if (request.getSourceIds() != null) {
            user.setPreferredSources(
                new HashSet<>(sourceRepository.findAllById(request.getSourceIds()))
            );
        }

        userRepository.save(user); 

        return mapToDto(user);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyCurrentPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VerifyPasswordRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found during password verification."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ErrorResponse("INVALID_PASSWORD", "Incorrect password.")
            );
        }

        return ResponseEntity.ok(Map.of("verified", true, "message", "Password verified successfully."));
    }

    @PutMapping("/api/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "User not authenticated."));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found during password change."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ErrorResponse("INVALID_PASSWORD", "Incorrect password.")
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPreferredCategories().stream()
                        .map(c -> new CategoryDto(c.getId(), c.getName()))
                        .collect(Collectors.toList()),
                user.getPreferredSources().stream()
                        .map(s -> new SourceDto(s.getId(), s.getName()))
                        .collect(Collectors.toList())
        );
    }
}
