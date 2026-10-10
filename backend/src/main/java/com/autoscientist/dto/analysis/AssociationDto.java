package com.autoscientist.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssociationDto {
    private Long id;
    private Long datasetId;
    private String var1;
    private String var2;
    private String var1Type;
    private String var2Type;
    private String testType;
    private Double statistic;
    private Double pValue;
    private Double effectSize;
    private Integer sampleSize;
    private Double degreesOfFreedom;
    private Boolean isSignificant;
    private String interpretation;
    private Double rankScore;
    private LocalDateTime createdAt;
}
