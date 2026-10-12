package com.doctorappointment.controller;

import com.doctorappointment.dto.PrescriptionCreateRequest;
import com.doctorappointment.entity.Prescription;
import com.doctorappointment.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @PostMapping
    public ResponseEntity<Prescription> createPrescription(@Valid @RequestBody PrescriptionCreateRequest request) {
        Prescription created = prescriptionService.createPrescription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Prescription>> getPatientPrescriptions(@PathVariable Long patientId) {
        return ResponseEntity.ok(prescriptionService.getPatientPrescriptions(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Prescription>> getDoctorPrescriptions(@PathVariable Long doctorId) {
        return ResponseEntity.ok(prescriptionService.getDoctorPrescriptions(doctorId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<Prescription> getAppointmentPrescription(@PathVariable Long appointmentId) {
        return prescriptionService.getAppointmentPrescription(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }
}
