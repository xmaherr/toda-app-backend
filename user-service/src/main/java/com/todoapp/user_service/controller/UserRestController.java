package com.todoapp.user_service.controller;

import com.todoapp.user_service.dto.*;
import com.todoapp.user_service.entity.User;
import com.todoapp.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;

    @Autowired
    public UserRestController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        User registeredUser = userService.registerUser(user);


        UserResponse response = new UserResponse(registeredUser.getId(), registeredUser.getEmail(), registeredUser.isEnabled());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping("/activate")
    public ResponseEntity<String> activateUser(@Valid @RequestBody UserActivationRequest request) {
        boolean activated = userService.activateUser(request.getEmail(), request.getOtpCode());
        if (activated) {
            return new ResponseEntity<>("Account activated successfully!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Activation failed. Invalid OTP or email, or account already active.", HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.loginUser(userDetails.getUsername());

        UserResponse response = new UserResponse(user.getId(), user.getEmail(), user.isEnabled());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/regenerateOtp")
    public ResponseEntity<String> regenerateOtp(@RequestParam String email) {
        userService.regenerateOtp(email);
        return new ResponseEntity<>("New OTP has been sent to your email.", HttpStatus.OK);
    }

    @PostMapping("/forgetPassword")
    public ResponseEntity<String> forgetPassword(@Valid @RequestBody ForgetPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return new ResponseEntity<>("Password reset OTP has been sent to your email.", HttpStatus.OK);
    }


    @PostMapping("/changePassword")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getEmail(), request.getOtpCode(), request.getNewPassword(), request.getConfirmPassword());
        return new ResponseEntity<>("Password has been changed successfully.", HttpStatus.OK);
    }
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(userDetails.getUsername(), request);
        UserResponse response = new UserResponse(updatedUser.getId(), updatedUser.getEmail(), updatedUser.isEnabled());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/delete") // <--- DELETE mapping for deletion
    public ResponseEntity<String> deleteUser(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UserLoginRequest request) {

        if (!userDetails.getUsername().equals(request.getEmail())) {
            throw new IllegalArgumentException("Cannot delete another user's account.");
        }

        userService.deleteUser(request.getEmail(), request.getPassword());
        return new ResponseEntity<>("User account deleted successfully.", HttpStatus.OK);
    }

}