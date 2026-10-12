package com.doctorappointment.controller;

import com.doctorappointment.dto.PatientProfileRequest;
import com.doctorappointment.dto.PatientRegistrationRequest;
import com.doctorappointment.dto.DoctorCredentialRequest;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.service.AuthService;
import com.doctorappointment.service.PatientService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AuthService authService;

    public PatientController(
            PatientService patientService,
            AuthService authService) {
        this.patientService = patientService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/me")
    public ResponseEntity<Patient> getMyProfile(Authentication authentication) {
        return patientService.getPatientByEmail(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable Long id,
            Authentication authentication) {
        requireOwnerOrAdmin(id, authentication);
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerPatient(request));
    }

    @PostMapping("/{id}/credentials")
    public ResponseEntity<Void> setPatientPassword(
            @PathVariable Long id,
            @Valid @RequestBody DoctorCredentialRequest request) {
        patientService.setPatientPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientProfileRequest request,
            Authentication authentication) {
        requireOwnerOrAdmin(id, authentication);
        Patient updatedPatient = new Patient(
                request.name().trim(),
                request.email(),
                request.phone().trim(),
                patientService.getPatientById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Patient not found"))
                        .getDateOfBirth(),
                request.gender().trim());
        return ResponseEntity.ok(
                patientService.updatePatient(id, updatedPatient));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(
            Long patientId,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin
                && !patientService.isOwnedBy(patientId, authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot access this patient profile");
        }
    }
}
