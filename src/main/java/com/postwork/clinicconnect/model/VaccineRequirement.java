package com.postwork.clinicconnect.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRequirement {

    private String vaccineName;
    private String vaccineAbbreviation;
    private String description;
    private List<DoseSchedule> doseSchedule;
    private String catchUpNote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoseSchedule {
        private int doseNumber;
        private int minimumAgeInDays;
        private int recommendedAgeInDays;
        private int maximumAgeInDays;
        private int minimumIntervalFromPreviousDoseInDays;
    }
}