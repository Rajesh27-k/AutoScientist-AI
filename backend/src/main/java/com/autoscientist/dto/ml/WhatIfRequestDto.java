package com.autoscientist.dto.ml;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatIfRequestDto {
    private String targetColumn;
    private Map<String, Object> featureValues;
}
