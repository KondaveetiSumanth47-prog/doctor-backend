package com.doctorappointment.service;

import com.doctorappointment.dto.AppointmentCreateRequest;
import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.AppointmentStatus;
import com.doctorappointment.entity.Availability;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.AppointmentRepository;
import com.doctorappointment.repository.AvailabilityRepository;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityRepository availabilityRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    private static String formatTime12Hour(LocalTime time) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
    }

    private static String doctorDisplayName(Doctor doctor) {
        if (doctor == null || doctor.getName() == null) return "Doctor";
        String name = doctor.getName().trim();
        if (name.toLowerCase().startsWith("dr.") || name.toLowerCase().startsWith("dr ")) {
            return name;
        }
        return "Dr. " + name;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(
            Long id,
            Authentication authentication) {
        Optional<Appointment> found = appointmentRepository.findById(id);
        found.ifPresent(appointment -> {
            String role = role(authentication);
            if ("PATIENT".equals(role)) {
                requireOwnerOrAdmin(
                        appointment.getPatient().getEmail(),
                        authentication,
                        "PATIENT");
            } else if ("DOCTOR".equals(role)) {
                requireOwnerOrAdmin(
                        appointment.getDoctor().getEmail(),
                        authentication,
                        "DOCTOR");
            }
        });
        return found;
    }

    public List<Appointment> getAppointmentsByDoctor(
            Long doctorId,
            Authentication authentication) {
        requireOwnerOrAdmin(
                doctorRepository.findById(doctorId)
                        .map(Doctor::getEmail)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Doctor not found")),
                authentication,
                "DOCTOR");
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByPatient(
            Long patientId,
            Authentication authentication) {
        requireOwnerOrAdmin(
                patientRepository.findById(patientId)
                        .map(Patient::getEmail)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Patient not found")),
                authentication,
                "PATIENT");
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getDoctorAppointmentsByDate(
            Long doctorId,
            LocalDate date,
            Authentication authentication) {
        getAppointmentsByDoctor(doctorId, authentication);
        return appointmentRepository
                .findByDoctorIdAndAppointmentDate(doctorId, date);
    }

    public List<Appointment> getAppointmentsByStatus(
            AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    @Transactional
    public Appointment createAppointment(
            AppointmentCreateRequest request,
            Authentication authentication) {
        Patient patient = patientRepository.findByEmailForUpdate(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No patient profile is linked to this account"));
        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        LocalDate date = request.appointmentDate();
        LocalTime time = request.appointmentTime();
        if (date.isBefore(LocalDate.now())
                || (date.equals(LocalDate.now())
                        && !time.isAfter(LocalTime.now()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment must be scheduled for a future date and time");
        }

        if (appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        doctor.getId(), date, time, AppointmentStatus.CANCELLED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The selected time is already booked");
        }
        if (appointmentRepository
                .existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        patient.getId(), date, time, AppointmentStatus.CANCELLED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have an appointment at the selected time");
        }

        List<Availability> availability =
                availabilityRepository.findByDoctorIdAndDayOfWeek(
                        doctor.getId(), date.getDayOfWeek());
        if (!availability.isEmpty()
                && availability.stream().noneMatch(slot ->
                        !time.isBefore(slot.getStartTime())
                                && time.isBefore(slot.getEndTime()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The doctor is not available at the selected time");
        }

        if ("REJECTED".equalsIgnoreCase(doctor.getVerificationStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This doctor is not accepting new appointments");
        }

        Appointment app = new Appointment(
                patient,
                doctor,
                date,
                time,
                request.reason() == null ? "" : request.reason().trim(),
                AppointmentStatus.PENDING);
        app.setConsultationType("ONLINE");
        app.setMeetingUrl("https://meet.jit.si/doc-app-visit-" + UUID.randomUUID().toString().substring(0, 8));
        Appointment saved = appointmentRepository.save(app);

        // Send In-App Notifications
        String formattedTime = formatTime12Hour(time);
        String docDisplay = doctorDisplayName(doctor);
        userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                notificationService.createNotification(
                        u,
                        "Appointment scheduled with " + docDisplay + " on " + date + " at " + formattedTime,
                        "APPOINTMENT"));

        userRepository.findByEmail(doctor.getEmail()).ifPresent(u ->
                notificationService.createNotification(
                        u,
                        "New appointment booked by " + patient.getName() + " on " + date + " at " + formattedTime,
                        "APPOINTMENT"));

        // Send Email Notification to Patient's registered email
        emailService.sendAppointmentBookedEmail(
                patient.getEmail(),
                patient.getName(),
                docDisplay,
                doctor.getSpecialization(),
                date.toString(),
                formattedTime,
                saved.getConsultationType(),
                saved.getMeetingUrl());

        return saved;
    }

    @Transactional
    public Appointment updateMeetingUrl(Long id, String url, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        requireOwnerOrAdmin(appointment.getDoctor().getEmail(), authentication, "DOCTOR");
        appointment.setMeetingUrl(url);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment processPayment(Long id, String paymentMethod, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        requireOwnerOrAdmin(appointment.getPatient().getEmail(), authentication, "PATIENT");
        appointment.setPaymentStatus("PAID");

        String method = (paymentMethod == null || paymentMethod.isBlank()) ? "DEBIT_CARD" : paymentMethod.trim().toUpperCase();
        appointment.setPaymentMethod(method);

        String prefix = method.startsWith("UPI") ? "UPI-" : method.startsWith("CASH") ? "CASH-" : "CARD-";
        String txnId = prefix + System.currentTimeMillis() + "-" + (int)(Math.random() * 9000 + 1000);
        appointment.setPaymentTransactionId(txnId);
        Appointment saved = appointmentRepository.save(appointment);

        Patient patient = saved.getPatient();
        Doctor doctor = saved.getDoctor();
        double fee = doctor != null && doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 500.0;
        String docDisplay = doctorDisplayName(doctor);
        String methodDisplay = method.startsWith("UPI") ? "UPI" : method.startsWith("CASH") ? "Cash at Clinic" : "Debit/Credit Card";

        userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                notificationService.createNotification(
                        u,
                        "Payment of ₹" + fee + " (" + methodDisplay + ") for appointment with " + docDisplay + " processed successfully. Ref: " + txnId,
                        "PAYMENT"));

        emailService.sendPaymentReceivedEmail(
                patient.getEmail(),
                patient.getName(),
                docDisplay,
                fee,
                txnId,
                methodDisplay,
                saved.getAppointmentDate().toString());

        return saved;
    }

    public List<Map<String, Object>> getDoctorDailyQueue(Long doctorId, Authentication authentication) {
        requireOwnerOrAdmin(
                doctorRepository.findById(doctorId)
                        .map(Doctor::getEmail)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found")),
                authentication,
                "DOCTOR");
        LocalDate today = LocalDate.now();
        List<Appointment> todayList = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, today);
        todayList.sort(Comparator.comparing(Appointment::getAppointmentTime));

        List<Map<String, Object>> queue = new ArrayList<>();
        int pos = 1;
        for (Appointment a : todayList) {
            Map<String, Object> item = new HashMap<>();
            item.put("queueNumber", pos++);
            item.put("appointmentId", a.getId());
            item.put("appointmentDate", a.getAppointmentDate());
            item.put("appointmentTime", a.getAppointmentTime());
            item.put("patientName", a.getPatient() != null ? a.getPatient().getName() : "Patient");
            item.put("patientEmail", a.getPatient() != null ? a.getPatient().getEmail() : "");
            item.put("reason", a.getReason());
            item.put("status", a.getStatus());
            item.put("paymentStatus", a.getPaymentStatus());
            item.put("paymentMethod", a.getPaymentMethod());
            item.put("meetingUrl", a.getMeetingUrl());
            queue.add(item);
        }
        return queue;
    }

    @Transactional
    public Appointment updateAppointmentStatus(
            Long id,
            AppointmentStatus status,
            Authentication authentication) {
        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Appointment status is required");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
        patientRepository.findByIdForUpdate(appointment.getPatient().getId());
        doctorRepository.findByIdForUpdate(appointment.getDoctor().getId());
        appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        String role = role(authentication);
        if ("PATIENT".equals(role)) {
            requireOwnerOrAdmin(
                    appointment.getPatient().getEmail(),
                    authentication,
                    "PATIENT");
            if (status != AppointmentStatus.CANCELLED
                    || (appointment.getStatus() != AppointmentStatus.PENDING
                            && appointment.getStatus() != AppointmentStatus.CONFIRMED)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Patients may only cancel their pending or confirmed appointments");
            }
        } else if ("DOCTOR".equals(role)) {
            requireOwnerOrAdmin(
                    appointment.getDoctor().getEmail(),
                    authentication,
                    "DOCTOR");
            boolean validPendingTransition = appointment.getStatus() == AppointmentStatus.PENDING
                    && (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED);
            boolean validConfirmedTransition = appointment.getStatus() == AppointmentStatus.CONFIRMED
                    && (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED);

            if (!validPendingTransition && !validConfirmedTransition) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Doctors may only confirm/complete/cancel visits");
            }
        } else if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Not authorized to update appointments");
        }

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);

        Patient patient = saved.getPatient();
        Doctor doctor = saved.getDoctor();
        String formattedTime = formatTime12Hour(saved.getAppointmentTime());
        String docDisplay = doctorDisplayName(doctor);

        if (status == AppointmentStatus.CONFIRMED) {
            userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                    notificationService.createNotification(
                            u,
                            docDisplay + " CONFIRMED your appointment on " + saved.getAppointmentDate() + " at " + formattedTime,
                            "APPOINTMENT"));

            emailService.sendAppointmentConfirmedEmail(
                    patient.getEmail(),
                    patient.getName(),
                    docDisplay,
                    saved.getAppointmentDate().toString(),
                    formattedTime,
                    saved.getConsultationType(),
                    saved.getMeetingUrl());

        } else if (status == AppointmentStatus.CANCELLED) {
            userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                    notificationService.createNotification(
                            u,
                            "Your appointment with " + docDisplay + " on " + saved.getAppointmentDate() + " at " + formattedTime + " has been CANCELLED.",
                            "APPOINTMENT"));

            userRepository.findByEmail(doctor.getEmail()).ifPresent(u ->
                    notificationService.createNotification(
                            u,
                            "Appointment with " + patient.getName() + " on " + saved.getAppointmentDate() + " at " + formattedTime + " was CANCELLED.",
                            "APPOINTMENT"));

            emailService.sendAppointmentCancelledEmail(
                    patient.getEmail(),
                    patient.getName(),
                    docDisplay,
                    saved.getAppointmentDate().toString(),
                    formattedTime);

        } else if (status == AppointmentStatus.COMPLETED) {
            userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                    notificationService.createNotification(
                            u,
                            "Consultation with " + docDisplay + " marked as COMPLETED. Prescriptions are available under My Prescriptions.",
                            "APPOINTMENT"));

            emailService.sendAppointmentCompletedEmail(
                    patient.getEmail(),
                    patient.getName(),
                    docDisplay,
                    saved.getAppointmentDate().toString());
        }

        return saved;
    }

    public Appointment updateAppointment(
            Long id,
            Appointment updatedAppointment,
            Authentication authentication) {
        return appointmentRepository.findById(id)
                .map(existing -> {
                    if (authentication != null) {
                        requireOwnerOrAdmin(
                                existing.getPatient().getEmail(),
                                authentication,
                                "PATIENT");
                    }
                    existing.setAppointmentDate(updatedAppointment.getAppointmentDate());
                    existing.setAppointmentTime(updatedAppointment.getAppointmentTime());
                    existing.setReason(updatedAppointment.getReason());
                    Appointment saved = appointmentRepository.save(existing);

                    Patient patient = saved.getPatient();
                    Doctor doctor = saved.getDoctor();
                    String formattedTime = formatTime12Hour(saved.getAppointmentTime());
                    String docDisplay = doctorDisplayName(doctor);

                    userRepository.findByEmail(patient.getEmail()).ifPresent(u ->
                            notificationService.createNotification(
                                    u,
                                    "Your appointment with " + docDisplay + " was rescheduled to " + saved.getAppointmentDate() + " at " + formattedTime,
                                    "APPOINTMENT"));

                    userRepository.findByEmail(doctor.getEmail()).ifPresent(u ->
                            notificationService.createNotification(
                                    u,
                                    "Appointment with " + patient.getName() + " was rescheduled to " + saved.getAppointmentDate() + " at " + formattedTime,
                                    "APPOINTMENT"));

                    emailService.sendAppointmentRescheduledEmail(
                            patient.getEmail(),
                            patient.getName(),
                            docDisplay,
                            saved.getAppointmentDate().toString(),
                            formattedTime);

                    return saved;
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    public void deleteAppointment(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Appointment not found");
        }
        appointmentRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(
            String resourceEmail,
            Authentication authentication,
            String ownerRole) {
        String currentRole = role(authentication);
        if (!"ADMIN".equals(currentRole)
                && (!ownerRole.equals(currentRole)
                        || !resourceEmail.equalsIgnoreCase(authentication.getName()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's appointments");
        }
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }
}
