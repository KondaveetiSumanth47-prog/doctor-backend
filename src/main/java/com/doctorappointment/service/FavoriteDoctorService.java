package com.doctorappointment.service;

import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.FavoriteDoctor;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.FavoriteDoctorRepository;
import com.doctorappointment.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteDoctorService {

    @Autowired
    private FavoriteDoctorRepository favoriteDoctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Transactional
    public FavoriteDoctor addFavorite(Long patientId, Long doctorId) {
        if (favoriteDoctorRepository.existsByPatientIdAndDoctorId(patientId, doctorId)) {
            return favoriteDoctorRepository.findByPatientIdAndDoctorId(patientId, doctorId).orElse(null);
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        FavoriteDoctor favorite = new FavoriteDoctor(patient, doctor);
        return favoriteDoctorRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long patientId, Long doctorId) {
        favoriteDoctorRepository.deleteByPatientIdAndDoctorId(patientId, doctorId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteDoctor> getPatientFavorites(Long patientId) {
        return favoriteDoctorRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long patientId, Long doctorId) {
        return favoriteDoctorRepository.existsByPatientIdAndDoctorId(patientId, doctorId);
    }
}
