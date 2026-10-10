package com.autoscientist.dto.dataset;

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
public class DatasetDetailDto {
    private Long id;
    private String name;
    private String originalFilename;
    private Long fileSize;
    private Integer rowCount;
    private Integer columnCount;
    private String datasetType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DatasetColumnDto> columns;
    private DataQualityReportDto qualityReport;
}
