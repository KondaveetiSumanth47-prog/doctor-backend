package com.doctorappointment.dto;

import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Patient;

public record AuthResponse(
        Long id,
        String email,
        String name,
        String role,
        Doctor doctor,
        Patient patient
) {
    public AuthResponse(String email, String role) {
        this(null, email, null, role, null, null);
    }
}
