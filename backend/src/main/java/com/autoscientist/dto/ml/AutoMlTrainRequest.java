package com.autoscientist.dto.ml;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoMlTrainRequest {
    private String targetColumn;
    private String problemType; // REGRESSION, CLASSIFICATION, or AUTO
    private String sensitiveColumn; // optional for fairness analysis
    private String experimentName;
    private String description;
}
