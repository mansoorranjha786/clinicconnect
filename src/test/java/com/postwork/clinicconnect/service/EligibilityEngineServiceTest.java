package com.postwork.clinicconnect.service;

import com.postwork.clinicconnect.model.ImmunizationGap;
import com.postwork.clinicconnect.model.Patient;
import com.postwork.clinicconnect.model.VaccinationRecord;
import com.postwork.clinicconnect.model.VaccineRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEngineServiceTest {

    private EligibilityEngineService eligibilityEngine;
    private List<VaccineRequirement> testSchedule;

    @BeforeEach
    void setUp() {
        eligibilityEngine = new EligibilityEngineService();
        testSchedule = buildTestSchedule();
    }

    @Test
    @DisplayName("Newborn with no vaccinations should flag HepB dose 1 as immediately due")
    void newbornWithNoVaccinations_shouldFlagHepBDose1() {
        Patient patient = Patient.builder()
                .patientId("TEST001")
                .firstName("Baby")
                .lastName("Test")
                .dateOfBirth(LocalDate.now().minusDays(30))
                .guardianName("Guardian Test")
                .guardianPhone("555-9999")
                .vaccinationHistory(Collections.emptyList())
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        assertThat(gaps).isNotEmpty();
        assertThat(gaps).anyMatch(gap ->
                gap.getVaccineName().equals("HepB") && gap.getMissedDoseNumber() == 1);
    }

    @Test
    @DisplayName("18-month-old missing HepB dose 3 should be flagged as overdue")
    void eighteenMonthOldMissingHepBDose3_shouldBeOverdue() {
        LocalDate birthDate = LocalDate.now().minusDays(548);

        Patient patient = Patient.builder()
                .patientId("TEST002")
                .firstName("Toddler")
                .lastName("Test")
                .dateOfBirth(birthDate)
                .guardianName("Guardian Test")
                .guardianPhone("555-9998")
                .vaccinationHistory(Arrays.asList(
                        VaccinationRecord.builder()
                                .vaccineName("HepB")
                                .doseNumber(1)
                                .dateAdministered(birthDate)
                                .build(),
                        VaccinationRecord.builder()
                                .vaccineName("HepB")
                                .doseNumber(2)
                                .dateAdministered(birthDate.plusDays(30))
                                .build()
                ))
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        assertThat(gaps).anyMatch(gap ->
                gap.getVaccineName().equals("HepB")
                        && gap.getMissedDoseNumber() == 3
                        && gap.getDaysOverdue() > 0);
    }

    @Test
    @DisplayName("Patient with all vaccines completed should have zero gaps")
    void patientWithAllVaccinesCompleted_shouldHaveZeroGaps() {
        LocalDate birthDate = LocalDate.now().minusDays(3650);

        Patient patient = Patient.builder()
                .patientId("TEST003")
                .firstName("Complete")
                .lastName("Test")
                .dateOfBirth(birthDate)
                .guardianName("Guardian Test")
                .guardianPhone("555-9997")
                .vaccinationHistory(Arrays.asList(
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(1)
                                .dateAdministered(birthDate).build(),
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(2)
                                .dateAdministered(birthDate.plusDays(30)).build(),
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(3)
                                .dateAdministered(birthDate.plusDays(180)).build(),
                        VaccinationRecord.builder().vaccineName("MMR").doseNumber(1)
                                .dateAdministered(birthDate.plusDays(365)).build(),
                        VaccinationRecord.builder().vaccineName("MMR").doseNumber(2)
                                .dateAdministered(birthDate.plusDays(1825)).build(),
                        VaccinationRecord.builder().vaccineName("Varicella").doseNumber(1)
                                .dateAdministered(birthDate.plusDays(365)).build(),
                        VaccinationRecord.builder().vaccineName("Varicella").doseNumber(2)
                                .dateAdministered(birthDate.plusDays(1825)).build()
                ))
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        long hepBGaps = gaps.stream()
                .filter(g -> g.getVaccineName().equals("HepB")).count();
        long mmrGaps = gaps.stream()
                .filter(g -> g.getVaccineName().equals("MMR")).count();
        long varGaps = gaps.stream()
                .filter(g -> g.getVaccineName().equals("Varicella")).count();

        assertThat(hepBGaps).isZero();
        assertThat(mmrGaps).isZero();
        assertThat(varGaps).isZero();
    }

    @Test
    @DisplayName("Gaps should be sorted by urgency level with CRITICAL first")
    void gaps_shouldBeSortedByUrgencyWithCriticalFirst() {
        Patient patient = Patient.builder()
                .patientId("TEST004")
                .firstName("Sort")
                .lastName("Test")
                .dateOfBirth(LocalDate.now().minusDays(730))
                .guardianName("Guardian Test")
                .guardianPhone("555-9996")
                .vaccinationHistory(Collections.emptyList())
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        if (gaps.size() >= 2) {
            ImmunizationGap.UrgencyLevel first = gaps.get(0).getUrgencyLevel();
            ImmunizationGap.UrgencyLevel last = gaps.get(gaps.size() - 1).getUrgencyLevel();
            assertThat(first.ordinal()).isLessThanOrEqualTo(last.ordinal());
        }
    }

    @Test
    @DisplayName("Patient under minimum age for vaccine should not be flagged")
    void patientUnderMinimumAge_shouldNotBeFlagged() {
        LocalDate birthDate = LocalDate.now().minusDays(10);

        Patient patient = Patient.builder()
                .patientId("TEST005")
                .firstName("Newborn")
                .lastName("Test")
                .dateOfBirth(birthDate)
                .guardianName("Guardian Test")
                .guardianPhone("555-9995")
                .vaccinationHistory(Collections.emptyList())
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        assertThat(gaps).noneMatch(gap -> gap.getVaccineName().equals("MMR"));
        assertThat(gaps).noneMatch(gap -> gap.getVaccineName().equals("Varicella"));
    }

    @Test
    @DisplayName("Days overdue should never be negative")
    void daysOverdue_shouldNeverBeNegative() {
        Patient patient = Patient.builder()
                .patientId("TEST006")
                .firstName("Recent")
                .lastName("Test")
                .dateOfBirth(LocalDate.now().minusDays(45))
                .guardianName("Guardian Test")
                .guardianPhone("555-9994")
                .vaccinationHistory(Collections.emptyList())
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        gaps.forEach(gap ->
                assertThat(gap.getDaysOverdue()).isGreaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Gap overdue more than 365 days should be CRITICAL urgency")
    void gapOverdue365Days_shouldBeCriticalUrgency() {
        LocalDate birthDate = LocalDate.now().minusDays(730);

        Patient patient = Patient.builder()
                .patientId("TEST007")
                .firstName("Overdue")
                .lastName("Test")
                .dateOfBirth(birthDate)
                .guardianName("Guardian Test")
                .guardianPhone("555-9993")
                .vaccinationHistory(Collections.emptyList())
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        assertThat(gaps).anyMatch(gap -> gap.getUrgencyLevel() == ImmunizationGap.UrgencyLevel.CRITICAL);
    }

    @Test
    @DisplayName("Urgency summary should count all urgency levels")
    void urgencySummary_shouldCountAllLevels() {
        List<ImmunizationGap> gaps = Arrays.asList(
                buildGap("P1", ImmunizationGap.UrgencyLevel.CRITICAL),
                buildGap("P2", ImmunizationGap.UrgencyLevel.CRITICAL),
                buildGap("P3", ImmunizationGap.UrgencyLevel.HIGH),
                buildGap("P4", ImmunizationGap.UrgencyLevel.MEDIUM),
                buildGap("P5", ImmunizationGap.UrgencyLevel.LOW)
        );

        Map<String, Long> summary = eligibilityEngine.calculateUrgencySummary(gaps);

        assertThat(summary.get("CRITICAL")).isEqualTo(2L);
        assertThat(summary.get("HIGH")).isEqualTo(1L);
        assertThat(summary.get("MEDIUM")).isEqualTo(1L);
        assertThat(summary.get("LOW")).isEqualTo(1L);
    }

    @Test
    @DisplayName("countPatientsWithGaps should count distinct patients only")
    void countPatientsWithGaps_shouldCountDistinctPatients() {
        List<ImmunizationGap> gaps = Arrays.asList(
                buildGap("PAT001", ImmunizationGap.UrgencyLevel.HIGH),
                buildGap("PAT001", ImmunizationGap.UrgencyLevel.LOW),
                buildGap("PAT002",ImmunizationGap.UrgencyLevel.CRITICAL)
        );

        int count = eligibilityEngine.countPatientsWithGaps(gaps);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Patient with no vaccination history list should be handled gracefully")
    void patientWithNullVaccinationHistory_shouldHandleGracefully() {
        Patient patient = Patient.builder()
                .patientId("TEST008")
                .firstName("Null")
                .lastName("History")
                .dateOfBirth(LocalDate.now().minusDays(400))
                .guardianName("Guardian Test")
                .guardianPhone("555-9992")
                .vaccinationHistory(null)
                .build();

        List<ImmunizationGap> gaps = eligibilityEngine.analyzePatientGaps(patient, testSchedule);

        assertThat(gaps).isNotNull();
        assertThat(gaps).isNotEmpty();
    }

    @Test
    @DisplayName("Three distinct overdue scenarios are correctly identified")
    void threeDistinctOverdueScenarios_shouldBeIdentified() {
        Patient infant = Patient.builder()
                .patientId("SCENARIO1")
                .firstName("Infant")
                .lastName("One")
                .dateOfBirth(LocalDate.now().minusDays(548))
                .guardianName("Parent One")
                .guardianPhone("555-0001")
                .vaccinationHistory(Collections.singletonList(
                        VaccinationRecord.builder()
                                .vaccineName("HepB").doseNumber(1)
                                .dateAdministered(LocalDate.now().minusDays(548))
                                .build()
                ))
                .build();

        Patient toddler = Patient.builder()
                .patientId("SCENARIO2")
                .firstName("Toddler")
                .lastName("Two")
                .dateOfBirth(LocalDate.now().minusDays(730))
                .guardianName("Parent Two")
                .guardianPhone("555-0002")
                .vaccinationHistory(Collections.emptyList())
                .build();

        Patient child = Patient.builder()
                .patientId("SCENARIO3")
                .firstName("Child")
                .lastName("Three")
                .dateOfBirth(LocalDate.now().minusDays(2000))
                .guardianName("Parent Three")
                .guardianPhone("555-0003")
                .vaccinationHistory(Arrays.asList(
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(1)
                                .dateAdministered(LocalDate.now().minusDays(2000)).build(),
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(2)
                                .dateAdministered(LocalDate.now().minusDays(1940)).build(),
                        VaccinationRecord.builder().vaccineName("HepB").doseNumber(3)
                                .dateAdministered(LocalDate.now().minusDays(1820)).build(),
                        VaccinationRecord.builder().vaccineName("MMR").doseNumber(1)
                                .dateAdministered(LocalDate.now().minusDays(1635)).build()
                ))
                .build();

        List<ImmunizationGap> gaps1 = eligibilityEngine.analyzePatientGaps(infant, testSchedule);
        List<ImmunizationGap> gaps2 = eligibilityEngine.analyzePatientGaps(toddler, testSchedule);
        List<ImmunizationGap> gaps3 = eligibilityEngine.analyzePatientGaps(child, testSchedule);

        assertThat(gaps1).anyMatch(g -> g.getVaccineName().equals("HepB") && g.getDaysOverdue() > 0);
        assertThat(gaps2).anyMatch(g -> g.getVaccineName().equals("DTaP") && g.getDaysOverdue() > 0);
        assertThat(gaps3).anyMatch(g -> g.getVaccineName().equals("MMR") && g.getDaysOverdue() > 0);
    }

    private List<VaccineRequirement> buildTestSchedule() {
        VaccineRequirement hepB = VaccineRequirement.builder()
                .vaccineName("HepB")
                .vaccineAbbreviation("HepB")
                .description("Hepatitis B vaccine")
                .catchUpNote("Catch-up through age 18")
                .doseSchedule(Arrays.asList(
                        new VaccineRequirement.DoseSchedule(1, 0, 0, 0, 0),
                        new VaccineRequirement.DoseSchedule(2, 28, 60, 0, 28),
                        new VaccineRequirement.DoseSchedule(3, 168, 180, 0, 56)
                ))
                .build();

        VaccineRequirement dtap = VaccineRequirement.builder()
                .vaccineName("DTaP")
                .vaccineAbbreviation("DTaP")
                .description("Diphtheria Tetanus Pertussis vaccine")
                .catchUpNote("Catch-up for unvaccinated children")
                .doseSchedule(Arrays.asList(
                        new VaccineRequirement.DoseSchedule(1, 42, 60, 0, 0),
                        new VaccineRequirement.DoseSchedule(2, 70, 120, 0, 28),
                        new VaccineRequirement.DoseSchedule(3, 98, 180, 0, 28),
                        new VaccineRequirement.DoseSchedule(4, 365, 456, 0, 182),
                        new VaccineRequirement.DoseSchedule(5, 1460, 1825, 2555, 182)
                ))
                .build();

        VaccineRequirement mmr = VaccineRequirement.builder()
                .vaccineName("MMR")
                .vaccineAbbreviation("MMR")
                .description("Measles Mumps Rubella vaccine")
                .catchUpNote("Two doses required")
                .doseSchedule(Arrays.asList(
                        new VaccineRequirement.DoseSchedule(1, 365, 365, 0, 0),
                        new VaccineRequirement.DoseSchedule(2, 1460, 1825, 0, 28)
                ))
                .build();

        VaccineRequirement varicella = VaccineRequirement.builder()
                .vaccineName("Varicella")
                .vaccineAbbreviation("VAR")
                .description("Varicella Chickenpox vaccine")
                .catchUpNote("Two doses required")
                .doseSchedule(Arrays.asList(
                        new VaccineRequirement.DoseSchedule(1, 365, 365, 0, 0),
                        new VaccineRequirement.DoseSchedule(2, 1460, 1825, 0, 84)
                ))
                .build();

        return Arrays.asList(hepB, dtap, mmr, varicella);
    }

    private ImmunizationGap buildGap(String patientId, ImmunizationGap.UrgencyLevel urgency) {
        return ImmunizationGap.builder()
                .patientId(patientId)
                .patientName("Test Patient")
                .vaccineName("TestVaccine")
                .vaccineAbbreviation("TV")
                .missedDoseNumber(1)
                .daysOverdue(30)
                .urgencyLevel(urgency)
                .build();
    }
}