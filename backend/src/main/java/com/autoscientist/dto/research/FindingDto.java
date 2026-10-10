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
public class FindingDto {
    private Long id;
    private Long datasetId;
    private Long researchQuestionId;
    private Long hypothesisId;
    private Long statisticalTestId;
    private String title;
    private String summary;
    private String evidenceText;
    private String confidenceLevel;
    private String causalityDisclaimer;
    private List<EvidenceNodeDto> evidenceNodes;
    private LocalDateTime createdAt;
}
