package com.autoscientist.controller;

import com.autoscientist.dto.analysis.BoxPlotDto;
import com.autoscientist.dto.analysis.CorrelationMatrixDto;
import com.autoscientist.dto.analysis.GroupedSummaryDto;
import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.EdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets/{datasetId}/eda")
@RequiredArgsConstructor
public class EdaController {

    private final EdaService edaService;
    private final AuthService authService;

    @GetMapping("/correlations")
    public ResponseEntity<ApiResponse<CorrelationMatrixDto>> getCorrelationMatrix(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        CorrelationMatrixDto matrix = edaService.getCorrelationMatrix(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Correlation matrix computed successfully", matrix));
    }

    @GetMapping("/boxplots")
    public ResponseEntity<ApiResponse<List<BoxPlotDto>>> getBoxPlots(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<BoxPlotDto> boxPlots = edaService.getBoxPlots(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Box plot distributions computed successfully", boxPlots));
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<List<GroupedSummaryDto>>> getGroupedSummary(
            @PathVariable("datasetId") Long datasetId,
            @RequestParam("numericVar") String numericVar,
            @RequestParam("categoricalVar") String categoricalVar,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<GroupedSummaryDto> summaries = edaService.getGroupedSummary(datasetId, numericVar, categoricalVar, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Grouped summary computed successfully", summaries));
    }
}
