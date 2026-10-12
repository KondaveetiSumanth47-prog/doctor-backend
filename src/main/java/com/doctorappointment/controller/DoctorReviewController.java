package com.doctorappointment.controller;

import com.doctorappointment.dto.DoctorReviewRequest;
import com.doctorappointment.entity.DoctorReview;
import com.doctorappointment.service.DoctorReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class DoctorReviewController {

    @Autowired
    private DoctorReviewService doctorReviewService;

    @PostMapping
    public ResponseEntity<DoctorReview> createReview(
            @Valid @RequestBody DoctorReviewRequest request,
            Authentication authentication) {
        DoctorReview created = doctorReviewService.createReview(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<DoctorReview>> getDoctorReviews(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorReviewService.getDoctorReviews(doctorId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DoctorReview>> getPatientReviews(@PathVariable Long patientId) {
        return ResponseEntity.ok(doctorReviewService.getPatientReviews(patientId));
    }
}
