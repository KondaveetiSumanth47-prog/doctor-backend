package com.doctorappointment.controller;

import com.doctorappointment.entity.FavoriteDoctor;
import com.doctorappointment.service.FavoriteDoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteDoctorController {

    @Autowired
    private FavoriteDoctorService favoriteDoctorService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<FavoriteDoctor>> getPatientFavorites(@PathVariable Long patientId) {
        return ResponseEntity.ok(favoriteDoctorService.getPatientFavorites(patientId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@RequestParam Long patientId, @RequestParam Long doctorId) {
        boolean isFav = favoriteDoctorService.isFavorite(patientId, doctorId);
        if (isFav) {
            favoriteDoctorService.removeFavorite(patientId, doctorId);
            return ResponseEntity.ok(Map.of("favorited", false, "message", "Removed from favorites"));
        } else {
            favoriteDoctorService.addFavorite(patientId, doctorId);
            return ResponseEntity.ok(Map.of("favorited", true, "message", "Added to favorites"));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> isFavorite(@RequestParam Long patientId, @RequestParam Long doctorId) {
        boolean isFav = favoriteDoctorService.isFavorite(patientId, doctorId);
        return ResponseEntity.ok(Map.of("isFavorite", isFav));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(@RequestParam Long patientId, @RequestParam Long doctorId) {
        favoriteDoctorService.removeFavorite(patientId, doctorId);
        return ResponseEntity.noContent().build();
    }
}
