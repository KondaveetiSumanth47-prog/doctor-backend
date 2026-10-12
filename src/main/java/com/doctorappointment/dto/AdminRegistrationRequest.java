package com.doctorappointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRegistrationRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 4, max = 72) String password
) {}
