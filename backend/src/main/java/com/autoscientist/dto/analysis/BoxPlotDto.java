package com.autoscientist.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxPlotDto {
    private String variable;
    private Double min;
    private Double q1;
    private Double median;
    private Double q3;
    private Double max;
    private Double iqr;
    private List<Double> outliers;
}
