package com.doctorappointment.controller;

import com.doctorappointment.dto.AppointmentCreateRequest;
import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.AppointmentStatus;
import com.doctorappointment.service.AppointmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(
            @PathVariable Long id,
            Authentication authentication) {
        return appointmentService.getAppointmentById(id, authentication)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(
            @PathVariable Long doctorId,
            Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentsByDoctor(
                        doctorId, authentication));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(
            @PathVariable Long patientId,
            Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentsByPatient(
                        patientId, authentication));
    }

    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<List<Appointment>> getDoctorAppointmentsByDate(
            @PathVariable Long doctorId,
            @PathVariable LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.getDoctorAppointmentsByDate(
                        doctorId, date, authentication));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Appointment>> getAppointmentsByStatus(
            @PathVariable AppointmentStatus status) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            @Valid @RequestBody AppointmentCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(
                        request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable Long id,
            @RequestBody Appointment appointment,
            Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.updateAppointment(id, appointment, authentication));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(
                appointmentService.updateAppointmentStatus(
                        id, status, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctor/{doctorId}/queue")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getDoctorQueue(
            @PathVariable Long doctorId,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getDoctorDailyQueue(doctorId, authentication));
    }

    @PutMapping("/{id}/meeting-url")
    public ResponseEntity<Appointment> updateMeetingUrl(
            @PathVariable Long id,
            @RequestParam String meetingUrl,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.updateMeetingUrl(id, meetingUrl, authentication));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Appointment> processPayment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "DEBIT_CARD") String paymentMethod,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.processPayment(id, paymentMethod, authentication));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exportAppointmentsCsv() {
        List<Appointment> list = appointmentService.getAllAppointments();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Patient Name,Patient Email,Doctor Name,Doctor Specialization,Date,Time,Status,Reason\n");
        for (Appointment a : list) {
            String patientName = a.getPatient() != null ? a.getPatient().getName() : "";
            String patientEmail = a.getPatient() != null ? a.getPatient().getEmail() : "";
            String doctorName = a.getDoctor() != null ? a.getDoctor().getName() : "";
            String doctorSpec = a.getDoctor() != null && a.getDoctor().getSpecialization() != null
                    ? a.getDoctor().getSpecialization() : "";
            String reason = a.getReason() != null ? a.getReason().replace(",", " ") : "";
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,\"%s\"\n",
                    a.getId(), patientName, patientEmail, doctorName, doctorSpec,
                    a.getAppointmentDate(), a.getAppointmentTime(), a.getStatus(), reason));
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"appointments_report.csv\"")
                .body(csv.toString());
    }
}
