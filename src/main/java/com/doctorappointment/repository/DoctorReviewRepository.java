package com.doctorappointment.repository;

import com.doctorappointment.entity.DoctorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {
    List<DoctorReview> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    Optional<DoctorReview> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);
    List<DoctorReview> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
