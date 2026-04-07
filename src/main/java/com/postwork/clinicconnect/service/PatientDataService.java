package com.postwork.clinicconnect.service;


import com.postwork.clinicconnect.exception.PatientNotFoundException;
import com.postwork.clinicconnect.model.Patient;
import com.postwork.clinicconnect.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientDataService {

    private static final Logger log = LoggerFactory.getLogger(PatientDataService.class);

    private final PatientRepository patientRepository;

    public PatientDataService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        log.info("Retrieved {} patients for analysis", patients.size());
        return patients;
    }

    public Patient getPatientById(String patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Patient not found with ID: " + patientId));
    }

    public List<Patient> getPatientsInAgeRange(int minDays, int maxDays) {
        return patientRepository.findByAgeRangeInDays(minDays, maxDays);
    }
}