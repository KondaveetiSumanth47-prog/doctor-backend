package com.doctorappointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCreateRequest(
        @NotNull Long doctorId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime appointmentTime,
        @Size(max = 500) String reason) {
}
