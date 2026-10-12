package com.doctorappointment;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.doctorappointment.entity.Availability;
import com.doctorappointment.entity.AppointmentStatus;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.Role;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.AvailabilityRepository;
import com.doctorappointment.repository.AppointmentRepository;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAndAppointmentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void corsAllowsConfiguredFrontendOriginAndRejectsOtherOrigins()
            throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "content-type,x-xsrf-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointIssuesSessionTokenAcceptedByUnsafeRequests()
            throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        Matcher tokenMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(csrfResult.getResponse().getContentAsString());
        assertTrue(tokenMatcher.find());
        String token = tokenMatcher.group(1);
        MockHttpSession session =
                (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertNotNull(session);

        String email = uniqueEmail();
        String body = """
                {
                  "name": "CSRF Test Patient",
                  "email": "%s",
                  "phone": "9876543210",
                  "dateOfBirth": "1990-01-01",
                  "gender": "Other",
                  "password": "PatientTest#123"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/patients")
                        .session(session)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void registrationHashesPasswordAndSessionLoginChecksCredentials()
            throws Exception {
        String email = uniqueEmail();
        String password = "PatientTest#123";
        String requestBody = """
                {
                  "name": "Test Patient",
                  "email": "%s",
                  "phone": "9876543211",
                  "dateOfBirth": "1990-01-01",
                  "gender": "Other",
                  "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.password").doesNotExist());

        User user = userRepository.findByEmail(email).orElseThrow();
        assertTrue(passwordEncoder.matches(password, user.getPassword()));
        assertTrue(!user.getPassword().equals(password));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login(email, password);
        assertNotNull(session);
        mockMvc.perform(get("/api/patients/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)));
        mockMvc.perform(get("/api/patients").session(session))
                .andExpect(status().isForbidden());

        // Test Admin Registration
        String adminEmail = uniqueEmail();
        String adminPassword = "AdminPassword123!";
        mockMvc.perform(post("/api/auth/register-admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Admin User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(adminEmail, adminPassword)))
                .andExpect(status().isCreated());

        MockHttpSession adminSession = login(adminEmail, adminPassword);
        mockMvc.perform(get("/api/auth/me").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(adminEmail)))
                .andExpect(jsonPath("$.role", is("ADMIN")));

        // Test Forgot / Reset Password flow with 6-digit verification code
        MvcResult codeResult = mockMvc.perform(post("/api/auth/send-reset-code")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationCode").isNotEmpty())
                .andReturn();

        Matcher codeMatcher = Pattern.compile("\"verificationCode\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(codeResult.getResponse().getContentAsString());
        assertTrue(codeMatcher.find());
        String code = codeMatcher.group(1);

        String newPassword = "NewSecretPassword#456";
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","verificationCode":"%s","newPassword":"%s"}
                                """.formatted(email, code, newPassword)))
                .andExpect(status().isNoContent());

        // Verify old password fails and new password succeeds
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isUnauthorized());

        MockHttpSession newSession = login(email, newPassword);
        assertNotNull(newSession);
    }

    @Test
    void bookingsRejectUnavailableAndConflictingSlotsAndEnforceOwnership()
            throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String patientEmail = "patient-" + suffix + "@gmail.com";
        String doctorEmail = "doctor-" + suffix + "@gmail.com";
        String otherDoctorEmail = "other-doctor-" + suffix + "@gmail.com";
        String patientPassword = "PatientTest#123";
        String doctorPassword = "DoctorTest#123";

        Patient patient = patientRepository.save(new Patient(
                "Test Patient", patientEmail, "5550100",
                LocalDate.of(1990, 1, 1), "Other"));
        Patient otherPatient = patientRepository.save(new Patient(
                "Other Patient", "other-" + suffix + "@gmail.com", "5550199",
                LocalDate.of(1992, 2, 2), "Other"));
        userRepository.save(new User(
                "Test Patient", patientEmail,
                passwordEncoder.encode(patientPassword), Role.PATIENT));
        Doctor doctor = doctorRepository.save(new Doctor(
                "Test Doctor", doctorEmail, "5550101",
                "Cardiology", "MD", 10));
        userRepository.save(new User(
                "Test Doctor", doctorEmail,
                passwordEncoder.encode(doctorPassword), Role.DOCTOR));
        Doctor otherDoctor = doctorRepository.save(new Doctor(
                "Other Doctor", otherDoctorEmail, "5550102",
                "Neurology", "MD", 8));
        userRepository.save(new User(
                "Other Doctor", otherDoctorEmail,
                passwordEncoder.encode("OtherDoctor#123"), Role.DOCTOR));

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        availabilityRepository.save(new Availability(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)));

        MockHttpSession patientSession = login(patientEmail, patientPassword);
        String unavailableRequest = appointmentJson(
                doctor.getId(), appointmentDate, "18:00");
        mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unavailableRequest))
                .andExpect(status().isBadRequest());

        String validRequest = appointmentJson(
                doctor.getId(), appointmentDate, "10:30");
        mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));
        long appointmentId = appointmentRepository
                .findByPatientId(patient.getId())
                .get(0)
                .getId();

        mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isConflict());

        String patientCancellationRequest = appointmentJson(
                doctor.getId(), appointmentDate, "11:30");
        mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientCancellationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));
        long patientCancellationAppointmentId = appointmentRepository
                .findByPatientId(patient.getId())
                .stream()
                .filter(appointment -> appointment.getAppointmentTime()
                        .equals(LocalTime.of(11, 30)))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/appointments/{id}/status",
                        patientCancellationAppointmentId)
                        .queryParam("status", "CANCELLED")
                        .session(patientSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        assertTrue(appointmentRepository.findById(
                        patientCancellationAppointmentId)
                .orElseThrow()
                .getStatus() == AppointmentStatus.CANCELLED);

        mockMvc.perform(get("/api/appointments/patient/{id}", otherPatient.getId())
                        .session(patientSession))
                .andExpect(status().isForbidden());

        MockHttpSession otherDoctorSession =
                login(otherDoctorEmail, "OtherDoctor#123");
        mockMvc.perform(put("/api/appointments/{id}/status", appointmentId)
                        .queryParam("status", "CANCELLED")
                        .session(otherDoctorSession)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        MockHttpSession doctorSession = login(doctorEmail, doctorPassword);
        mockMvc.perform(put("/api/appointments/{id}/status", appointmentId)
                        .queryParam("status", "CONFIRMED")
                        .session(doctorSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        mockMvc.perform(put("/api/appointments/{id}/status", appointmentId)
                        .queryParam("status", "CONFIRMED")
                        .session(patientSession)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/appointments/{id}/status", appointmentId)
                        .queryParam("status", "CANCELLED")
                        .session(patientSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void adminAnalyticsReviewsQueueAndPaymentWorkflow() throws Exception {
        String adminEmail = uniqueEmail();
        String adminPassword = "AdminPassword123!";
        mockMvc.perform(post("/api/auth/register-admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Analytics Admin",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(adminEmail, adminPassword)))
                .andExpect(status().isCreated());

        MockHttpSession adminSession = login(adminEmail, adminPassword);
        assertNotNull(adminSession);

        // Verify Admin Analytics Endpoint
        mockMvc.perform(get("/api/admin/analytics").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").exists())
                .andExpect(jsonPath("$.totalDoctors").exists())
                .andExpect(jsonPath("$.totalAppointments").exists());

        // Create Patient and Doctor
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String patientEmail = "p-" + suffix + "@gmail.com";
        String doctorEmail = "d-" + suffix + "@gmail.com";
        String patientPassword = "PatientPassword123!";
        String doctorPassword = "DoctorPassword123!";

        Patient patient = patientRepository.save(new Patient("Review Patient", patientEmail, "5551234", LocalDate.of(1995, 5, 5), "Female"));
        userRepository.save(new User("Review Patient", patientEmail, passwordEncoder.encode(patientPassword), Role.PATIENT));

        Doctor doctor = doctorRepository.save(new Doctor("Review Doctor", doctorEmail, "5555678", "Pediatrics", "MBBS, MD", 12, 600.0));
        userRepository.save(new User("Review Doctor", doctorEmail, passwordEncoder.encode(doctorPassword), Role.DOCTOR));

        // Test Doctor Verification Endpoint
        mockMvc.perform(put("/api/doctors/{id}/verification", doctor.getId())
                        .queryParam("status", "APPROVED")
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")));

        // Book Appointment
        MockHttpSession patientSession = login(patientEmail, patientPassword);
        LocalDate date = LocalDate.now().plusDays(2);
        String bookingJson = appointmentJson(doctor.getId(), date, "14:00");

        MvcResult bookingResult = mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingUrl").isNotEmpty())
                .andReturn();

        Matcher idMatcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(bookingResult.getResponse().getContentAsString());
        assertTrue(idMatcher.find());
        long apptId = Long.parseLong(idMatcher.group(1));

        // Test Process Payment
        mockMvc.perform(post("/api/appointments/{id}/pay", apptId)
                        .session(patientSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("PAID")))
                .andExpect(jsonPath("$.paymentTransactionId").isNotEmpty());

        // Test Doctor Daily Queue Endpoint
        MockHttpSession doctorSession = login(doctorEmail, doctorPassword);
        mockMvc.perform(get("/api/appointments/doctor/{doctorId}/queue", doctor.getId())
                        .session(doctorSession))
                .andExpect(status().isOk());

        // Doctor marks appointment COMPLETED
        mockMvc.perform(put("/api/appointments/{id}/status", apptId)
                        .queryParam("status", "CONFIRMED")
                        .session(doctorSession)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/appointments/{id}/status", apptId)
                        .queryParam("status", "COMPLETED")
                        .session(doctorSession)
                        .with(csrf()))
                .andExpect(status().isOk());

        // Patient submits Review for Completed Appointment
        String reviewJson = """
                {
                  "appointmentId": %d,
                  "rating": 5,
                  "reviewText": "Outstanding medical care and prompt consultation!"
                }
                """.formatted(apptId);

        mockMvc.perform(post("/api/reviews")
                        .session(patientSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating", is(5)));

        // Verify public doctor reviews GET endpoint
        mockMvc.perform(get("/api/reviews/doctor/{doctorId}", doctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating", is(5)));
    }

    @Test
    void patientRegistrationValidatesTenDigitPhoneNumber() throws Exception {
        String email = uniqueEmail();

        // 9-digit phone must be rejected with 400
        String body9 = """
                {
                  "name": "Invalid Phone Patient",
                  "email": "%s",
                  "phone": "987654321",
                  "dateOfBirth": "1990-01-01",
                  "gender": "Other",
                  "password": "PatientTest#123"
                }
                """.formatted(email);
        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body9))
                .andExpect(status().isBadRequest());

        // 11-digit phone must be rejected with 400
        String body11 = """
                {
                  "name": "Invalid Phone Patient",
                  "email": "%s",
                  "phone": "98765432101",
                  "dateOfBirth": "1990-01-01",
                  "gender": "Other",
                  "password": "PatientTest#123"
                }
                """.formatted(email);
        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body11))
                .andExpect(status().isBadRequest());

        // Alpha characters must be rejected with 400
        String bodyAlpha = """
                {
                  "name": "Invalid Phone Patient",
                  "email": "%s",
                  "phone": "987654321A",
                  "dateOfBirth": "1990-01-01",
                  "gender": "Other",
                  "password": "PatientTest#123"
                }
                """.formatted(email);
        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAlpha))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String appointmentJson(
            Long doctorId,
            LocalDate date,
            String time) {
        return """
                {
                  "doctorId": %d,
                  "appointmentDate": "%s",
                  "appointmentTime": "%s",
                  "reason": "Integration test"
                }
                """.formatted(doctorId, date, time);
    }

    private String uniqueEmail() {
        return "patient-" + UUID.randomUUID().toString().replace("-", "")
                + "@gmail.com";
    }
}
