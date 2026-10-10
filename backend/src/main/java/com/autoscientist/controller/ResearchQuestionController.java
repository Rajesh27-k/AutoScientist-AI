package com.autoscientist.controller;

import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.dto.research.FindingDto;
import com.autoscientist.dto.research.ResearchQuestionDto;
import com.autoscientist.dto.research.ResearchQuestionRequest;
import com.autoscientist.entity.User;
import com.autoscientist.service.AuthService;
import com.autoscientist.service.ResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResearchQuestionController {

    private final ResearchService researchService;
    private final AuthService authService;

    @PostMapping("/datasets/{datasetId}/research-questions")
    public ResponseEntity<ApiResponse<ResearchQuestionDto>> createResearchQuestion(
            @PathVariable("datasetId") Long datasetId,
            @Valid @RequestBody ResearchQuestionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        ResearchQuestionDto created = researchService.createResearchQuestion(datasetId, request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Research question analyzed and hypotheses formulated successfully", created));
    }

    @GetMapping("/datasets/{datasetId}/research-questions")
    public ResponseEntity<ApiResponse<List<ResearchQuestionDto>>> getDatasetResearchQuestions(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<ResearchQuestionDto> list = researchService.getDatasetResearchQuestions(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Research questions retrieved successfully", list));
    }

    @GetMapping("/research-questions/{id}")
    public ResponseEntity<ApiResponse<ResearchQuestionDto>> getResearchQuestion(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        ResearchQuestionDto rq = researchService.getResearchQuestion(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Research question retrieved successfully", rq));
    }

    @PostMapping("/research-questions/{rqId}/hypotheses/{hypothesisId}/execute")
    public ResponseEntity<ApiResponse<FindingDto>> executeHypothesisTest(
            @PathVariable("rqId") Long rqId,
            @PathVariable("hypothesisId") Long hypothesisId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        FindingDto finding = researchService.executeHypothesisTest(rqId, hypothesisId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Hypothesis statistical test executed and finding generated", finding));
    }
}
