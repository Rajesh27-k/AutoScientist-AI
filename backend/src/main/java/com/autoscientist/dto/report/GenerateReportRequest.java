package com.autoscientist.dto.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateReportRequest {
    private Long researchQuestionId;
    private String customTitle;
}
