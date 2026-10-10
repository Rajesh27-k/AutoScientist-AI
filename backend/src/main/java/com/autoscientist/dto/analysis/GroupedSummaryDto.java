package com.autoscientist.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedSummaryDto {
    private String groupName;
    private Integer count;
    private Double mean;
    private Double stdDev;
    private Double median;
    private Double min;
    private Double max;
}
