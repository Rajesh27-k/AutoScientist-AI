package com.autoscientist.controller;

import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.dto.ml.AutoMlResponseDto;
import com.autoscientist.dto.ml.AutoMlTrainRequest;
import com.autoscientist.dto.ml.WhatIfRequestDto;
import com.autoscientist.dto.ml.WhatIfResponseDto;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.AutoMlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datasets/{id}/automl")
@RequiredArgsConstructor
public class AutoMlController {

    private final AutoMlService autoMlService;
    private final AuthService authService;

    @PostMapping("/train")
    public ResponseEntity<ApiResponse<AutoMlResponseDto>> trainAutoMl(
            @PathVariable Long id,
            @RequestBody AutoMlTrainRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUserEntity(userDetails.getUsername());
        AutoMlResponseDto response = autoMlService.trainAndBenchmark(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("AutoML benchmark completed successfully.", response));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AutoMlResponseDto>> getLatestAutoMl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUserEntity(userDetails.getUsername());
        AutoMlResponseDto response = autoMlService.getLatestBenchmark(id, user);
        return ResponseEntity.ok(ApiResponse.success("Latest AutoML benchmark retrieved.", response));
    }

    @PostMapping("/whatif")
    public ResponseEntity<ApiResponse<WhatIfResponseDto>> simulateWhatIf(
            @PathVariable Long id,
            @RequestBody WhatIfRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUserEntity(userDetails.getUsername());
        WhatIfResponseDto response = autoMlService.predictWhatIf(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("What-If simulation calculated.", response));
    }
}
