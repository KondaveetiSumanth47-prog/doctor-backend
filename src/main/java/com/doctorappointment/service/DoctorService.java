package com.doctorappointment.service;

import com.doctorappointment.dto.DoctorCredentialRequest;
import com.doctorappointment.dto.DoctorProfileRequest;
import com.doctorappointment.dto.DoctorRegistrationRequest;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Role;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    public Optional<Doctor> getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(normalizeEmail(email));
    }

    @Transactional
    public Doctor createDoctor(DoctorRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (doctorRepository.existsByEmail(email)
                || userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account already exists for this email");
        }

        userRepository.save(new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.DOCTOR));
        Doctor doctor = new Doctor(
                request.name().trim(),
                email,
                request.phone().trim(),
                request.specialization().trim(),
                request.qualification().trim(),
                request.experience());
        if (request.consultationFee() != null && request.consultationFee() > 0) {
            doctor.setConsultationFee(request.consultationFee());
        }
        return doctorRepository.save(doctor);
    }

    @Transactional
    public void setDoctorPassword(
            Long doctorId,
            DoctorCredentialRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
        String email = normalizeEmail(doctor.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> new User(
                        doctor.getName(), email, "", Role.DOCTOR));
        if (user.getRole() != Role.DOCTOR) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The doctor's email is assigned to another account role");
        }
        user.setName(doctor.getName());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Transactional
    public Doctor updateDoctor(Long id, DoctorProfileRequest request) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
        String email = normalizeEmail(request.email());
        if (!existingDoctor.getEmail().equalsIgnoreCase(email)
                && (doctorRepository.existsByEmail(email)
                        || userRepository.existsByEmail(email))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account already exists for this email");
        }

        String oldEmail = existingDoctor.getEmail();
        existingDoctor.setName(request.name().trim());
        existingDoctor.setEmail(email);
        existingDoctor.setPhone(request.phone().trim());
        existingDoctor.setSpecialization(request.specialization().trim());
        existingDoctor.setQualification(request.qualification().trim());
        existingDoctor.setExperience(request.experience());
        if (request.consultationFee() != null && request.consultationFee() >= 0) {
            existingDoctor.setConsultationFee(request.consultationFee());
        }

        userRepository.findByEmail(oldEmail).ifPresent(user -> {
            user.setEmail(email);
            user.setName(request.name().trim());
            userRepository.save(user);
        });
        return doctorRepository.save(existingDoctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
        doctorRepository.delete(doctor);
        userRepository.findByEmail(doctor.getEmail())
                .filter(user -> user.getRole() == Role.DOCTOR)
                .ifPresent(userRepository::delete);
    }

    public boolean isOwnedBy(Long id, String email) {
        return doctorRepository.findById(id)
                .map(doctor -> doctor.getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    @Transactional
    public Doctor updateVerificationStatus(Long id, String status) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
        if (status == null || (!status.equalsIgnoreCase("APPROVED") && !status.equalsIgnoreCase("REJECTED") && !status.equalsIgnoreCase("PENDING"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification status");
        }
        doctor.setVerificationStatus(status.toUpperCase(Locale.ROOT));
        return doctorRepository.save(doctor);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
