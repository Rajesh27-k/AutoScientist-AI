package com.autoscientist.service;

import com.autoscientist.dto.research.FindingDto;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Finding;
import com.autoscientist.entity.User;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.exception.UnauthorizedException;
import com.autoscientist.repository.FindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindingService {

    private final FindingRepository findingRepository;
    private final DatasetService datasetService;
    private final ResearchService researchService;

    @Transactional(readOnly = true)
    public List<FindingDto> getDatasetFindings(Long datasetId, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);
        return findingRepository.findByDatasetOrderByCreatedAtDesc(dataset)
                .stream()
                .map(researchService::mapToFindingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FindingDto getFinding(Long id, User user) {
        Finding finding = findingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finding not found with id: " + id));

        if (!finding.getDataset().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to finding.");
        }

        return researchService.mapToFindingDto(finding);
    }
}
