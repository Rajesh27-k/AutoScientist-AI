package com.autoscientist.dto.drift;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnDriftDto {
    private String columnName;
    private String dataType;
    private double psiScore;
    private String driftStatus; // NO_DRIFT, MODERATE_DRIFT, SIGNIFICANT_DRIFT
    private double pValue;
    private Double referenceMean;
    private Double targetMean;
    private Double meanShiftPct;
    private Map<String, Double> referenceDistribution;
    private Map<String, Double> targetDistribution;
    private String interpretation;
}
