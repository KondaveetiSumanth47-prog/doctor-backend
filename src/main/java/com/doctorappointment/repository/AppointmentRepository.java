package com.doctorappointment.repository;

import com.doctorappointment.entity.Appointment;
import com.doctorappointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDate(
            Long doctorId,
            LocalDate appointmentDate
    );

    List<Appointment> findByStatus(AppointmentStatus status);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long doctorId,
            LocalDate appointmentDate,
            java.time.LocalTime appointmentTime,
            AppointmentStatus status);

    boolean existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long patientId,
            LocalDate appointmentDate,
            java.time.LocalTime appointmentTime,
            AppointmentStatus status);
}
