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
public class VaccinationRecord {

    private String vaccineName;
    private int doseNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateAdministered;

    private String lotNumber;
    private String administeredBy;
}