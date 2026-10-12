package com.doctorappointment.repository;

import com.doctorappointment.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from Patient patient where lower(patient.email) = lower(:email)")
    Optional<Patient> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from Patient patient where patient.id = :id")
    Optional<Patient> findByIdForUpdate(@Param("id") Long id);
}