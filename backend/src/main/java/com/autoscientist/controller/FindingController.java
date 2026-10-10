package com.autoscientist.controller;

import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.dto.research.FindingDto;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.FindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FindingController {

    private final FindingService findingService;
    private final AuthService authService;

    @GetMapping("/datasets/{datasetId}/findings")
    public ResponseEntity<ApiResponse<List<FindingDto>>> getDatasetFindings(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<FindingDto> findings = findingService.getDatasetFindings(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Findings retrieved successfully", findings));
    }

    @GetMapping("/findings/{id}")
    public ResponseEntity<ApiResponse<FindingDto>> getFinding(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        FindingDto finding = findingService.getFinding(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Finding details retrieved successfully", finding));
    }
}
