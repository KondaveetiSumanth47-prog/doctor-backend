package com.doctorappointment.controller;

import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.AppointmentStatus;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.repository.AppointmentRepository;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        Map<String, Object> data = new HashMap<>();

        long totalPatients = patientRepository.count();
        List<Doctor> doctors = doctorRepository.findAll();
        long totalDoctors = doctors.size();
        long approvedDoctors = doctors.stream().filter(d -> "APPROVED".equalsIgnoreCase(d.getVerificationStatus())).count();
        long pendingDoctors = doctors.stream().filter(d -> "PENDING".equalsIgnoreCase(d.getVerificationStatus())).count();

        List<Appointment> appointments = appointmentRepository.findAll();
        long totalAppointments = appointments.size();
        long pendingAppointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();
        long confirmedAppointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long completedAppointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelledAppointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        Map<String, Long> specialtyDist = new HashMap<>();
        for (Appointment a : appointments) {
            if (a.getDoctor() != null && a.getDoctor().getSpecialization() != null) {
                String spec = a.getDoctor().getSpecialization();
                specialtyDist.put(spec, specialtyDist.getOrDefault(spec, 0L) + 1);
            }
        }

        Map<String, Long> monthlyTrends = new TreeMap<>();
        for (Appointment a : appointments) {
            if (a.getAppointmentDate() != null) {
                String monthKey = a.getAppointmentDate().getYear() + "-" + String.format("%02d", a.getAppointmentDate().getMonthValue());
                monthlyTrends.put(monthKey, monthlyTrends.getOrDefault(monthKey, 0L) + 1);
            }
        }

        data.put("totalPatients", totalPatients);
        data.put("totalDoctors", totalDoctors);
        data.put("approvedDoctors", approvedDoctors);
        data.put("pendingDoctors", pendingDoctors);
        data.put("totalAppointments", totalAppointments);
        data.put("pendingAppointments", pendingAppointments);
        data.put("confirmedAppointments", confirmedAppointments);
        data.put("completedAppointments", completedAppointments);
        data.put("cancelledAppointments", cancelledAppointments);
        data.put("specialtyDistribution", specialtyDist);
        data.put("monthlyTrends", monthlyTrends);

        return ResponseEntity.ok(data);
    }
}
