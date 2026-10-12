package com.doctorappointment.service;

import com.doctorappointment.entity.Specialization;
import com.doctorappointment.repository.SpecializationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpecializationService {

    private final SpecializationRepository specializationRepository;

    public SpecializationService(
            SpecializationRepository specializationRepository) {
        this.specializationRepository = specializationRepository;
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    public Optional<Specialization> getSpecializationById(Long id) {
        return specializationRepository.findById(id);
    }

    public Optional<Specialization> getSpecializationByName(String name) {
        return specializationRepository.findByName(name);
    }

    public Specialization saveSpecialization(Specialization specialization) {
        return specializationRepository.save(specialization);
    }

    public Specialization updateSpecialization(
            Long id,
            Specialization updatedSpecialization) {

        return specializationRepository.findById(id)
                .map(existingSpecialization -> {
                    existingSpecialization.setName(
                            updatedSpecialization.getName());

                    existingSpecialization.setDescription(
                            updatedSpecialization.getDescription());

                    return specializationRepository.save(
                            existingSpecialization);
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Specialization not found with id: " + id));
    }

    public void deleteSpecialization(Long id) {
        specializationRepository.deleteById(id);
    }
}