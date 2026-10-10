package com.autoscientist.controller;

import com.autoscientist.dto.analysis.AssociationDto;
import com.autoscientist.dto.common.ApiResponse;
import com.autoscientist.entity.User;
import com.autoscientist.service.AssociationService;
import com.autoscientist.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets/{datasetId}/associations")
@RequiredArgsConstructor
public class AssociationController {

    private final AssociationService associationService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssociationDto>>> getAssociations(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<AssociationDto> associations = associationService.getAssociations(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Associations retrieved successfully", associations));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<List<AssociationDto>>> refreshAssociations(
            @PathVariable("datasetId") Long datasetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = authService.getCurrentUserEntity(userDetails.getUsername());
        List<AssociationDto> associations = associationService.computeAndSaveAssociations(datasetId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Associations freshly computed and updated", associations));
    }
}
