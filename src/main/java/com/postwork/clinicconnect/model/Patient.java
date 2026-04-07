package com.postwork.clinicconnect.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    private String patientId;
    private String firstName;
    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String guardianName;
    private String guardianPhone;
    private String insuranceId;
    private List<VaccinationRecord> vaccinationHistory;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}