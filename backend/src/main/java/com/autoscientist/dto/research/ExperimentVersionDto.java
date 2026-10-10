package com.autoscientist.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentVersionDto {
    private Long id;
    private Long experimentId;
    private Integer versionNumber;
    private String preprocessingJson;
    private String featureSelectionJson;
    private String modelsSummaryJson;
    private String bestModelName;
    private Double bestMetricValue;
    private LocalDateTime createdAt;
}
