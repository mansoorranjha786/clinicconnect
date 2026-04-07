package com.postwork.clinicconnect.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImmunizationGap {

    private String patientId;
    private String patientName;
    private String guardianName;
    private String guardianPhone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private int patientAgeInDays;
    private String vaccineName;
    private String vaccineAbbreviation;
    private int missedDoseNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recommendedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextEligibleDate;

    private long daysOverdue;
    private UrgencyLevel urgencyLevel;
    private String clinicalNote;

    public enum UrgencyLevel {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}