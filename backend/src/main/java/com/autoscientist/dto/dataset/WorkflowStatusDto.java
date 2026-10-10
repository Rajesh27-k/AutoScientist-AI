package com.autoscientist.dto.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusDto {

    private Long datasetId;
    private String datasetName;
    private int rowCount;
    private int columnCount;
    private Map<String, StageStatusDto> stages;
    private String currentRecommendedStage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageStatusDto {
        private String stageKey;
        private String label;
        private boolean completed;
        private int count;
        private String summary;
        private boolean prerequisitesMet;
        private String prerequisiteMessage;
        private String prerequisiteStage;
    }
}
