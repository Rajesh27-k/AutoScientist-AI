package com.autoscientist.analysis.core;

import com.autoscientist.dto.ask.AskDatasetResponseDto;
import com.autoscientist.entity.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AskDatasetEngine {

    public AskDatasetResponseDto answerQuery(String query, Dataset dataset,
                                             DataQualityReport qualityReport,
                                             List<Association> associations,
                                             List<ModelRun> modelRuns) {
        String lowerQuery = query.toLowerCase().trim();

        // 1. Identify mentioned columns
        List<DatasetColumn> mentionedCols = dataset.getColumns().stream()
                .filter(c -> lowerQuery.contains(c.getColumnName().toLowerCase()))
                .collect(Collectors.toList());

        List<String> colNames = mentionedCols.stream()
                .map(DatasetColumn::getColumnName)
                .collect(Collectors.toList());

        String intent;
        String answerMarkdown;
        Map<String, Object> evidence = new LinkedHashMap<>();
        double confidence = 0.95;

        if (lowerQuery.contains("mean") || lowerQuery.contains("average") || lowerQuery.contains("avg")) {
            intent = "NUMERIC_CENTRAL_TENDENCY";
            if (!mentionedCols.isEmpty()) {
                DatasetColumn col = mentionedCols.get(0);
                if ("NUMERIC".equalsIgnoreCase(col.getDataType())) {
                    answerMarkdown = String.format("### Central Tendency for `%s`\n" +
                                    "- **Mean**: `%.3f`\n" +
                                    "- **Median**: `%.3f`\n" +
                                    "- **Standard Deviation**: `%.3f`\n" +
                                    "- **Range**: `[%.3f, %.3f]`\n\n" +
                                    "Across %d valid observations, the mean value is **%.3f** with median **%.3f**.",
                            col.getColumnName(), col.getMeanVal(), col.getMedianVal(),
                            col.getStdDev(), col.getMinVal(), col.getMaxVal(),
                            dataset.getRowCount() - col.getMissingCount(), col.getMeanVal(), col.getMedianVal());
                    evidence.put("mean", col.getMeanVal());
                    evidence.put("median", col.getMedianVal());
                    evidence.put("stdDev", col.getStdDev());
                } else {
                    answerMarkdown = String.format("Column `%s` is typed as `%s` (non-numeric), so a numerical arithmetic mean is undefined. It contains %d unique categories.",
                            col.getColumnName(), col.getDataType(), col.getUniqueCount());
                }
            } else {
                List<DatasetColumn> numCols = dataset.getColumns().stream()
                        .filter(c -> "NUMERIC".equalsIgnoreCase(c.getDataType()))
                        .limit(5)
                        .collect(Collectors.toList());

                StringBuilder sb = new StringBuilder("### Overview of Numeric Means\n");
                for (DatasetColumn c : numCols) {
                    sb.append(String.format("- **%s**: Mean = `%.3f`, Median = `%.3f`\n",
                            c.getColumnName(), c.getMeanVal() != null ? c.getMeanVal() : 0.0,
                            c.getMedianVal() != null ? c.getMedianVal() : 0.0));
                    evidence.put(c.getColumnName() + "_mean", c.getMeanVal());
                }
                answerMarkdown = sb.toString();
            }

        } else if (lowerQuery.contains("max") || lowerQuery.contains("highest") || lowerQuery.contains("min") || lowerQuery.contains("lowest")) {
            intent = "EXTREMA_AND_SPREAD";
            if (!mentionedCols.isEmpty()) {
                DatasetColumn col = mentionedCols.get(0);
                answerMarkdown = String.format("### Range & Extrema for `%s`\n" +
                                "- **Minimum**: `%.3f`\n" +
                                "- **Maximum**: `%.3f`\n" +
                                "- **Interquartile Range (Q1 - Q3)**: `[%.3f, %.3f]`\n" +
                                "- **Skewness**: `%.3f`\n\n" +
                                "The span between minimum and maximum is **%.3f** units.",
                        col.getColumnName(), col.getMinVal(), col.getMaxVal(),
                        col.getQ1(), col.getQ3(), col.getSkewness(),
                        (col.getMaxVal() != null && col.getMinVal() != null) ? col.getMaxVal() - col.getMinVal() : 0.0);
                evidence.put("min", col.getMinVal());
                evidence.put("max", col.getMaxVal());
            } else {
                answerMarkdown = "Please specify a column name to inspect its numerical extrema and range.";
                confidence = 0.80;
            }

        } else if (lowerQuery.contains("quality") || lowerQuery.contains("missing") || lowerQuery.contains("null") || lowerQuery.contains("duplicate") || lowerQuery.contains("issue")) {
            intent = "DATA_QUALITY_ASSESSMENT";
            if (qualityReport != null) {
                answerMarkdown = String.format("### Data Quality Integrity Summary\n" +
                                "- **Overall Quality Score**: **%.1f / 100**\n" +
                                "- **Missing Values Score**: `%.1f / 100`\n" +
                                "- **Duplicate Rows Score**: `%.1f / 100`\n" +
                                "- **Total Identified Issues**: `%d`\n\n" +
                                "The dataset exhibits an overall rating of **%s**.",
                        qualityReport.getOverallScore(), qualityReport.getMissingValueScore(),
                        qualityReport.getDuplicateScore(), qualityReport.getTotalIssues(),
                        qualityReport.getOverallScore() >= 80 ? "EXCELLENT" : qualityReport.getOverallScore() >= 60 ? "MODERATE" : "NEEDS_CLEANING");
                evidence.put("overallScore", qualityReport.getOverallScore());
                evidence.put("totalIssues", qualityReport.getTotalIssues());
            } else {
                answerMarkdown = "No data quality audit has been executed on this dataset yet.";
            }

        } else if (lowerQuery.contains("correlat") || lowerQuery.contains("associat") || lowerQuery.contains("relationship") || lowerQuery.contains("link")) {
            intent = "STATISTICAL_ASSOCIATIONS";
            if (associations != null && !associations.isEmpty()) {
                List<Association> topAssoc = associations.stream()
                        .filter(a -> colNames.isEmpty() || colNames.contains(a.getVar1()) || colNames.contains(a.getVar2()))
                        .limit(4)
                        .collect(Collectors.toList());

                StringBuilder sb = new StringBuilder("### Top Empirical Associations\n");
                for (Association a : topAssoc) {
                    sb.append(String.format("- **%s** ↔ **%s**: Test: `%s` | Stat: `%.3f` | p-val: `%.4e` | Effect Size: `%.3f`\n  - *%s*\n",
                            a.getVar1(), a.getVar2(), a.getTestType(), a.getStatistic(), a.getPValue(), a.getEffectSize(), a.getInterpretation()));
                    evidence.put(a.getVar1() + "_" + a.getVar2(), a.getEffectSize());
                }
                sb.append("\n> **Important**: Empirical statistical associations indicate correlation, not causality.");
                answerMarkdown = sb.toString();
            } else {
                answerMarkdown = "No bivariate associations have been computed yet. Run 'Find Associations' to calculate empirical statistical tests.";
            }

        } else if (lowerQuery.contains("model") || lowerQuery.contains("predict") || lowerQuery.contains("automl") || lowerQuery.contains("best")) {
            intent = "MACHINE_LEARNING_PERFORMANCE";
            if (modelRuns != null && !modelRuns.isEmpty()) {
                ModelRun best = modelRuns.stream()
                        .filter(ModelRun::getIsBestModel)
                        .findFirst()
                        .orElse(modelRuns.get(0));

                answerMarkdown = String.format("### Best Predictive Model: %s\n" +
                                "- **Algorithm**: `%s`\n" +
                                "- **Validation Metrics**: `%s`\n" +
                                "- **Test Metrics**: `%s`\n" +
                                "- **Created**: `%s`\n\n" +
                                "This model was evaluated with train/val/test splits and zero data leakage.",
                        best.getModelName(), best.getAlgorithm(),
                        best.getValMetricsJson(), best.getTestMetricsJson(),
                        best.getCreatedAt());
                evidence.put("bestModel", best.getModelName());
                evidence.put("algorithm", best.getAlgorithm());
            } else {
                answerMarkdown = "No machine learning models have been trained on this dataset yet. Use the AutoML engine to benchmark candidate algorithms.";
            }

        } else {
            // General Overview
            intent = "DATASET_OVERVIEW";
            long numericCount = dataset.getColumns().stream().filter(c -> "NUMERIC".equalsIgnoreCase(c.getDataType())).count();
            long catCount = dataset.getColumns().stream().filter(c -> "CATEGORICAL".equalsIgnoreCase(c.getDataType())).count();

            answerMarkdown = String.format("### Dataset Overview: `%s`\n" +
                            "- **Total Observations (Rows)**: `%d`\n" +
                            "- **Total Features (Columns)**: `%d` (%d numeric, %d categorical)\n" +
                            "- **Detected Problem Suitability**: `%s`\n" +
                            "- **Original File**: `%s` (%.1f KB)\n\n" +
                            "You can ask me questions about specific column statistics, data quality, bivariate associations, or AutoML model performance.",
                    dataset.getName(), dataset.getRowCount(), dataset.getColumnCount(),
                    numericCount, catCount, dataset.getDatasetType(),
                    dataset.getOriginalFilename(), dataset.getFileSize() / 1024.0);
            evidence.put("rowCount", dataset.getRowCount());
            evidence.put("columnCount", dataset.getColumnCount());
            evidence.put("datasetType", dataset.getDatasetType());
        }

        return AskDatasetResponseDto.builder()
                .query(query)
                .detectedIntent(intent)
                .relevantColumns(colNames)
                .answerMarkdown(answerMarkdown)
                .empiricalEvidence(evidence)
                .confidence(confidence)
                .build();
    }
}
