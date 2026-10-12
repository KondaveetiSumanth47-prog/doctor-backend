package com.doctorappointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DoctorReviewRequest(
        @NotNull(message = "Appointment ID is required") Long appointmentId,
        @NotNull(message = "Rating is required") @Min(1) @Max(5) Integer rating,
        String reviewText
) {}
