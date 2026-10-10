package com.autoscientist.analysis.quality;

import com.autoscientist.entity.DataQualityIssue;
import com.autoscientist.entity.DataQualityReport;
import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.DatasetColumn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class DataQualityEngine {

    public DataQualityReport evaluate(InputStream inputStream, Dataset dataset, List<DatasetColumn> columns) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        List<String> headers;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(false)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            headers = new ArrayList<>(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                List<String> rowValues = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    rowValues.add(record.get(i));
                }
                rows.add(rowValues);
            }
        }

        int totalRows = rows.size();
        List<DataQualityIssue> issues = new ArrayList<>();

        DataQualityReport report = DataQualityReport.builder()
                .dataset(dataset)
                .issues(issues)
                .build();

        // 1. Check duplicate rows
        Set<String> uniqueRowHashes = new HashSet<>();
        int duplicateRowCount = 0;
        for (List<String> row : rows) {
            String hash = String.join("|||", row);
            if (!uniqueRowHashes.add(hash)) {
                duplicateRowCount++;
            }
        }

        double duplicateScore = totalRows > 0 ? Math.max(0.0, 100.0 - ((double) duplicateRowCount / totalRows * 100.0)) : 100.0;
        if (duplicateRowCount > 0) {
            issues.add(DataQualityIssue.builder()
                    .report(report)
                    .columnName(null)
                    .issueType("DUPLICATE_ROWS")
                    .severity(duplicateRowCount > totalRows * 0.10 ? "WARNING" : "INFO")
                    .description(String.format("Found %d duplicate rows (%.2f%% of total rows).", duplicateRowCount, (double) duplicateRowCount / totalRows * 100.0))
                    .recommendation("Consider deduplicating records prior to statistical modeling to avoid bias.")
                    .affectedRows(duplicateRowCount)
                    .build());
        }

        // 2. Column-level quality audits
        double totalMissingPctSum = 0;
        int outlierCountSum = 0;
        int inconsistentCategoriesCount = 0;

        for (DatasetColumn col : columns) {
            totalMissingPctSum += col.getMissingPct();

            // Missingness checks
            if (col.getMissingPct() > 50.0) {
                issues.add(DataQualityIssue.builder()
                        .report(report)
                        .columnName(col.getColumnName())
                        .issueType("CRITICAL_MISSINGNESS")
                        .severity("ERROR")
                        .description(String.format("Column '%s' has %.1f%% missing values.", col.getColumnName(), col.getMissingPct()))
                        .recommendation("Imputation may introduce heavy bias. Consider excluding or obtaining complete data.")
                        .affectedRows(col.getMissingCount())
                        .build());
            } else if (col.getMissingPct() > 15.0) {
                issues.add(DataQualityIssue.builder()
                        .report(report)
                        .columnName(col.getColumnName())
                        .issueType("HIGH_MISSINGNESS")
                        .severity("WARNING")
                        .description(String.format("Column '%s' has %.1f%% missing values.", col.getColumnName(), col.getMissingPct()))
                        .recommendation("Use robust imputation (median for numeric, mode for categorical) or missingness indicators.")
                        .affectedRows(col.getMissingCount())
                        .build());
            } else if (col.getMissingCount() > 0) {
                issues.add(DataQualityIssue.builder()
                        .report(report)
                        .columnName(col.getColumnName())
                        .issueType("LOW_MISSINGNESS")
                        .severity("INFO")
                        .description(String.format("Column '%s' has %d missing values (%.1f%%).", col.getColumnName(), col.getMissingCount(), col.getMissingPct()))
                        .recommendation("Paired complete-case analysis or mean/median imputation is suitable.")
                        .affectedRows(col.getMissingCount())
                        .build());
            }

            // Zero variance check
            if (col.getUniqueCount() <= 1 && totalRows > 1) {
                issues.add(DataQualityIssue.builder()
                        .report(report)
                        .columnName(col.getColumnName())
                        .issueType("ZERO_VARIANCE")
                        .severity("WARNING")
                        .description(String.format("Column '%s' has constant values (0 variance).", col.getColumnName()))
                        .recommendation("Constant columns provide no explanatory power in statistical tests or ML. Safe to drop.")
                        .affectedRows(totalRows)
                        .build());
            }

            // Unique Identifier check
            if (col.getUniqueCount() == totalRows && totalRows > 10 && (col.getDataType().equals("TEXT") || col.getDataType().equals("NUMERIC"))) {
                issues.add(DataQualityIssue.builder()
                        .report(report)
                        .columnName(col.getColumnName())
                        .issueType("UNIQUE_IDENTIFIER")
                        .severity("INFO")
                        .description(String.format("Column '%s' has 100%% distinct values; appears to be an ID/key.", col.getColumnName()))
                        .recommendation("Exclude identifiers from statistical modeling to prevent spurious overfitting.")
                        .affectedRows(totalRows)
                        .build());
            }

            // Outlier detection for numeric columns using IQR rule
            if (col.getDataType().equals("NUMERIC") && col.getQ1() != null && col.getQ3() != null) {
                double iqr = col.getQ3() - col.getQ1();
                if (iqr > 0) {
                    double lowerBound = col.getQ1() - 1.5 * iqr;
                    double upperBound = col.getQ3() + 1.5 * iqr;

                    int colIndex = col.getColumnIndex();
                    int outliers = 0;
                    for (List<String> row : rows) {
                        try {
                            double val = Double.parseDouble(row.get(colIndex));
                            if (val < lowerBound || val > upperBound) {
                                outliers++;
                            }
                        } catch (Exception ignored) {}
                    }

                    if (outliers > 0) {
                        outlierCountSum += outliers;
                        issues.add(DataQualityIssue.builder()
                                .report(report)
                                .columnName(col.getColumnName())
                                .issueType("OUTLIERS_DETECTED")
                                .severity("INFO")
                                .description(String.format("Column '%s' has %d potential statistical outliers (beyond 1.5x IQR).", col.getColumnName(), outliers))
                                .recommendation("Verify if outliers represent legitimate extreme observations or measurement errors.")
                                .affectedRows(outliers)
                                .build());
                    }
                }
            }

            // Inconsistent casing check for categorical columns
            if (col.getDataType().equals("CATEGORICAL")) {
                int colIndex = col.getColumnIndex();
                Map<String, Set<String>> lowerToOriginals = new HashMap<>();
                for (List<String> row : rows) {
                    String val = row.get(colIndex);
                    if (val != null && !val.trim().isEmpty()) {
                        String lower = val.trim().toLowerCase();
                        lowerToOriginals.computeIfAbsent(lower, k -> new HashSet<>()).add(val.trim());
                    }
                }

                for (Map.Entry<String, Set<String>> entry : lowerToOriginals.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        inconsistentCategoriesCount++;
                        issues.add(DataQualityIssue.builder()
                                .report(report)
                                .columnName(col.getColumnName())
                                .issueType("INCONSISTENT_CASING")
                                .severity("WARNING")
                                .description(String.format("Column '%s' contains casing inconsistencies for value '%s': %s.",
                                        col.getColumnName(), entry.getKey(), entry.getValue()))
                                .recommendation("Standardize category strings (e.g. normalize to lowercase/uppercase).")
                                .affectedRows(entry.getValue().size())
                                .build());
                        break;
                    }
                }
            }
        }

        // Compute scores
        int numCols = columns.isEmpty() ? 1 : columns.size();
        double avgMissingPct = totalMissingPctSum / numCols;
        double missingValueScore = Math.max(0.0, 100.0 - avgMissingPct * 1.5);

        double outlierFraction = totalRows > 0 ? (double) outlierCountSum / (totalRows * numCols) : 0.0;
        double outlierScore = Math.max(0.0, 100.0 - (outlierFraction * 100.0 * 2.0));

        double consistencyScore = Math.max(0.0, 100.0 - (inconsistentCategoriesCount * 15.0));

        // Overall weighted score
        double overallScore = (missingValueScore * 0.40) + (duplicateScore * 0.25) +
                (outlierScore * 0.15) + (consistencyScore * 0.20);

        report.setOverallScore(Math.round(overallScore * 10.0) / 10.0);
        report.setMissingValueScore(Math.round(missingValueScore * 10.0) / 10.0);
        report.setDuplicateScore(Math.round(duplicateScore * 10.0) / 10.0);
        report.setOutlierScore(Math.round(outlierScore * 10.0) / 10.0);
        report.setConsistencyScore(Math.round(consistencyScore * 10.0) / 10.0);
        report.setTotalIssues(issues.size());

        return report;
    }
}
