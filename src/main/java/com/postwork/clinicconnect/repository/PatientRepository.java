package com.postwork.clinicconnect.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postwork.clinicconnect.model.Patient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientRepository {

    private static final Logger log = LoggerFactory.getLogger(PatientRepository.class);

    private final ObjectMapper objectMapper;
    private List<Patient> patients = Collections.emptyList();

    public PatientRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadPatients() {
        try {
            ClassPathResource resource = new ClassPathResource("patients.json");
            InputStream inputStream = resource.getInputStream();
            patients = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            log.info("Loaded {} patients from local data store", patients.size());
        } catch (IOException e) {
            log.error("Failed to load patient data from patients.json: {}", e.getMessage());
            patients = Collections.emptyList();
        }
    }

    public List<Patient> findAll() {
        return Collections.unmodifiableList(patients);
    }

    public Optional<Patient> findById(String patientId) {
        return patients.stream()
                .filter(p -> p.getPatientId().equals(patientId))
                .findFirst();
    }

    public List<Patient> findByAgeRangeInDays(int minDays, int maxDays) {
        return patients.stream()
                .filter(p -> {
                    long ageInDays = java.time.temporal.ChronoUnit.DAYS.between(
                            p.getDateOfBirth(), java.time.LocalDate.now());
                    return ageInDays >= minDays && ageInDays <= maxDays;
                })
                .toList();
    }
}