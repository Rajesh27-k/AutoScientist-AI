package com.autoscientist.dto.drift;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetDriftReportDto {
    private Long referenceDatasetId;
    private String referenceDatasetName;
    private Long targetDatasetId;
    private String targetDatasetName;
    private double overallDriftIndex;
    private String overallStatus; // STABLE, LOW_RISK, ACTION_REQUIRED
    private int totalFeaturesAnalyzed;
    private int driftedFeaturesCount;
    private List<ColumnDriftDto> columnDrifts;
    private String summaryRecommendation;
}
