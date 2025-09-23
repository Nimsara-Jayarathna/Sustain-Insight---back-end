package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.payload.AuthResponse;
import com.news_aggregator.backend.payload.LoginRequest;
import com.news_aggregator.backend.payload.SignupRequest;
import com.news_aggregator.backend.repository.UserRepository;
import com.news_aggregator.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        //user.setJobTitle(request.getJobTitle());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        System.out.println("DEBUG → Signup success for: " + user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Object principal = authentication.getPrincipal();
        User user;
        if (principal instanceof User) {
            user = (User) principal;
        } else {
            throw new RuntimeException("Unexpected principal type: " + principal.getClass());
        }

        String token = jwtService.generateToken(user);

        System.out.println("DEBUG → Login success for: " + user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user));
    }
}
