package com.doctorappointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DoctorProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits") String phone,
        @NotBlank @Size(max = 120) String specialization,
        @NotBlank @Size(max = 120) String qualification,
        @NotNull @PositiveOrZero Integer experience,
        Double consultationFee) {

    public DoctorProfileRequest(
            String name, String email, String phone,
            String specialization, String qualification,
            Integer experience) {
        this(name, email, phone, specialization, qualification, experience, 500.0);
    }
}
