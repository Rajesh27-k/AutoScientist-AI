package com.autoscientist.dto.ml;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatIfResponseDto {
    private String targetColumn;
    private String problemType;
    private Double predictedValue;
    private String predictedClass;
    private Double confidence;
    private Double baselineMean;
    private Double deltaFromBaseline;
    private List<String> keyInfluences;
}
