package com.autoscientist.service;

import com.autoscientist.analysis.core.AskDatasetEngine;
import com.autoscientist.dto.ask.AskDatasetRequestDto;
import com.autoscientist.dto.ask.AskDatasetResponseDto;
import com.autoscientist.entity.*;
import com.autoscientist.repository.AssociationRepository;
import com.autoscientist.repository.DataQualityReportRepository;
import com.autoscientist.repository.ExperimentRepository;
import com.autoscientist.repository.ModelRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AskDatasetService {

    private final DatasetService datasetService;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final AssociationRepository associationRepository;
    private final ExperimentRepository experimentRepository;
    private final ModelRunRepository modelRunRepository;
    private final AskDatasetEngine askDatasetEngine;

    @Transactional(readOnly = true)
    public AskDatasetResponseDto askQuestion(Long datasetId, AskDatasetRequestDto request, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);
        DataQualityReport qualityReport = dataQualityReportRepository.findByDataset(dataset).orElse(null);
        List<Association> associations = associationRepository.findByDatasetOrderByRankScoreDesc(dataset);

        // Fetch latest model runs if any
        List<Experiment> experiments = experimentRepository.findByUserOrderByCreatedAtDesc(user);
        List<ModelRun> modelRuns = experiments.stream()
                .filter(e -> e.getDataset().getId().equals(datasetId))
                .findFirst()
                .map(e -> e.getVersions().isEmpty() ? Collections.<ModelRun>emptyList()
                        : modelRunRepository.findByExperimentVersion(e.getVersions().get(e.getVersions().size() - 1)))
                .orElse(Collections.emptyList());

        return askDatasetEngine.answerQuery(request.getQuery(), dataset, qualityReport, associations, modelRuns);
    }
}
