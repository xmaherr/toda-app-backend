package com.todoapp.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todoapp.user_service.dto.*;
import com.todoapp.user_service.entity.User;
import com.todoapp.user_service.exception.ResourceNotFoundException;
import com.todoapp.user_service.security.config.JwtSecurityConfig;
import com.todoapp.user_service.security.jwt.JwtTokenProvider;
import com.todoapp.user_service.security.userdetail.CustomUserDetailsService;
import com.todoapp.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRestController.class)
@Import({JwtTokenProvider.class, JwtSecurityConfig.class})
public class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserResponse testUserResponse;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserService userService() {
            return mock(UserService.class); // نرجع نسخة mock
        }
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);

        testUserResponse = new UserResponse(testUser.getId(), testUser.getEmail(), testUser.isEnabled());
        reset(userService);
    }

    @Test
    void registerUser_Success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "password123");

        User userToRegister = new User();
        userToRegister.setEmail(request.getEmail());
        userToRegister.setPassword(request.getPassword());

        User registeredUserFromService = new User();
        registeredUserFromService.setId(1L);
        registeredUserFromService.setEmail(request.getEmail());
        registeredUserFromService.setPassword("encodedPassword");
        registeredUserFromService.setEnabled(false);

        when(userService.registerUser(any(User.class))).thenReturn(registeredUserFromService);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(registeredUserFromService.getId()))
                .andExpect(jsonPath("$.email").value(registeredUserFromService.getEmail()))
                .andExpect(jsonPath("$.enabled").value(registeredUserFromService.isEnabled()));

        verify(userService, times(1)).registerUser(any(User.class));
    }

    @Test
    void registerUser_InvalidInput_ReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("invalid-email", "short");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerUser_UserAlreadyExists_ReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "password123");

        when(userService.registerUser(any(User.class))).thenThrow(new IllegalArgumentException("User with this email already exists."));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with this email already exists."));
        verify(userService, times(1)).registerUser(any(User.class));
    }

    @Test
    void activateUser_Success() throws Exception {
        UserActivationRequest request = new UserActivationRequest("test@example.com", "123456");
        when(userService.activateUser(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/users/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account activated successfully!"));
        verify(userService, times(1)).activateUser(anyString(), anyString());
    }

    @Test
    void activateUser_Failure() throws Exception {
        UserActivationRequest request = new UserActivationRequest("test@example.com", "123456");
        when(userService.activateUser(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/users/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Activation failed. Invalid OTP or email, or account already active."));
        verify(userService, times(1)).activateUser(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getUserProfile_Success() throws Exception {
        when(userService.loginUser(testUser.getEmail())).thenReturn(testUser);

        mockMvc.perform(get("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.enabled").value(testUser.isEnabled()));

        verify(userService, times(1)).loginUser(testUser.getEmail());
    }

    @Test
    void getUserProfile_NoAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regenerateOtp_Success() throws Exception {
        doNothing().when(userService).regenerateOtp(anyString());

        mockMvc.perform(post("/api/users/regenerateOtp")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("New OTP has been sent to your email."));
        verify(userService, times(1)).regenerateOtp("test@example.com");
    }

    @Test
    void regenerateOtp_UserNotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found")).when(userService).regenerateOtp(anyString());

        mockMvc.perform(post("/api/users/regenerateOtp")
                        .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        verify(userService, times(1)).regenerateOtp("nonexistent@example.com");
    }

    @Test
    void forgetPassword_Success() throws Exception {
        ForgetPasswordRequest request = new ForgetPasswordRequest("test@example.com");
        doNothing().when(userService).forgotPassword(anyString());

        mockMvc.perform(post("/api/users/forgetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset OTP has been sent to your email."));
        verify(userService, times(1)).forgotPassword("test@example.com");
    }

    @Test
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("test@example.com", "123456", "newPass123", "newPass123");
        doNothing().when(userService).changePassword(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/users/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password has been changed successfully."));
        verify(userService, times(1)).changePassword(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateUser_Success() throws Exception {

        UserUpdateRequest request = new UserUpdateRequest();
        request.setCurrentPassword("currentPassword123");
        request.setNewEmail("newemail@example.com");
        request.setNewPassword("newPassword123");
        User updatedUser = new User();
        updatedUser.setId(testUser.getId());
        updatedUser.setEmail(testUser.getEmail());
        updatedUser.setPassword("newPassword123");
        updatedUser.setEnabled(testUser.isEnabled());

        when(userService.updateUser(eq(testUser.getEmail()), any(UserUpdateRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(updatedUser.getEmail()));

        verify(userService, times(1)).updateUser(eq(testUser.getEmail()), any(UserUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteUser_Success() throws Exception {
        UserLoginRequest request = new UserLoginRequest("test@example.com", "password123");
        doNothing().when(userService).deleteUser(anyString(), anyString());

        mockMvc.perform(delete("/api/users/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User account deleted successfully."));
        verify(userService, times(1)).deleteUser("test@example.com", "password123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteUser_WrongPassword_ReturnsBadRequest() throws Exception {
        UserLoginRequest request = new UserLoginRequest("test@example.com", "wrongpassword");
        doThrow(new IllegalArgumentException("Invalid email or password.")).when(userService).deleteUser(anyString(), anyString());

        mockMvc.perform(delete("/api/users/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        verify(userService, times(1)).deleteUser("test@example.com", "wrongpassword");
    }
}