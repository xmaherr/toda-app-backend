package com.todoapp.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Email(message = "Invalid email format")
    private String newEmail;


    @NotBlank(message = "Current password cannot be blank")
    private String currentPassword;


    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}