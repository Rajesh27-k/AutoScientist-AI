package com.autoscientist.dto.research;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String predictorVariable;

    private String targetVariable;
}
