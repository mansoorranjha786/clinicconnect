package com.postwork.clinicconnect.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicActionReport {

    private String reportId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    private String clinicName;
    private int totalPatientsAnalyzed;
    private int totalPatientsWithGaps;
    private int totalGapsIdentified;
    private Map<String, Long> gapsByUrgencyLevel;
    private List<ImmunizationGap> actionItems;
    private String dataSource;
    private boolean usedFallbackData;
}