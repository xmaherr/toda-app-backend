package com.todoapp.todo_service.security.jwt;

import com.todoapp.todo_service.dto.UserProfileResponse;
import com.todoapp.todo_service.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final CustomUserDetailsService customUserDetailsService;
    private final RestTemplate restTemplate;
    @Value("${user-service.url}")
    private String userServiceUrl;

    @Autowired
    public JwtAuthenticationFilter(CustomUserDetailsService customUserDetailsService, RestTemplate restTemplate) {
        this.customUserDetailsService = customUserDetailsService;
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);


        String validationUrl = userServiceUrl + "/api/auth/validateToken";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserProfileResponse> userProfileResponse = restTemplate.exchange(
                    validationUrl,
                    HttpMethod.POST,
                    entity,
                    UserProfileResponse.class
            );

            if (userProfileResponse.getStatusCode().is2xxSuccessful() && userProfileResponse.getBody() != null) {
                UserProfileResponse userProfile = userProfileResponse.getBody();
                String userEmail = userProfile.getEmail();

                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = new User(userEmail, "", new ArrayList<>());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                System.err.println("Token validation failed by User Service: " + userProfileResponse.getStatusCode());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

        } catch (Exception e) {

            System.err.println("Error calling User Service for token validation: " + e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
