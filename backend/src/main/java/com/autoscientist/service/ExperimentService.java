package com.autoscientist.service;

import com.autoscientist.dto.research.ExperimentDto;
import com.autoscientist.dto.research.ExperimentVersionDto;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.Experiment;
import com.autoscientist.entity.ExperimentVersion;
import com.autoscientist.entity.User;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.exception.UnauthorizedException;
import com.autoscientist.repository.ExperimentRepository;
import com.autoscientist.repository.ExperimentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentVersionRepository experimentVersionRepository;
    private final DatasetService datasetService;

    @Transactional
    public ExperimentDto createExperiment(Long datasetId, String name, String description,
                                          String targetColumn, String problemType, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);

        Experiment exp = Experiment.builder()
                .dataset(dataset)
                .user(user)
                .name(name != null ? name.trim() : "Experiment - " + targetColumn)
                .description(description)
                .targetColumn(targetColumn)
                .problemType(problemType != null ? problemType : "REGRESSION")
                .status("INITIALIZED")
                .build();

        exp = experimentRepository.save(exp);
        return mapToDto(exp);
    }

    @Transactional
    public ExperimentVersionDto recordVersion(Long experimentId, String preprocessingJson,
                                              String featureSelectionJson, String modelsSummaryJson,
                                              String bestModelName, Double bestMetricValue, User user) {
        Experiment exp = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found: " + experimentId));

        if (!exp.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to experiment.");
        }

        int nextVersion = exp.getVersions().size() + 1;

        ExperimentVersion ver = ExperimentVersion.builder()
                .experiment(exp)
                .versionNumber(nextVersion)
                .preprocessingJson(preprocessingJson)
                .featureSelectionJson(featureSelectionJson)
                .modelsSummaryJson(modelsSummaryJson)
                .bestModelName(bestModelName)
                .bestMetricValue(bestMetricValue)
                .build();

        ver = experimentVersionRepository.save(ver);
        exp.setStatus("COMPLETED");
        experimentRepository.save(exp);

        return mapToVersionDto(ver);
    }

    @Transactional(readOnly = true)
    public List<ExperimentDto> listUserExperiments(User user) {
        return experimentRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExperimentDto getExperiment(Long id, User user) {
        Experiment exp = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found: " + id));

        if (!exp.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to experiment.");
        }

        return mapToDto(exp);
    }

    public ExperimentDto mapToDto(Experiment exp) {
        List<ExperimentVersionDto> verDtos = exp.getVersions().stream()
                .map(this::mapToVersionDto)
                .collect(Collectors.toList());

        return ExperimentDto.builder()
                .id(exp.getId())
                .datasetId(exp.getDataset().getId())
                .researchQuestionId(exp.getResearchQuestion() != null ? exp.getResearchQuestion().getId() : null)
                .name(exp.getName())
                .description(exp.getDescription())
                .targetColumn(exp.getTargetColumn())
                .problemType(exp.getProblemType())
                .status(exp.getStatus())
                .versions(verDtos)
                .createdAt(exp.getCreatedAt())
                .build();
    }

    public ExperimentVersionDto mapToVersionDto(ExperimentVersion ver) {
        return ExperimentVersionDto.builder()
                .id(ver.getId())
                .experimentId(ver.getExperiment().getId())
                .versionNumber(ver.getVersionNumber())
                .preprocessingJson(ver.getPreprocessingJson())
                .featureSelectionJson(ver.getFeatureSelectionJson())
                .modelsSummaryJson(ver.getModelsSummaryJson())
                .bestModelName(ver.getBestModelName())
                .bestMetricValue(ver.getBestMetricValue())
                .createdAt(ver.getCreatedAt())
                .build();
    }
}
