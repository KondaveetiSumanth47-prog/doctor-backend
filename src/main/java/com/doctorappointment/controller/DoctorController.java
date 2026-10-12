package com.doctorappointment.controller;

import com.doctorappointment.dto.DoctorProfileRequest;
import com.doctorappointment.dto.DoctorCredentialRequest;
import com.doctorappointment.dto.DoctorRegistrationRequest;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.service.DoctorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/me")
    public ResponseEntity<Doctor> getMyProfile(Authentication authentication) {
        return doctorService.getDoctorByEmail(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return doctorService.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Doctor> createDoctor(
            @Valid @RequestBody DoctorRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.createDoctor(request));
    }

    @PostMapping("/{id}/credentials")
    public ResponseEntity<Void> setDoctorPassword(
            @PathVariable Long id,
            @Valid @RequestBody DoctorCredentialRequest request) {
        doctorService.setDoctorPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorProfileRequest request,
            Authentication authentication) {
        requireOwnerOrAdmin(id, authentication);
        return ResponseEntity.ok(doctorService.updateDoctor(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/verification")
    public ResponseEntity<Doctor> updateVerificationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(doctorService.updateVerificationStatus(id, status));
    }

    private void requireOwnerOrAdmin(
            Long doctorId,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin
                && !doctorService.isOwnedBy(doctorId, authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot access this doctor profile");
        }
    }
}
