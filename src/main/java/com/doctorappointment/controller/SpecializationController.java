package com.doctorappointment.controller;

import com.doctorappointment.entity.Specialization;
import com.doctorappointment.service.SpecializationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationController {

    private final SpecializationService specializationService;

    public SpecializationController(
            SpecializationService specializationService) {
        this.specializationService = specializationService;
    }

    @GetMapping
    public ResponseEntity<List<Specialization>> getAllSpecializations() {
        return ResponseEntity.ok(
                specializationService.getAllSpecializations()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Specialization> getSpecializationById(
            @PathVariable Long id) {

        return specializationService.getSpecializationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Specialization> getSpecializationByName(
            @PathVariable String name) {

        return specializationService.getSpecializationByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Specialization> createSpecialization(
            @RequestBody Specialization specialization) {

        return ResponseEntity.ok(
                specializationService.saveSpecialization(specialization)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Specialization> updateSpecialization(
            @PathVariable Long id,
            @RequestBody Specialization specialization) {

        return ResponseEntity.ok(
                specializationService.updateSpecialization(
                        id, specialization)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialization(
            @PathVariable Long id) {

        specializationService.deleteSpecialization(id);
        return ResponseEntity.noContent().build();
    }
}