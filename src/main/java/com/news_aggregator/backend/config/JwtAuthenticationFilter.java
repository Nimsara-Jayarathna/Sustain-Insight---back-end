package com.news_aggregator.backend.config;

import com.news_aggregator.backend.exception.SessionExpiredException;
import com.news_aggregator.backend.model.UserSession;
import com.news_aggregator.backend.service.JwtService;
import com.news_aggregator.backend.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   UserSessionService userSessionService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userSessionService = userSessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(jwt);
            UUID sessionId = jwtService.extractSessionId(jwt);

            if (sessionId == null) {
                throw new SessionExpiredException("Session expired, please login again");
            }

            Optional<UserSession> sessionOpt = userSessionService.findById(sessionId);
            UserSession session = sessionOpt.orElseThrow(() ->
                    new SessionExpiredException("Session expired, please login again"));

            if (!session.isActive()) {
                throw new SessionExpiredException("Session expired, please login again");
            }

            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
                userSessionService.deactivateSession(sessionId);
                throw new SessionExpiredException("Session expired, please login again");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(jwt, userDetails)) {
                    userSessionService.updateLastActive(sessionId);
                    request.setAttribute("sessionId", sessionId);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(sessionId);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (SessionExpiredException e) {
            writeSessionExpiredResponse(response, e.getMessage());
        }
    }

    private void writeSessionExpiredResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
