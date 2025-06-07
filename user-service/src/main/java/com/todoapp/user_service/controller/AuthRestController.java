package com.todoapp.user_service.controller;

import com.todoapp.user_service.dto.UserLoginRequest;
import com.todoapp.user_service.dto.JwtAuthResponse;
import com.todoapp.user_service.dto.UserResponse;
import com.todoapp.user_service.entity.User;
import com.todoapp.user_service.security.jwt.JwtTokenProvider;
import com.todoapp.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthRestController(UserService userService,
                              AuthenticationManager authenticationManager,
                              JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody UserLoginRequest loginDto) {
           Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        userService.loginUser(loginDto.getEmail());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        return new ResponseEntity<>(new JwtAuthResponse(token), HttpStatus.OK);
    }

    @PostMapping("/validateToken")
    public ResponseEntity<UserResponse> validateTokenAndGetProfile(
            @RequestHeader("Authorization") String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided and start with Bearer.");
        }

        String token = authorizationHeader.substring(7);

        User user = userService.validateTokenAndGetUser(token);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled()
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
