package com.postwork.clinicconnect.controller;

import com.postwork.clinicconnect.model.ClinicActionReport;
import com.postwork.clinicconnect.model.ImmunizationGap;
import com.postwork.clinicconnect.model.Patient;
import com.postwork.clinicconnect.model.VaccineRequirement;
import com.postwork.clinicconnect.service.CdcScheduleService;
import com.postwork.clinicconnect.service.EligibilityEngineService;
import com.postwork.clinicconnect.service.PatientDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/immunization")
public class ImmunizationController {

    private static final Logger log = LoggerFactory.getLogger(ImmunizationController.class);

    private final PatientDataService patientDataService;
    private final CdcScheduleService cdcScheduleService;
    private final EligibilityEngineService eligibilityEngineService;

    public ImmunizationController(
            PatientDataService patientDataService,
            CdcScheduleService cdcScheduleService,
            EligibilityEngineService eligibilityEngineService) {
        this.patientDataService = patientDataService;
        this.cdcScheduleService = cdcScheduleService;
        this.eligibilityEngineService = eligibilityEngineService;
    }

    @GetMapping("/daily-report")
    public ResponseEntity<ClinicActionReport> getDailyClinicReport() {
        log.info("Generating daily clinic action report");

        boolean usedFallback = !cdcScheduleService.isRemoteSourceAvailable();
        List<VaccineRequirement> schedule = cdcScheduleService.fetchImmunizationSchedule();
        List<Patient> patients = patientDataService.getAllPatients();

        List<ImmunizationGap> allGaps = patients.stream()
                .flatMap(patient -> eligibilityEngineService
                        .analyzePatientGaps(patient, schedule).stream())
                .sorted(Comparator
                        .comparing(ImmunizationGap::getUrgencyLevel)
                        .thenComparing(Comparator.comparingLong(ImmunizationGap::getDaysOverdue)
                                .reversed()))
                .collect(Collectors.toList());

        Map<String, Long> urgencySummary = eligibilityEngineService.calculateUrgencySummary(allGaps);
        int patientsWithGaps = eligibilityEngineService.countPatientsWithGaps(allGaps);

        ClinicActionReport report = ClinicActionReport.builder()
                .reportId(UUID.randomUUID().toString())
                .reportDate(LocalDate.now())
                .generatedAt(LocalDateTime.now())
                .clinicName("Starlight Pediatrics")
                .totalPatientsAnalyzed(patients.size())
                .totalPatientsWithGaps(patientsWithGaps)
                .totalGapsIdentified(allGaps.size())
                .gapsByUrgencyLevel(urgencySummary)
                .actionItems(allGaps)
                .dataSource(usedFallback ? "LOCAL_FALLBACK" : "CDC_REMOTE")
                .usedFallbackData(usedFallback)
                .build();

        log.info("Report generated: {} gaps found across {} patients",
                allGaps.size(), patientsWithGaps);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/patient/{patientId}/gaps")
    public ResponseEntity<List<ImmunizationGap>> getPatientGaps(
            @PathVariable String patientId) {
        log.info("Fetching immunization gaps for patient: {}", patientId);

        Patient patient = patientDataService.getPatientById(patientId);
        List<VaccineRequirement> schedule = cdcScheduleService.fetchImmunizationSchedule();
        List<ImmunizationGap> gaps = eligibilityEngineService.analyzePatientGaps(patient, schedule);

        return ResponseEntity.ok(gaps);
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientDataService.getAllPatients());
    }

    @GetMapping("/schedule")
    public ResponseEntity<List<VaccineRequirement>> getImmunizationSchedule() {
        List<VaccineRequirement> schedule = cdcScheduleService.fetchImmunizationSchedule();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("clinic", "Starlight Pediatrics");
        health.put("timestamp", LocalDateTime.now());
        health.put("cdcApiAvailable", cdcScheduleService.isRemoteSourceAvailable());
        health.put("totalPatients", patientDataService.getAllPatients().size());
        return ResponseEntity.ok(health);
    }
}