package com.doctorappointment.service;

import com.doctorappointment.dto.DoctorReviewRequest;
import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.AppointmentStatus;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.DoctorReview;
import com.doctorappointment.repository.AppointmentRepository;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.DoctorReviewRepository;
import com.doctorappointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DoctorReviewService {

    @Autowired
    private DoctorReviewRepository doctorReviewRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public DoctorReview createReview(DoctorReviewRequest request, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reviews can only be submitted for completed appointments.");
        }

        if (authentication != null && !appointment.getPatient().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review another patient's appointment.");
        }

        if (doctorReviewRepository.existsByAppointmentId(request.appointmentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A review has already been submitted for this appointment.");
        }

        DoctorReview review = new DoctorReview(
                appointment.getPatient(),
                appointment.getDoctor(),
                appointment,
                request.rating(),
                request.reviewText() == null ? "" : request.reviewText().trim()
        );

        DoctorReview saved = doctorReviewRepository.save(review);

        // Recalculate average rating for Doctor
        Doctor doctor = appointment.getDoctor();
        List<DoctorReview> doctorReviews = doctorReviewRepository.findByDoctorIdOrderByCreatedAtDesc(doctor.getId());
        double sum = 0.0;
        for (DoctorReview r : doctorReviews) {
            sum += r.getRating();
        }
        double avg = doctorReviews.isEmpty() ? 5.0 : Math.round((sum / doctorReviews.size()) * 10.0) / 10.0;
        doctor.setRating(avg);
        doctor.setReviewCount(doctorReviews.size());
        doctorRepository.save(doctor);

        // Notify Doctor
        userRepository.findByEmail(doctor.getEmail()).ifPresent(user -> {
            notificationService.createNotification(
                    user,
                    "Patient " + appointment.getPatient().getName() + " left a " + request.rating() + "⭐ review for appointment #" + appointment.getId() + ".",
                    "REVIEW"
            );
        });

        return saved;
    }

    @Transactional(readOnly = true)
    public List<DoctorReview> getDoctorReviews(Long doctorId) {
        return doctorReviewRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    @Transactional(readOnly = true)
    public List<DoctorReview> getPatientReviews(Long patientId) {
        return doctorReviewRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }
}
