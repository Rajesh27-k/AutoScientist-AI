package com.autoscientist.dto.ask;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskDatasetRequestDto {
    private String query;
}
