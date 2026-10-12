package com.doctorappointment.controller;

import com.doctorappointment.dto.AuthResponse;
import com.doctorappointment.dto.CsrfResponse;
import com.doctorappointment.dto.LoginRequest;
import com.doctorappointment.dto.ResetCodeResponse;
import com.doctorappointment.dto.ResetPasswordRequest;
import com.doctorappointment.dto.SendResetCodeRequest;
import com.doctorappointment.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AuthService authService;
    private final com.doctorappointment.repository.UserRepository userRepository;
    private final com.doctorappointment.repository.DoctorRepository doctorRepository;
    private final com.doctorappointment.repository.PatientRepository patientRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            AuthService authService,
            com.doctorappointment.repository.UserRepository userRepository,
            com.doctorappointment.repository.DoctorRepository doctorRepository,
            com.doctorappointment.repository.PatientRepository patientRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.authService = authService;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(
            HttpServletRequest request,
            HttpServletResponse response) {
        var token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                            .unauthenticated(
                                    AuthService.normalizeEmail(request.email()),
                                    request.password()));
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        securityContextRepository.saveContext(
                context, servletRequest, servletResponse);
        return toAuthResponse(authentication);
    }

    @GetMapping("/me")
    public AuthResponse currentUser(Authentication authentication) {
        return toAuthResponse(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send-reset-code")
    public ResetCodeResponse sendResetCode(
            @Valid @RequestBody SendResetCodeRequest request) {
        return authService.sendResetCode(request.email());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithCode(
                request.email(),
                request.verificationCode(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register-admin")
    public ResponseEntity<java.util.Map<String, String>> registerAdmin(
            @Valid @RequestBody com.doctorappointment.dto.AdminRegistrationRequest request) {
        authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(java.util.Map.of("message", "Administrator account created successfully"));
    }

    private AuthResponse toAuthResponse(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user has no application role"));

        String email = AuthService.normalizeEmail(authentication.getName());
        com.doctorappointment.entity.User user = userRepository.findByEmail(email).orElse(null);
        Long userId = user != null ? user.getId() : null;
        String name = user != null ? user.getName() : email;

        com.doctorappointment.entity.Doctor doctor = null;
        com.doctorappointment.entity.Patient patient = null;

        if ("DOCTOR".equalsIgnoreCase(role)) {
            doctor = doctorRepository.findByEmail(email).orElse(null);
            if (doctor == null && user != null) {
                com.doctorappointment.entity.Doctor newDoc = new com.doctorappointment.entity.Doctor(
                        user.getName(), email, "", "General Medicine", "MBBS", 1);
                doctor = doctorRepository.save(newDoc);
            }
        } else if ("PATIENT".equalsIgnoreCase(role)) {
            patient = patientRepository.findByEmail(email).orElse(null);
            if (patient == null && user != null) {
                com.doctorappointment.entity.Patient newPat = new com.doctorappointment.entity.Patient(
                        user.getName(), email, "", null, "Other");
                patient = patientRepository.save(newPat);
            }
        }

        return new AuthResponse(userId, email, name, role, doctor, patient);
    }
}
