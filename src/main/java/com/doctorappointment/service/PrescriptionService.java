package com.doctorappointment.service;

import com.doctorappointment.dto.PrescriptionCreateRequest;
import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.Prescription;
import com.doctorappointment.repository.AppointmentRepository;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.doctorappointment.repository.UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Prescription createPrescription(PrescriptionCreateRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + request.getDoctorId()));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElse(null);
        }

        Prescription prescription = new Prescription(
                patient,
                doctor,
                appointment,
                request.getDiagnosis(),
                request.getMedicines(),
                request.getInstructions()
        );

        Prescription saved = prescriptionRepository.save(prescription);

        // Notify patient
        String rawDocName = doctor.getName() != null ? doctor.getName().trim() : "Doctor";
        String docName = (rawDocName.toLowerCase().startsWith("dr.") || rawDocName.toLowerCase().startsWith("dr "))
                ? rawDocName : "Dr. " + rawDocName;
        userRepository.findByEmail(patient.getEmail()).ifPresent(user -> {
            notificationService.createNotification(
                    user,
                    docName + " issued a new digital prescription for you.",
                    "PRESCRIPTION"
            );
        });

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Prescription> getPatientPrescriptions(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<Prescription> getDoctorPrescriptions(Long doctorId) {
        return prescriptionRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    @Transactional(readOnly = true)
    public Optional<Prescription> getAppointmentPrescription(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    @Transactional(readOnly = true)
    public Prescription getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + id));
    }
}
