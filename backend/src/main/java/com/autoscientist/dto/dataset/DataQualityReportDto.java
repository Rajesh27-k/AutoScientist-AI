package com.autoscientist.dto.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityReportDto {
    private Long id;
    private Double overallScore;
    private Double missingValueScore;
    private Double duplicateScore;
    private Double outlierScore;
    private Double consistencyScore;
    private Integer totalIssues;
    private List<DataQualityIssueDto> issues;
}
