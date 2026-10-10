package com.autoscientist.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HypothesisDto {
    private Long id;
    private Long researchQuestionId;
    private String nullHypothesis;
    private String altHypothesis;
    private String rationale;
    private String suggestedTest;
    private String var1;
    private String var2;
    private Double confidence;
    private Integer rankOrder;
    private List<StatisticalTestDto> statisticalTests;
    private LocalDateTime createdAt;
}
