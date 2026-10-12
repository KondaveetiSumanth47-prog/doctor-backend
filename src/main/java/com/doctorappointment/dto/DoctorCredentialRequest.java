package com.doctorappointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorCredentialRequest(
        @NotBlank @Size(min = 4, max = 72) String password) {
}
