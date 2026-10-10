package com.autoscientist.controller;

import com.autoscientist.dto.ask.AskDatasetRequestDto;
import com.autoscientist.dto.ask.AskDatasetResponseDto;
import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.entity.User;
import com.autoscientist.service.AskDatasetService;
import com.autoscientist.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datasets/{id}/ask")
@RequiredArgsConstructor
public class AskDatasetController {

    private final AskDatasetService askDatasetService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<AskDatasetResponseDto>> askQuestion(
            @PathVariable Long id,
            @RequestBody AskDatasetRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUserEntity(userDetails.getUsername());
        AskDatasetResponseDto response = askDatasetService.askQuestion(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("Question answered using empirical dataset evidence.", response));
    }
}
