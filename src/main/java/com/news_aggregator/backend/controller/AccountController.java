package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.CategoryDto;
import com.news_aggregator.backend.dto.SourceDto;
import com.news_aggregator.backend.dto.UserDto;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.payload.PreferenceUpdateRequest;
import com.news_aggregator.backend.repository.CategoryRepository;
import com.news_aggregator.backend.repository.SourceRepository;
import com.news_aggregator.backend.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;

    public AccountController(UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             SourceRepository sourceRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.sourceRepository = sourceRepository;
    }

    // ✅ Get logged-in user
    @GetMapping("/me")
    public UserDto getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("No authenticated user");

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDto(user);
    }

    // ✅ Update preferences + name
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

        // Update preferences
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

        userRepository.save(user); // persists changes

        return mapToDto(user);
    }

    // Helper to map entity -> DTO
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
