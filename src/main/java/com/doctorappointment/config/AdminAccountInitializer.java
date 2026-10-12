package com.doctorappointment.config;

import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Role;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.UserRepository;
import com.doctorappointment.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminAccountInitializer(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!email.isBlank() && password.length() >= 12) {
            String normalizedEmail = AuthService.normalizeEmail(email);
            userRepository.findByEmail(normalizedEmail).ifPresentOrElse(
                    existing -> {
                        if (existing.getRole() != Role.ADMIN) {
                            throw new IllegalStateException(
                                    "Configured administrator email belongs to a non-admin account");
                        }
                    },
                    () -> userRepository.save(new User(
                            "Administrator",
                            normalizedEmail,
                            passwordEncoder.encode(password),
                            Role.ADMIN)));
        }

        if (doctorRepository.count() == 0) {
            String doc1Email = "dr.kapoor56912@gmail.com";
            if (!userRepository.existsByEmail(doc1Email)) {
                userRepository.save(new User("Dr. Anita Kapoor", doc1Email, passwordEncoder.encode("DocPassword123!"), Role.DOCTOR));
            }
            Doctor doc1 = new Doctor(
                    "Dr. Anita Kapoor", doc1Email, "9876528792", "Cardiologist", "MBBS, MD Cardiology", 12, 950.0);
            doc1.setVerificationStatus("APPROVED");
            doc1.setRating(5.0);
            doc1.setReviewCount(1);
            doctorRepository.save(doc1);

            String doc2Email = "satya358@gmail.com";
            if (!userRepository.existsByEmail(doc2Email)) {
                userRepository.save(new User("Dr. Satya", doc2Email, passwordEncoder.encode("satya@358"), Role.DOCTOR));
            }
            Doctor doc2 = new Doctor(
                    "Dr. Satya", doc2Email, "9347147369", "Orthopedics", "DM (Doctorate of Medicine)", 11, 3000.0);
            doc2.setVerificationStatus("APPROVED");
            doc2.setRating(5.0);
            doc2.setReviewCount(0);
            doctorRepository.save(doc2);
        }
    }
}
