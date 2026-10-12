package com.doctorappointment.repository;

import com.doctorappointment.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {

    Optional<Specialization> findByName(String name);

    boolean existsByName(String name);
}
