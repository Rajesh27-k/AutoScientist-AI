package com.autoscientist.service;

import com.autoscientist.analysis.fairness.FairnessEngine;
import com.autoscientist.analysis.feature.FeatureEngineeringEngine;
import com.autoscientist.analysis.feature.FeatureEngineeringEngine.DatasetSplit;
import com.autoscientist.analysis.ml.AutoMlEngine;
import com.autoscientist.analysis.ml.AutoMlEngine.ModelBenchmarkResult;
import com.autoscientist.analysis.ml.AutoMlEngine.PredictiveModel;
import com.autoscientist.analysis.xai.ExplainabilityEngine;
import com.autoscientist.dto.ml.*;
import com.autoscientist.entity.*;
import com.autoscientist.exception.BadRequestException;
import com.autoscientist.exception.ResourceNotFoundException;
import com.autoscientist.repository.ExperimentRepository;
import com.autoscientist.repository.ExperimentVersionRepository;
import com.autoscientist.repository.ModelRunRepository;
import com.autoscientist.util.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMlService {

    private final DatasetService datasetService;
    private final FileStorageService fileStorageService;
    private final FeatureEngineeringEngine featureEngineeringEngine;
    private final AutoMlEngine autoMlEngine;
    private final ExplainabilityEngine explainabilityEngine;
    private final FairnessEngine fairnessEngine;
    private final ExperimentRepository experimentRepository;
    private final ExperimentVersionRepository experimentVersionRepository;
    private final ModelRunRepository modelRunRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache active trained models for instant What-If simulator predictions
    private static final Map<Long, CachedModelContext> MODEL_CACHE = new ConcurrentHashMap<>();

    private record CachedModelContext(
            PredictiveModel model,
            DatasetSplit split,
            double baselineMean,
            List<FeatureImportanceDto> featureImportances
    ) {}

    @Transactional
    public AutoMlResponseDto trainAndBenchmark(Long datasetId, AutoMlTrainRequest request, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);

        String targetCol = request.getTargetColumn();
        if (targetCol == null || targetCol.isBlank()) {
            throw new BadRequestException("Target column must be specified for AutoML.");
        }

        // Validate target column exists in dataset
        boolean colExists = dataset.getColumns().stream()
                .anyMatch(c -> c.getColumnName().equalsIgnoreCase(targetCol));
        if (!colExists) {
            throw new BadRequestException("Column '" + targetCol + "' not found in dataset.");
        }

        DatasetSplit split;
        try (InputStream is = fileStorageService.getInputStream(dataset.getStoredFilename())) {
            split = featureEngineeringEngine.prepareDataset(is, dataset.getColumns(), targetCol, request.getProblemType());
        } catch (Exception e) {
            log.error("Feature engineering failed for dataset {}", datasetId, e);
            throw new BadRequestException("Feature preprocessing failed: " + e.getMessage());
        }

        // Run AutoML Benchmark
        List<ModelBenchmarkResult> benchmarkResults = autoMlEngine.runAutoMlBenchmark(split);
        if (benchmarkResults.isEmpty()) {
            throw new BadRequestException("No suitable models could be trained on this dataset.");
        }

        ModelBenchmarkResult bestModel = benchmarkResults.get(0);

        // Compute Explainability (Feature Importance)
        List<FeatureImportanceDto> featureImportances = explainabilityEngine.computeFeatureImportance(split, bestModel);

        // Compute Fairness
        FairnessDto fairnessDto = fairnessEngine.evaluateFairness(
                dataset.getColumns(), split, bestModel, request.getSensitiveColumn());

        // Baseline mean for target
        double baselineMean = 0;
        for (double v : split.getYTrain()) baselineMean += v;
        if (split.getYTrain().length > 0) baselineMean /= split.getYTrain().length;

        // Persist Experiment & ExperimentVersion
        String expName = request.getExperimentName() != null && !request.getExperimentName().isBlank()
                ? request.getExperimentName()
                : "AutoML - " + targetCol;

        Experiment experiment = Experiment.builder()
                .dataset(dataset)
                .user(user)
                .name(expName)
                .description(request.getDescription())
                .targetColumn(targetCol)
                .problemType(split.getProblemType())
                .status("COMPLETED")
                .build();
        experiment = experimentRepository.save(experiment);

        Double bestScore = "REGRESSION".equals(split.getProblemType())
                ? bestModel.getValMetrics().getOrDefault("r2", 0.0)
                : bestModel.getValMetrics().getOrDefault("f1", 0.0);

        String prepJson = safeJson(split.getPreprocessingConfig());
        String summaryJson = safeJson(buildSummaryMap(benchmarkResults));

        ExperimentVersion version = ExperimentVersion.builder()
                .experiment(experiment)
                .versionNumber(1)
                .preprocessingJson(prepJson)
                .modelsSummaryJson(summaryJson)
                .bestModelName(bestModel.getModelName())
                .bestMetricValue(bestScore)
                .build();
        version = experimentVersionRepository.save(version);

        // Persist Model Runs
        List<ModelRunDto> runDtos = new ArrayList<>();
        for (ModelBenchmarkResult res : benchmarkResults) {
            ModelRun run = ModelRun.builder()
                    .experimentVersion(version)
                    .modelName(res.getModelName())
                    .algorithm(res.getAlgorithm())
                    .hyperparametersJson(safeJson(res.getHyperparameters()))
                    .trainMetricsJson(safeJson(res.getTrainMetrics()))
                    .valMetricsJson(safeJson(res.getValMetrics()))
                    .testMetricsJson(safeJson(res.getTestMetrics()))
                    .isBestModel(res.isBest())
                    .fairnessMetricsJson(res.isBest() ? safeJson(fairnessDto) : null)
                    .build();
            run = modelRunRepository.save(run);

            runDtos.add(ModelRunDto.builder()
                    .id(run.getId())
                    .modelName(res.getModelName())
                    .algorithm(res.getAlgorithm())
                    .hyperparameters(res.getHyperparameters())
                    .trainMetrics(res.getTrainMetrics())
                    .valMetrics(res.getValMetrics())
                    .testMetrics(res.getTestMetrics())
                    .isBestModel(res.isBest())
                    .fairnessMetrics(res.isBest() ? Map.of(
                            "disparateImpactStatus", fairnessDto.getDisparateImpactStatus(),
                            "demographicParityRatio", fairnessDto.getDemographicParityRatio()
                    ) : null)
                    .build());
        }

        // Cache the best model for What-If simulator
        MODEL_CACHE.put(datasetId, new CachedModelContext(
                bestModel.getTrainedModel(),
                split,
                baselineMean,
                featureImportances
        ));

        // Formulate Scientific Summary
        String metricName = "REGRESSION".equals(split.getProblemType()) ? "R²" : "Macro-F1";
        String topDriver = featureImportances.isEmpty() ? "None" : featureImportances.get(0).getFeatureName();
        String summary = String.format("AutoML evaluated %d candidate architectures on target '%s'. Best performer is %s with validation %s of %.3f. The top predictive driver is '%s'. Fairness verification: %s.",
                benchmarkResults.size(), targetCol, bestModel.getModelName(), metricName, bestScore, topDriver, fairnessDto.getDisparateImpactStatus());

        return AutoMlResponseDto.builder()
                .experimentId(experiment.getId())
                .experimentVersionId(version.getId())
                .experimentName(expName)
                .problemType(split.getProblemType())
                .targetColumn(targetCol)
                .models(runDtos)
                .bestModel(runDtos.get(0))
                .featureImportances(featureImportances)
                .fairness(fairnessDto)
                .scientificSummary(summary)
                .build();
    }

    public WhatIfResponseDto predictWhatIf(Long datasetId, WhatIfRequestDto req, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);

        CachedModelContext context = MODEL_CACHE.get(datasetId);
        if (context == null) {
            // Check if there is an existing experiment to reload
            List<Experiment> exps = experimentRepository.findByUserOrderByCreatedAtDesc(user);
            Experiment matchingExp = exps.stream()
                    .filter(e -> e.getDataset().getId().equals(datasetId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("No trained model exists for dataset " + datasetId + ". Please run AutoML training first."));

            AutoMlTrainRequest autoTrainReq = AutoMlTrainRequest.builder()
                    .targetColumn(matchingExp.getTargetColumn())
                    .problemType(matchingExp.getProblemType())
                    .build();
            trainAndBenchmark(datasetId, autoTrainReq, user);
            context = MODEL_CACHE.get(datasetId);
        }

        DatasetSplit split = context.split();
        PredictiveModel model = context.model();
        List<String> featureNames = split.getFeatureNames();
        Map<String, Object> inputVals = req.getFeatureValues() != null ? req.getFeatureValues() : Collections.emptyMap();

        // Construct input feature vector
        double[] x = new double[featureNames.size()];
        for (int i = 0; i < featureNames.size(); i++) {
            String fName = featureNames.get(i);
            Object raw = inputVals.get(fName);

            // Check if input was provided directly or needs one-hot mapping
            if (raw != null) {
                try {
                    x[i] = Double.parseDouble(raw.toString());
                } catch (NumberFormatException e) {
                    x[i] = 0.0;
                }
            } else {
                // Check if this feature is a one-hot category: prefix_category
                boolean matchedOneHot = false;
                for (Map.Entry<String, Object> entry : inputVals.entrySet()) {
                    String prefix = entry.getKey() + "_";
                    if (fName.startsWith(prefix)) {
                        String catValue = fName.substring(prefix.length());
                        if (catValue.equalsIgnoreCase(String.valueOf(entry.getValue()))) {
                            x[i] = 1.0;
                            matchedOneHot = true;
                            break;
                        }
                    }
                }
                if (!matchedOneHot) {
                    x[i] = 0.0; // Default median/zero after standard scaling
                }
            }
        }

        double pred = model.predictSingle(x);
        double delta = pred - context.baselineMean();

        String predClass = null;
        Double conf = 1.0;
        if ("CLASSIFICATION".equals(split.getProblemType())) {
            int classIdx = (int) Math.round(pred);
            predClass = split.getReverseClassMap() != null ? split.getReverseClassMap().get(classIdx) : String.valueOf(classIdx);
            if (predClass == null) predClass = "Class " + classIdx;
        }

        List<String> influences = context.featureImportances().stream()
                .limit(3)
                .map(f -> f.getFeatureName() + " (" + f.getRelativePercentage() + "%)")
                .collect(Collectors.toList());

        return WhatIfResponseDto.builder()
                .targetColumn(split.getTargetColumn())
                .problemType(split.getProblemType())
                .predictedValue(Math.round(pred * 1000.0) / 1000.0)
                .predictedClass(predClass)
                .confidence(conf)
                .baselineMean(Math.round(context.baselineMean() * 1000.0) / 1000.0)
                .deltaFromBaseline(Math.round(delta * 1000.0) / 1000.0)
                .keyInfluences(influences)
                .build();
    }

    @Transactional(readOnly = true)
    public AutoMlResponseDto getLatestBenchmark(Long datasetId, User user) {
        Dataset dataset = datasetService.getDatasetEntityWithOwnership(datasetId, user);
        CachedModelContext context = MODEL_CACHE.get(datasetId);

        List<Experiment> exps = experimentRepository.findByDatasetAndUserOrderByCreatedAtDesc(dataset, user);
        if (exps.isEmpty()) {
            return null;
        }

        Experiment experiment = exps.get(0);
        List<ExperimentVersion> versions = experimentVersionRepository.findByExperimentOrderByVersionNumberDesc(experiment);
        if (versions.isEmpty()) {
            return null;
        }

        ExperimentVersion version = versions.get(0);
        List<ModelRun> runs = modelRunRepository.findByExperimentVersion(version);

        List<ModelRunDto> runDtos = new ArrayList<>();
        ModelRunDto bestModelDto = null;

        for (ModelRun mr : runs) {
            Map<String, Double> valM = parseMetrics(mr.getValMetricsJson());
            Map<String, Double> testM = parseMetrics(mr.getTestMetricsJson());
            Map<String, Double> trainM = parseMetrics(mr.getTrainMetricsJson());

            ModelRunDto dto = ModelRunDto.builder()
                    .id(mr.getId())
                    .modelName(mr.getModelName())
                    .algorithm(mr.getAlgorithm())
                    .hyperparameters(parseMap(mr.getHyperparametersJson()))
                    .trainMetrics(trainM)
                    .valMetrics(valM)
                    .testMetrics(testM)
                    .isBestModel(Boolean.TRUE.equals(mr.getIsBestModel()))
                    .fairnessMetrics(parseMap(mr.getFairnessMetricsJson()))
                    .build();

            runDtos.add(dto);
            if (Boolean.TRUE.equals(mr.getIsBestModel()) && bestModelDto == null) {
                bestModelDto = dto;
            }
        }

        if (bestModelDto == null && !runDtos.isEmpty()) {
            bestModelDto = runDtos.get(0);
        }

        List<FeatureImportanceDto> featList = context != null ? context.featureImportances() : Collections.emptyList();

        FairnessDto fairnessDto = null;
        if (bestModelDto != null && bestModelDto.getFairnessMetrics() != null) {
            Map<String, Object> fm = bestModelDto.getFairnessMetrics();
            double ratio = 1.0;
            Object ratioObj = fm.get("demographicParityRatio");
            if (ratioObj instanceof Number num) {
                ratio = num.doubleValue();
            }
            fairnessDto = FairnessDto.builder()
                    .sensitiveColumn("Auto-Detected")
                    .demographicParityRatio(ratio)
                    .disparateImpactStatus(ratio >= 0.8 ? "PASS" : "FLAGGED")
                    .recommendation("Fairness verified against 80% four-fifths standard.")
                    .build();
        }

        return AutoMlResponseDto.builder()
                .experimentId(experiment.getId())
                .experimentVersionId(version.getId())
                .experimentName(experiment.getName())
                .problemType(experiment.getProblemType())
                .targetColumn(experiment.getTargetColumn())
                .models(runDtos)
                .bestModel(bestModelDto)
                .featureImportances(featList)
                .fairness(fairnessDto)
                .scientificSummary("AutoML benchmark history retrieved for target variable '" + experiment.getTargetColumn() + "'.")
                .build();
    }

    private Map<String, Double> parseMetrics(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> buildSummaryMap(List<ModelBenchmarkResult> results) {
        Map<String, Object> map = new HashMap<>();
        for (ModelBenchmarkResult r : results) {
            map.put(r.getModelName(), Map.of(
                    "algorithm", r.getAlgorithm(),
                    "valMetrics", r.getValMetrics(),
                    "isBest", r.isBest()
            ));
        }
        return map;
    }
}
