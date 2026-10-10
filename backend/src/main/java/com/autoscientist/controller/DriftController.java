package com.autoscientist.controller;

import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.dto.drift.DatasetDriftReportDto;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.DataDriftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drift")
@RequiredArgsConstructor
public class DriftController {

    private final DataDriftService dataDriftService;
    private final AuthService authService;

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<DatasetDriftReportDto>> compareDatasets(
            @RequestParam("referenceId") Long referenceId,
            @RequestParam("targetId") Long targetId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUserEntity(userDetails.getUsername());
        DatasetDriftReportDto report = dataDriftService.compareDatasets(referenceId, targetId, user);
        return ResponseEntity.ok(ApiResponse.success("Drift analysis completed successfully.", report));
    }
}
