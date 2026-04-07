package com.postwork.clinicconnect.service;


import com.postwork.clinicconnect.model.ImmunizationGap;
import com.postwork.clinicconnect.model.Patient;
import com.postwork.clinicconnect.model.VaccinationRecord;
import com.postwork.clinicconnect.model.VaccineRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityEngineService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityEngineService.class);

    private static final int CRITICAL_THRESHOLD_DAYS = 365;
    private static final int HIGH_THRESHOLD_DAYS = 180;
    private static final int MEDIUM_THRESHOLD_DAYS = 90;

    public List<ImmunizationGap> analyzePatientGaps(
            Patient patient, List<VaccineRequirement> schedule) {

        List<ImmunizationGap> gaps = new ArrayList<>();
        LocalDate today = LocalDate.now();
        long patientAgeInDays = ChronoUnit.DAYS.between(patient.getDateOfBirth(), today);

        log.debug("Analyzing patient {} aged {} days", patient.getPatientId(), patientAgeInDays);

        for (VaccineRequirement requirement : schedule) {
            List<ImmunizationGap> vaccineGaps = evaluateVaccineGaps(
                    patient, requirement, patientAgeInDays, today);
            gaps.addAll(vaccineGaps);
        }

        return gaps.stream()
                .sorted(Comparator
                        .comparing(ImmunizationGap::getUrgencyLevel)
                        .thenComparing(Comparator.comparingLong(ImmunizationGap::getDaysOverdue)
                                .reversed()))
                .collect(Collectors.toList());
    }

    private List<ImmunizationGap> evaluateVaccineGaps(
            Patient patient,
            VaccineRequirement requirement,
            long patientAgeInDays,
            LocalDate today) {

        List<ImmunizationGap> gaps = new ArrayList<>();
        List<VaccinationRecord> patientHistory = getVaccineHistory(
                patient, requirement.getVaccineName());

        int completedDoses = patientHistory.size();

        for (VaccineRequirement.DoseSchedule doseSchedule : requirement.getDoseSchedule()) {
            if (doseSchedule.getDoseNumber() <= completedDoses) {
                continue;
            }

            if (patientAgeInDays < doseSchedule.getMinimumAgeInDays()) {
                continue;
            }

            if (doseSchedule.getMaximumAgeInDays() > 0
                    && patientAgeInDays > doseSchedule.getMaximumAgeInDays()) {
                continue;
            }

            LocalDate earliestEligible = calculateEarliestEligibleDate(
                    patient, doseSchedule, patientHistory);

            LocalDate recommendedDate = patient.getDateOfBirth()
                    .plusDays(doseSchedule.getRecommendedAgeInDays());

            if (!today.isBefore(earliestEligible)) {
                long daysOverdue = calculateDaysOverdue(today, recommendedDate);
                ImmunizationGap.UrgencyLevel urgency = calculateUrgencyLevel(daysOverdue, patientAgeInDays,
                        doseSchedule.getMaximumAgeInDays());

                ImmunizationGap gap = ImmunizationGap.builder()
                        .patientId(patient.getPatientId())
                        .patientName(patient.getFullName())
                        .guardianName(patient.getGuardianName())
                        .guardianPhone(patient.getGuardianPhone())
                        .dateOfBirth(patient.getDateOfBirth())
                        .patientAgeInDays((int) patientAgeInDays)
                        .vaccineName(requirement.getVaccineName())
                        .vaccineAbbreviation(requirement.getVaccineAbbreviation())
                        .missedDoseNumber(doseSchedule.getDoseNumber())
                        .recommendedDate(recommendedDate)
                        .nextEligibleDate(earliestEligible)
                        .daysOverdue(Math.max(0, daysOverdue))
                        .urgencyLevel(urgency)
                        .clinicalNote(buildClinicalNote(requirement, doseSchedule, daysOverdue))
                        .build();

                gaps.add(gap);
                log.debug("Gap identified: Patient {} missing {} dose {}",
                        patient.getPatientId(), requirement.getVaccineName(),
                        doseSchedule.getDoseNumber());
            }
        }

        return gaps;
    }

    private LocalDate calculateEarliestEligibleDate(
            Patient patient,
            VaccineRequirement.DoseSchedule doseSchedule,
            List<VaccinationRecord> history) {

        LocalDate ageBasedEligibility = patient.getDateOfBirth()
                .plusDays(doseSchedule.getMinimumAgeInDays());

        if (!history.isEmpty() && doseSchedule.getMinimumIntervalFromPreviousDoseInDays() > 0) {
            LocalDate lastDoseDate = history.stream()
                    .map(VaccinationRecord::getDateAdministered)
                    .max(LocalDate::compareTo)
                    .orElse(patient.getDateOfBirth());

            LocalDate intervalBasedEligibility = lastDoseDate
                    .plusDays(doseSchedule.getMinimumIntervalFromPreviousDoseInDays());

            return ageBasedEligibility.isAfter(intervalBasedEligibility)
                    ? ageBasedEligibility : intervalBasedEligibility;
        }

        return ageBasedEligibility;
    }

    private long calculateDaysOverdue(LocalDate today, LocalDate recommendedDate) {
        return ChronoUnit.DAYS.between(recommendedDate, today);
    }

    private ImmunizationGap.UrgencyLevel calculateUrgencyLevel(
            long daysOverdue, long patientAgeInDays, int maxAgeInDays) {

        if (maxAgeInDays > 0) {
            long daysUntilMaxAge = maxAgeInDays - patientAgeInDays;
            if (daysUntilMaxAge <= 30) {
                return ImmunizationGap.UrgencyLevel.CRITICAL;
            }
        }

        if (daysOverdue >= CRITICAL_THRESHOLD_DAYS) {
            return ImmunizationGap.UrgencyLevel.CRITICAL;
        } else if (daysOverdue >= HIGH_THRESHOLD_DAYS) {
            return ImmunizationGap.UrgencyLevel.HIGH;
        } else if (daysOverdue >= MEDIUM_THRESHOLD_DAYS) {
            return ImmunizationGap.UrgencyLevel.MEDIUM;
        } else {
            return ImmunizationGap.UrgencyLevel.LOW;
        }
    }

    private List<VaccinationRecord> getVaccineHistory(Patient patient, String vaccineName) {
        if (patient.getVaccinationHistory() == null) {
            return Collections.emptyList();
        }
        return patient.getVaccinationHistory().stream()
                .filter(record -> record.getVaccineName().equalsIgnoreCase(vaccineName))
                .sorted(Comparator.comparingInt(VaccinationRecord::getDoseNumber))
                .collect(Collectors.toList());
    }

    private String buildClinicalNote(
            VaccineRequirement requirement,
            VaccineRequirement.DoseSchedule doseSchedule,
            long daysOverdue) {

        StringBuilder note = new StringBuilder();
        note.append(requirement.getDescription()).append(". ");
        note.append("Dose ").append(doseSchedule.getDoseNumber()).append(" is ");

        if (daysOverdue > 0) {
            note.append(daysOverdue).append(" days overdue. ");
        } else {
            note.append("now due. ");
        }

        if (requirement.getCatchUpNote() != null && !requirement.getCatchUpNote().isBlank()) {
            note.append("Catch-up note: ").append(requirement.getCatchUpNote());
        }

        return note.toString().trim();
    }

    public Map<String, Long> calculateUrgencySummary(List<ImmunizationGap> gaps) {
        return gaps.stream()
                .collect(Collectors.groupingBy(
                        gap -> gap.getUrgencyLevel().name(),
                        Collectors.counting()));
    }

    public int countPatientsWithGaps(List<ImmunizationGap> gaps) {
        return (int) gaps.stream()
                .map(ImmunizationGap::getPatientId)
                .distinct()
                .count();
    }
}