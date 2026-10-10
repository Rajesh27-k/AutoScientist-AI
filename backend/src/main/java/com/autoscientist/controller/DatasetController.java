package com.autoscientist.controller;

import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.dto.dataset.DataQualityReportDto;
import com.autoscientist.dto.dataset.DatasetDetailDto;
import com.autoscientist.dto.dataset.DatasetDto;
import com.autoscientist.dto.dataset.WorkflowStatusDto;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.DatasetService;
import com.autoscientist.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final AuthService authService;
    private final WorkflowService workflowService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DatasetDetailDto>> uploadDataset(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        DatasetDetailDto result = datasetService.uploadDataset(file, name, description, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dataset uploaded and profiled successfully", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DatasetDto>>> listDatasets(
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<DatasetDto> datasets = datasetService.listUserDatasets(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Datasets retrieved successfully", datasets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DatasetDetailDto>> getDatasetDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        DatasetDetailDto dataset = datasetService.getDatasetDetail(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Dataset details retrieved successfully", dataset));
    }

    @GetMapping("/{id}/quality")
    public ResponseEntity<ApiResponse<DataQualityReportDto>> getDatasetQuality(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        DatasetDetailDto dataset = datasetService.getDatasetDetail(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Quality report retrieved successfully", dataset.getQualityReport()));
    }

    @GetMapping("/{id}/workflow-status")
    public ResponseEntity<ApiResponse<WorkflowStatusDto>> getWorkflowStatus(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        WorkflowStatusDto workflow = workflowService.getWorkflowStatus(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Workflow status retrieved successfully", workflow));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDataset(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        datasetService.deleteDataset(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Dataset deleted successfully", null));
    }
}
