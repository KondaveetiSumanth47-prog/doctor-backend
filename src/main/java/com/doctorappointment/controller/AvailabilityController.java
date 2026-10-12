package com.doctorappointment.controller;

import com.doctorappointment.entity.Availability;
import com.doctorappointment.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<Availability>> getAllAvailabilities() {
        return ResponseEntity.ok(
                availabilityService.getAllAvailabilities()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Availability> getAvailabilityById(
            @PathVariable Long id,
            Authentication authentication) {

        return availabilityService.getAvailabilityById(id, authentication)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Availability>> getDoctorAvailabilities(
            @PathVariable Long doctorId) {

        return ResponseEntity.ok(
                availabilityService.getDoctorAvailabilities(doctorId)
        );
    }

    @PostMapping
    public ResponseEntity<Availability> createAvailability(
            @Valid @RequestBody Availability availability,
            Authentication authentication) {

        return ResponseEntity.ok(
                availabilityService.saveAvailability(availability, authentication)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Availability> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody Availability availability,
            Authentication authentication) {

        return ResponseEntity.ok(
                availabilityService.updateAvailability(id, availability, authentication)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Long id,
            Authentication authentication) {

        availabilityService.deleteAvailability(id, authentication);
        return ResponseEntity.noContent().build();
    }
}