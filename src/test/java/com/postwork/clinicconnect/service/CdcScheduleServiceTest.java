package com.postwork.clinicconnect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.postwork.clinicconnect.model.VaccineRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CdcScheduleServiceTest {

    @Mock
    private WebClient cdcWebClient;

    private CdcScheduleService cdcScheduleService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        cdcScheduleService = new CdcScheduleService(cdcWebClient, objectMapper);
        ReflectionTestUtils.setField(cdcScheduleService, "scheduleEndpoint", "/test");
        ReflectionTestUtils.setField(cdcScheduleService, "maxRetryAttempts", 1);
        ReflectionTestUtils.setField(cdcScheduleService, "retryDelaySeconds", 1);
    }

    @Test
    @DisplayName("Fallback schedule should load successfully from classpath resource")
    void fallbackSchedule_shouldLoadSuccessfully() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        assertThat(schedule).isNotNull();
        assertThat(schedule).isNotEmpty();
        assertThat(schedule).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Fallback schedule should contain HepB vaccine")
    void fallbackSchedule_shouldContainHepB() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        assertThat(schedule).anyMatch(v -> v.getVaccineName().equals("HepB"));
    }

    @Test
    @DisplayName("Fallback schedule should contain MMR vaccine")
    void fallbackSchedule_shouldContainMMR() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        assertThat(schedule).anyMatch(v -> v.getVaccineName().equals("MMR"));
    }

    @Test
    @DisplayName("Fallback schedule should contain DTaP vaccine")
    void fallbackSchedule_shouldContainDTaP() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        assertThat(schedule).anyMatch(v -> v.getVaccineName().equals("DTaP"));
    }

    @Test
    @DisplayName("Fallback schedule should contain Varicella vaccine")
    void fallbackSchedule_shouldContainVaricella() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        assertThat(schedule).anyMatch(v -> v.getVaccineName().equals("Varicella"));
    }

    @Test
    @DisplayName("Each vaccine in fallback schedule should have at least one dose schedule entry")
    void fallbackSchedule_eachVaccineShouldHaveDoseSchedule() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        schedule.forEach(vaccine -> {
            assertThat(vaccine.getDoseSchedule()).isNotNull();
            assertThat(vaccine.getDoseSchedule()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("HepB in fallback schedule should have 3 doses")
    void hepBInFallbackSchedule_shouldHaveThreeDoses() {
        List<VaccineRequirement> schedule = cdcScheduleService.loadFallbackSchedule();

        VaccineRequirement hepB = schedule.stream()
                .filter(v -> v.getVaccineName().equals("HepB"))
                .findFirst()
                .orElseThrow();

        assertThat(hepB.getDoseSchedule()).hasSize(3);
    }

}