package com.autoscientist.analysis.eda;

import com.autoscientist.analysis.statistics.StatisticalTestEngine;
import com.autoscientist.entity.DatasetColumn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EdaEngine {

    private final StatisticalTestEngine statEngine;

    public EdaEngine(StatisticalTestEngine statEngine) {
        this.statEngine = statEngine;
    }

    public static class CorrelationMatrix {
        private final List<String> variables;
        private final double[][] matrix;

        public CorrelationMatrix(List<String> variables, double[][] matrix) {
            this.variables = variables;
            this.matrix = matrix;
        }

        public List<String> getVariables() { return variables; }
        public double[][] getMatrix() { return matrix; }
    }

    public static class BoxPlotData {
        private final String variable;
        private final double min;
        private final double q1;
        private final double median;
        private final double q3;
        private final double max;
        private final double iqr;
        private final List<Double> outliers;

        public BoxPlotData(String variable, double min, double q1, double median, double q3, double max, double iqr, List<Double> outliers) {
            this.variable = variable;
            this.min = min;
            this.q1 = q1;
            this.median = median;
            this.q3 = q3;
            this.max = max;
            this.iqr = iqr;
            this.outliers = outliers;
        }

        public String getVariable() { return variable; }
        public double getMin() { return min; }
        public double getQ1() { return q1; }
        public double getMedian() { return median; }
        public double getQ3() { return q3; }
        public double getMax() { return max; }
        public double getIqr() { return iqr; }
        public List<Double> getOutliers() { return outliers; }
    }

    public static class GroupSummary {
        private final String groupName;
        private final int count;
        private final double mean;
        private final double stdDev;
        private final double median;
        private final double min;
        private final double max;

        public GroupSummary(String groupName, int count, double mean, double stdDev, double median, double min, double max) {
            this.groupName = groupName;
            this.count = count;
            this.mean = mean;
            this.stdDev = stdDev;
            this.median = median;
            this.min = min;
            this.max = max;
        }

        public String getGroupName() { return groupName; }
        public int count() { return count; }
        public double getMean() { return mean; }
        public double getStdDev() { return stdDev; }
        public double getMedian() { return median; }
        public double getMin() { return min; }
        public double getMax() { return max; }
    }

    public CorrelationMatrix computeCorrelationMatrix(InputStream inputStream, List<DatasetColumn> columns) throws Exception {
        List<DatasetColumn> numCols = columns.stream()
                .filter(c -> c.getDataType().equals("NUMERIC") && !c.getDataType().equals("IDENTIFIER"))
                .collect(Collectors.toList());

        List<String> variables = numCols.stream().map(DatasetColumn::getColumnName).collect(Collectors.toList());
        int k = variables.size();
        double[][] matrix = new double[k][k];

        if (k == 0) {
            return new CorrelationMatrix(variables, matrix);
        }

        Map<String, List<String>> colData = parseColumns(inputStream, variables);

        for (int i = 0; i < k; i++) {
            matrix[i][i] = 1.0;
            String varI = variables.get(i);
            List<String> rawI = colData.get(varI);

            for (int j = i + 1; j < k; j++) {
                String varJ = variables.get(j);
                List<String> rawJ = colData.get(varJ);

                StatisticalTestEngine.TestResult res = statEngine.computePearsonCorrelation(varI, varJ, rawI, rawJ);
                matrix[i][j] = res.getStatistic();
                matrix[j][i] = res.getStatistic();
            }
        }

        return new CorrelationMatrix(variables, matrix);
    }

    public List<BoxPlotData> computeBoxPlots(InputStream inputStream, List<DatasetColumn> columns) throws Exception {
        List<DatasetColumn> numCols = columns.stream()
                .filter(c -> c.getDataType().equals("NUMERIC") && !c.getDataType().equals("IDENTIFIER"))
                .collect(Collectors.toList());

        List<String> variables = numCols.stream().map(DatasetColumn::getColumnName).collect(Collectors.toList());
        Map<String, List<String>> colData = parseColumns(inputStream, variables);

        List<BoxPlotData> boxPlots = new ArrayList<>();
        for (String var : variables) {
            List<String> raw = colData.get(var);
            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (String s : raw) {
                try {
                    stats.addValue(Double.parseDouble(s));
                } catch (Exception ignored) {}
            }

            if (stats.getN() > 0) {
                double q1 = stats.getPercentile(25);
                double q3 = stats.getPercentile(75);
                double median = stats.getPercentile(50);
                double min = stats.getMin();
                double max = stats.getMax();
                double iqr = q3 - q1;

                List<Double> outliers = new ArrayList<>();
                if (iqr > 0) {
                    double lowerFence = q1 - 1.5 * iqr;
                    double upperFence = q3 + 1.5 * iqr;
                    for (double val : stats.getValues()) {
                        if (val < lowerFence || val > upperFence) {
                            outliers.add(val);
                        }
                    }
                }

                boxPlots.add(new BoxPlotData(var, min, q1, median, q3, max, iqr, outliers));
            }
        }

        return boxPlots;
    }

    public List<GroupSummary> computeGroupedSummary(InputStream inputStream, String numVar, String catVar) throws Exception {
        Map<String, List<String>> colData = parseColumns(inputStream, Arrays.asList(numVar, catVar));
        List<String> numRaw = colData.get(numVar);
        List<String> catRaw = colData.get(catVar);

        Map<String, DescriptiveStatistics> groupMap = new HashMap<>();
        int n = Math.min(numRaw.size(), catRaw.size());

        for (int i = 0; i < n; i++) {
            String sNum = numRaw.get(i);
            String sCat = catRaw.get(i);
            if (sNum != null && sCat != null && !sNum.trim().isEmpty() && !sCat.trim().isEmpty() &&
                    !sNum.equalsIgnoreCase("na") && !sCat.equalsIgnoreCase("na")) {
                try {
                    double v = Double.parseDouble(sNum.trim());
                    groupMap.computeIfAbsent(sCat.trim(), k -> new DescriptiveStatistics()).addValue(v);
                } catch (Exception ignored) {}
            }
        }

        List<GroupSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, DescriptiveStatistics> entry : groupMap.entrySet()) {
            DescriptiveStatistics st = entry.getValue();
            if (st.getN() > 0) {
                summaries.add(new GroupSummary(
                        entry.getKey(),
                        (int) st.getN(),
                        Math.round(st.getMean() * 100.0) / 100.0,
                        Math.round(st.getStandardDeviation() * 100.0) / 100.0,
                        Math.round(st.getPercentile(50) * 100.0) / 100.0,
                        Math.round(st.getMin() * 100.0) / 100.0,
                        Math.round(st.getMax() * 100.0) / 100.0
                ));
            }
        }

        summaries.sort(Comparator.comparing(GroupSummary::getGroupName));
        return summaries;
    }

    private Map<String, List<String>> parseColumns(InputStream inputStream, List<String> targetColumns) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        targetColumns.forEach(c -> result.put(c, new ArrayList<>()));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(false)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                for (String col : targetColumns) {
                    if (record.isMapped(col)) {
                        result.get(col).add(record.get(col));
                    }
                }
            }
        }

        return result;
    }
}
