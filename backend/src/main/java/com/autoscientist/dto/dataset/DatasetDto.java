package com.autoscientist.dto.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetDto {
    private Long id;
    private String name;
    private String originalFilename;
    private Long fileSize;
    private Integer rowCount;
    private Integer columnCount;
    private String datasetType;
    private String description;
    private Double qualityScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
