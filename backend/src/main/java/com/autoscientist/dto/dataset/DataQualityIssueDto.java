package com.autoscientist.dto.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssueDto {
    private Long id;
    private String columnName;
    private String issueType;
    private String severity;
    private String description;
    private String recommendation;
    private Integer affectedRows;
}
