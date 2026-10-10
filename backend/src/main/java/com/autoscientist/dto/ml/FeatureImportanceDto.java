package com.autoscientist.dto.ml;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureImportanceDto {
    private String featureName;
    private double importanceScore;
    private double relativePercentage;
    private int rank;
    private String direction; // POSITIVE, NEGATIVE, or NON_LINEAR
    private String interpretation;
}
