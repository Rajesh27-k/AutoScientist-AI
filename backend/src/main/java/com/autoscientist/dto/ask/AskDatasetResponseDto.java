package com.autoscientist.dto.ask;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskDatasetResponseDto {
    private String query;
    private String detectedIntent;
    private List<String> relevantColumns;
    private String answerMarkdown;
    private Map<String, Object> empiricalEvidence;
    private double confidence;
}
