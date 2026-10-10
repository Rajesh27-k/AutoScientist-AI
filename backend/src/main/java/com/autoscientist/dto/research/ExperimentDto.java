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
public class ExperimentDto {
    private Long id;
    private Long datasetId;
    private Long researchQuestionId;
    private String name;
    private String description;
    private String targetColumn;
    private String problemType;
    private String status;
    private List<ExperimentVersionDto> versions;
    private LocalDateTime createdAt;
}
