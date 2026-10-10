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
public class ResearchQuestionDto {
    private Long id;
    private Long datasetId;
    private String questionText;
    private String predictorVariable;
    private String targetVariable;
    private String questionType;
    private String suggestedTest;
    private String status;
    private List<HypothesisDto> hypotheses;
    private LocalDateTime createdAt;
}
