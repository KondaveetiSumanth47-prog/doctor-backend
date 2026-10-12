package com.doctorappointment.service;

import com.doctorappointment.dto.DoctorCredentialRequest;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.Role;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(
            PatientRepository patientRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Optional<Patient> getPatientByEmail(String email) {
        return patientRepository.findByEmail(email.trim().toLowerCase());
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient updatePatient(Long id, Patient updatedPatient) {
        return patientRepository.findById(id)
                .map(existingPatient -> {
                    String newEmail = updatedPatient.getEmail().trim().toLowerCase();
                    if (!existingPatient.getEmail().equalsIgnoreCase(newEmail)
                            && (patientRepository.existsByEmail(newEmail)
                                    || userRepository.existsByEmail(newEmail))) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "An account already exists for this email");
                    }

                    String oldEmail = existingPatient.getEmail();
                    existingPatient.setName(updatedPatient.getName());
                    existingPatient.setEmail(newEmail);
                    existingPatient.setPhone(updatedPatient.getPhone());
                    existingPatient.setDateOfBirth(updatedPatient.getDateOfBirth());
                    existingPatient.setGender(updatedPatient.getGender());

                    userRepository.findByEmail(oldEmail).ifPresent(user -> {
                        user.setEmail(newEmail);
                        user.setName(updatedPatient.getName());
                        userRepository.save(user);
                    });
                    return patientRepository.save(existingPatient);
                })
                .orElseThrow(() ->
                        new RuntimeException("Patient not found with id: " + id));
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found"));
        patientRepository.delete(patient);
        userRepository.findByEmail(patient.getEmail())
                .filter(user -> user.getRole() == Role.PATIENT)
                .ifPresent(userRepository::delete);
    }

    public boolean isOwnedBy(Long id, String email) {
        return patientRepository.findById(id)
                .map(patient -> patient.getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    @Transactional
    public void setPatientPassword(
            Long patientId,
            DoctorCredentialRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found"));
        String email = patient.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> new User(
                        patient.getName(), email, "", Role.PATIENT));
        if (user.getRole() != Role.PATIENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The patient's email is assigned to another account role");
        }
        user.setName(patient.getName());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }
}