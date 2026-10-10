package com.autoscientist.dto.report;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {
    private Long id;
    private Long datasetId;
    private String datasetName;
    private Long researchQuestionId;
    private String title;
    private String summary;
    private String markdownContent;
    private String pdfDownloadUrl;
    private LocalDateTime createdAt;
}
