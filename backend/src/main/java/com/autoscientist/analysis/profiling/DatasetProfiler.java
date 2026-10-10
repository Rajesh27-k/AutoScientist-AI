package com.autoscientist.analysis.profiling;

import com.autoscientist.entity.Dataset;
import com.autoscientist.entity.DatasetColumn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DatasetProfiler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ProfilingResult {
        private final List<DatasetColumn> columns;
        private final int rowCount;
        private final int columnCount;
        private final String datasetType;

        public ProfilingResult(List<DatasetColumn> columns, int rowCount, int columnCount, String datasetType) {
            this.columns = columns;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.datasetType = datasetType;
        }

        public List<DatasetColumn> getColumns() {
            return columns;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public String getDatasetType() {
            return datasetType;
        }
    }

    public ProfilingResult profile(InputStream inputStream, Dataset dataset) throws Exception {
        List<String> headers;
        List<List<String>> rows = new ArrayList<>();

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
        int totalCols = headers.size();
        List<DatasetColumn> columns = new ArrayList<>();

        int numericCount = 0;
        int categoricalCount = 0;
        int dateTimeCount = 0;

        for (int colIdx = 0; colIdx < totalCols; colIdx++) {
            String colName = headers.get(colIdx);
            List<String> rawValues = new ArrayList<>();
            for (List<String> row : rows) {
                rawValues.add(row.get(colIdx));
            }

            // Missing values analysis
            int missingCount = 0;
            List<String> nonNullValues = new ArrayList<>();
            for (String val : rawValues) {
                if (val == null || val.trim().isEmpty() || val.equalsIgnoreCase("na") ||
                        val.equalsIgnoreCase("null") || val.equalsIgnoreCase("nan") || val.equalsIgnoreCase("none")) {
                    missingCount++;
                } else {
                    nonNullValues.add(val.trim());
                }
            }

            double missingPct = totalRows > 0 ? (double) missingCount / totalRows * 100.0 : 0.0;
            Set<String> uniqueSet = new HashSet<>(nonNullValues);
            int uniqueCount = uniqueSet.size();

            // Type inference
            TypeInference inference = inferColumnType(colName, nonNullValues, uniqueCount, totalRows);
            String dataType = inference.type;
            double confidence = inference.confidence;

            if (dataType.equals("NUMERIC")) numericCount++;
            else if (dataType.equals("CATEGORICAL") || dataType.equals("BOOLEAN")) categoricalCount++;
            else if (dataType.equals("DATE_TIME")) dateTimeCount++;

            DatasetColumn column = DatasetColumn.builder()
                    .dataset(dataset)
                    .columnIndex(colIdx)
                    .columnName(colName)
                    .dataType(dataType)
                    .typeConfidence(confidence)
                    .missingCount(missingCount)
                    .missingPct(Math.round(missingPct * 100.0) / 100.0)
                    .uniqueCount(uniqueCount)
                    .build();

            if (dataType.equals("NUMERIC") || (dataType.equals("IDENTIFIER") && inference.numeric)) {
                computeNumericStats(column, nonNullValues);
            } else {
                computeCategoricalStats(column, nonNullValues);
            }

            columns.add(column);
        }

        // Infer dataset macro type
        String datasetType = inferDatasetType(numericCount, categoricalCount, dateTimeCount, totalCols);

        return new ProfilingResult(columns, totalRows, totalCols, datasetType);
    }

    private static class TypeInference {
        String type;
        double confidence;
        boolean numeric;

        TypeInference(String type, double confidence, boolean numeric) {
            this.type = type;
            this.confidence = confidence;
            this.numeric = numeric;
        }
    }

    private boolean isIdentifierName(String colName) {
        if (colName == null) return false;
        String clean = colName.trim().toLowerCase();
        if (clean.equals("id") || clean.equals("uuid") || clean.equals("guid") || clean.equals("index") || clean.equals("row_id") || clean.equals("row_num")) {
            return true;
        }
        if (clean.endsWith("_id") || clean.startsWith("id_") || clean.startsWith("id.")) {
            return true;
        }
        if (colName.endsWith("Id") && colName.length() > 2) {
            return true;
        }
        return clean.contains("customer_id") || clean.contains("customerid")
                || clean.contains("user_id") || clean.contains("userid")
                || clean.contains("record_id") || clean.contains("recordid")
                || clean.contains("transaction_id") || clean.contains("transactionid")
                || clean.contains("claim_id") || clean.contains("claimid")
                || clean.contains("patient_id") || clean.contains("patientid")
                || clean.contains("student_id") || clean.contains("studentid")
                || clean.contains("account_id") || clean.contains("accountid")
                || clean.contains("order_id") || clean.contains("orderid");
    }

    private boolean isAllIntegerOrText(List<String> values) {
        for (String v : values) {
            if (v == null || v.isEmpty()) continue;
            try {
                double d = Double.parseDouble(v);
                if (d != Math.floor(d)) {
                    return false; // fractional decimal -> continuous analytical variable
                }
            } catch (NumberFormatException ignored) {
                // Alphanumeric code or text ID
            }
        }
        return true;
    }

    private TypeInference inferColumnType(String colName, List<String> values, int uniqueCount, int totalRows) {
        if (values.isEmpty()) {
            return new TypeInference("UNKNOWN", 0.5, false);
        }

        int numericMatches = 0;
        int boolMatches = 0;
        int dateMatches = 0;
        int longTextMatches = 0;

        for (String v : values) {
            if (isNumeric(v)) {
                numericMatches++;
            }
            if (isBoolean(v)) {
                boolMatches++;
            }
            if (isDateOrTime(v)) {
                dateMatches++;
            }
            if (v.length() > 60) {
                longTextMatches++;
            }
        }

        int n = values.size();
        double numRatio = (double) numericMatches / n;
        double boolRatio = (double) boolMatches / n;
        double dateRatio = (double) dateMatches / n;
        double distinctRatio = (double) uniqueCount / n;
        boolean matchesIdName = isIdentifierName(colName);

        // Detect identifier columns (e.g., id, customer_id, user_id, transaction_id, etc.)
        if (matchesIdName && (uniqueCount == n || distinctRatio > 0.40 || uniqueCount >= 20 || totalRows < 10) && isAllIntegerOrText(values)) {
            return new TypeInference("IDENTIFIER", 0.99, numRatio >= 0.85);
        }

        if (numRatio >= 0.90) {
            // Check if it's binary numeric 0/1 with exactly 2 unique values
            if (uniqueCount <= 2 && (values.stream().allMatch(v -> v.equals("0") || v.equals("1")))) {
                return new TypeInference("BOOLEAN", 0.95, true);
            }
            return new TypeInference("NUMERIC", numRatio, true);
        }

        if (boolRatio >= 0.95) {
            return new TypeInference("BOOLEAN", boolRatio, false);
        }

        if (dateRatio >= 0.85) {
            return new TypeInference("DATE_TIME", dateRatio, false);
        }

        if ((double) longTextMatches / n > 0.40) {
            return new TypeInference("TEXT", 0.90, false);
        }

        // Distinct ratio
        if (distinctRatio < 0.35 || uniqueCount <= 50) {
            return new TypeInference("CATEGORICAL", 0.95, false);
        }

        return new TypeInference("TEXT", 0.80, false);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(String str) {
        String s = str.toLowerCase();
        return s.equals("true") || s.equals("false") || s.equals("yes") || s.equals("no") || s.equals("t") || s.equals("f");
    }

    private boolean isDateOrTime(String str) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                LocalDate.parse(str, fmt);
                return true;
            } catch (Exception ignored) {}
            try {
                LocalDateTime.parse(str, fmt);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void computeNumericStats(DatasetColumn column, List<String> values) throws Exception {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (String v : values) {
            try {
                stats.addValue(Double.parseDouble(v));
            } catch (NumberFormatException ignored) {}
        }

        if (stats.getN() > 0) {
            column.setMinVal(stats.getMin());
            column.setMaxVal(stats.getMax());
            column.setMeanVal(stats.getMean());
            column.setMedianVal(stats.getPercentile(50));
            column.setStdDev(stats.getStandardDeviation());
            column.setVariance(stats.getVariance());
            column.setQ1(stats.getPercentile(25));
            column.setQ3(stats.getPercentile(75));
            column.setSkewness(Double.isNaN(stats.getSkewness()) ? 0.0 : stats.getSkewness());

            // Build histogram bins
            double min = stats.getMin();
            double max = stats.getMax();
            int numBins = 10;
            double binWidth = (max > min) ? (max - min) / numBins : 1.0;

            Map<String, Object> distMap = new LinkedHashMap<>();
            List<String> binLabels = new ArrayList<>();
            List<Integer> binCounts = new ArrayList<>();

            double[] binEdges = new double[numBins + 1];
            for (int i = 0; i <= numBins; i++) {
                binEdges[i] = min + i * binWidth;
            }

            int[] counts = new int[numBins];
            double[] allValues = stats.getValues();
            for (double val : allValues) {
                int binIndex = (int) ((val - min) / binWidth);
                if (binIndex >= numBins) binIndex = numBins - 1;
                if (binIndex < 0) binIndex = 0;
                counts[binIndex]++;
            }

            for (int i = 0; i < numBins; i++) {
                binLabels.add(String.format("%.2f - %.2f", binEdges[i], binEdges[i + 1]));
                binCounts.add(counts[i]);
            }

            distMap.put("labels", binLabels);
            distMap.put("counts", binCounts);
            column.setDistributionJson(objectMapper.writeValueAsString(distMap));
        }
    }

    private void computeCategoricalStats(DatasetColumn column, List<String> values) throws Exception {
        Map<String, Integer> freqMap = new LinkedHashMap<>();
        for (String v : values) {
            freqMap.put(v, freqMap.getOrDefault(v, 0) + 1);
        }

        // Sort by frequency descending and take top 20
        Map<String, Integer> topCategories = freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        column.setCategoriesJson(objectMapper.writeValueAsString(topCategories));
    }

    public String inferDatasetType(int numericCount, int categoricalCount, int dateTimeCount, int totalCols) {
        if (dateTimeCount >= 1 && (double) dateTimeCount / totalCols >= 0.15) {
            return "TIME_SERIES";
        }
        if ((double) numericCount / totalCols >= 0.80) {
            return "REGRESSION";
        }
        if ((double) categoricalCount / totalCols >= 0.60) {
            return "CLASSIFICATION";
        }
        return "MIXED";
    }
}
