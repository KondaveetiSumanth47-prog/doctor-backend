package com.doctorappointment.service;

import com.doctorappointment.dto.PatientRegistrationRequest;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.Role;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public AuthService(
            PatientRepository patientRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            EmailService emailService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Transactional
    public Patient registerPatient(PatientRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)
                || patientRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account already exists for this email");
        }

        User user = userRepository.save(new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.PATIENT));

        Patient patient = patientRepository.save(new Patient(
                request.name().trim(),
                email,
                request.phone().trim(),
                request.dateOfBirth(),
                request.gender().trim()));

        // Create welcome in-app notification
        notificationService.createNotification(
                user,
                "Welcome to Online Doctor Appointment System! Your account has been registered successfully.",
                "REGISTRATION");

        // Send registration confirmation email to the registered email address
        emailService.sendPatientRegistrationEmail(email, patient.getName());

        return patient;
    }

    @Transactional
    public User registerAdmin(com.doctorappointment.dto.AdminRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account already exists for this email");
        }

        return userRepository.save(new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.ADMIN));
    }

    @Transactional
    public com.doctorappointment.dto.ResetCodeResponse sendResetCode(String email) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No account found with this email address"));

        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        user.setResetCode(code);
        user.setResetCodeExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Send email with the verification code
        emailService.sendPasswordResetCodeEmail(normalized, user.getName(), code);

        // Create in-app notification
        notificationService.createNotification(
                user,
                "Password reset code requested. Your 6-digit verification code is: " + code + " (valid for 15 minutes).",
                "SECURITY");

        return new com.doctorappointment.dto.ResetCodeResponse(
                "Verification code sent to " + email,
                code);
    }

    @Transactional
    public void resetPasswordWithCode(String email, String verificationCode, String newPassword) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No account found with this email address"));

        if (user.getResetCode() == null
                || !user.getResetCode().equals(verificationCode == null ? "" : verificationCode.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid verification code");
        }

        if (user.getResetCodeExpiry() == null
                || java.time.LocalDateTime.now().isAfter(user.getResetCodeExpiry())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Verification code has expired. Please request a new code");
        }

        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must be at least 4 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepository.save(user);
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.endsWith("@gmail.com")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only @gmail.com email addresses are allowed");
        }
        return normalized;
    }
}
