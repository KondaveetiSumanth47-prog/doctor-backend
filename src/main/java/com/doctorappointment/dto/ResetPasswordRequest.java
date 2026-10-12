package com.doctorappointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification code is required")
        String verificationCode,

        @NotBlank(message = "New password is required")
        @Size(min = 4, max = 72, message = "Password must be at least 4 characters long")
        String newPassword
) {}
