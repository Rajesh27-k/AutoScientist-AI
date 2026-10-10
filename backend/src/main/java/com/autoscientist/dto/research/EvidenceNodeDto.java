package com.autoscientist.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceNodeDto {
    private Long id;
    private String nodeType;
    private Long referenceId;
    private String description;
    private String dataPayloadJson;
    private LocalDateTime createdAt;
}
