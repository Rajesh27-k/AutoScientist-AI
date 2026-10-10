package com.autoscientist.dto.dashboard;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEventDto {
    private String eventType; // DATASET_UPLOAD, ASSOCIATION_DISCOVERY, QUESTION_ASKED, MODEL_TRAINED, FINDING_PRODUCED, REPORT_EXPORTED
    private String title;
    private String description;
    private Long datasetId;
    private String datasetName;
    private LocalDateTime timestamp;
}
