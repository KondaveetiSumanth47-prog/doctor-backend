package com.doctorappointment.service;

import com.doctorappointment.entity.Availability;
import com.doctorappointment.entity.Doctor;
import com.doctorappointment.repository.AvailabilityRepository;
import com.doctorappointment.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;

    public AvailabilityService(
            AvailabilityRepository availabilityRepository,
            DoctorRepository doctorRepository) {
        this.availabilityRepository = availabilityRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<Availability> getAllAvailabilities() {
        return availabilityRepository.findAll();
    }

    public Optional<Availability> getAvailabilityById(
            Long id,
            Authentication authentication) {
        Optional<Availability> availability = availabilityRepository.findById(id);
        availability.ifPresent(value -> requireOwnerOrAdmin(
                value.getDoctor(), authentication));
        return availability;
    }

    public List<Availability> getDoctorAvailabilities(Long doctorId) {
        return availabilityRepository.findByDoctorId(doctorId);
    }

    public List<Availability> getDoctorAvailabilityByDay(
            Long doctorId,
            DayOfWeek dayOfWeek) {

        return availabilityRepository
                .findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
    }

    @Transactional
    public Availability saveAvailability(
            Availability availability,
            Authentication authentication) {
        if (availability.getDoctor() == null
                || availability.getDoctor().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A doctor must be selected");
        }
        Doctor doctor = findAndAuthorizeDoctor(
                availability.getDoctor().getId(), authentication);
        validateTimeRange(availability);
        availability.setDoctor(doctor);
        return availabilityRepository.save(availability);
    }

    public Availability updateAvailability(
            Long id,
            Availability updatedAvailability,
            Authentication authentication) {

        return availabilityRepository.findById(id)
                .map(existingAvailability -> {
                    requireOwnerOrAdmin(
                            existingAvailability.getDoctor(), authentication);
                    if (updatedAvailability.getDoctor() == null
                            || updatedAvailability.getDoctor().getId() == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "A doctor must be selected");
                    }
                    Doctor doctor = findAndAuthorizeDoctor(
                            updatedAvailability.getDoctor().getId(),
                            authentication);
                    validateTimeRange(updatedAvailability);
                    existingAvailability.setDoctor(doctor);

                    existingAvailability.setDayOfWeek(
                            updatedAvailability.getDayOfWeek());

                    existingAvailability.setStartTime(
                            updatedAvailability.getStartTime());

                    existingAvailability.setEndTime(
                            updatedAvailability.getEndTime());

                    return availabilityRepository.save(
                            existingAvailability);
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Availability not found with id: " + id));
    }

    public void deleteAvailability(
            Long id,
            Authentication authentication) {
        Availability availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Availability not found"));
        requireOwnerOrAdmin(availability.getDoctor(), authentication);
        availabilityRepository.deleteById(id);
    }

    private Doctor findAndAuthorizeDoctor(
            Long doctorId,
            Authentication authentication) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));
        requireOwnerOrAdmin(doctor, authentication);
        return doctor;
    }

    private void requireOwnerOrAdmin(
            Doctor doctor,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !doctor.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage another doctor's availability");
        }
    }

    private void validateTimeRange(Availability availability) {
        if (availability.getStartTime() == null
                || availability.getEndTime() == null
                || !availability.getStartTime().isBefore(availability.getEndTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Availability start time must be before its end time");
        }
    }
}