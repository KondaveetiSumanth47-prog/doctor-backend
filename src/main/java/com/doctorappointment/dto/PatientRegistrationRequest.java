package com.doctorappointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientRegistrationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits") String phone,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Size(max = 30) String gender,
        @NotBlank @Size(min = 4, max = 72) String password) {
}
