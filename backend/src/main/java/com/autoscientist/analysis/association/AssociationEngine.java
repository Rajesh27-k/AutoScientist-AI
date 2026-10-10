package com.autoscientist.analysis.association;

import com.autoscientist.analysis.statistics.StatisticalTestEngine;
import com.autoscientist.entity.Association;
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
public class AssociationEngine {

    private final StatisticalTestEngine testEngine;

    public AssociationEngine(StatisticalTestEngine testEngine) {
        this.testEngine = testEngine;
    }

    public List<Association> discoverAssociations(InputStream inputStream, Dataset dataset, List<DatasetColumn> columns) throws Exception {
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
                List<String> r = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    r.add(record.get(i));
                }
                rows.add(r);
            }
        }

        // Map column name to raw value column list
        Map<String, List<String>> colData = new HashMap<>();
        Map<String, DatasetColumn> colMeta = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String colName = headers.get(i);
            List<String> colVals = new ArrayList<>();
            for (List<String> row : rows) {
                colVals.add(row.get(i));
            }
            colData.put(colName, colVals);
        }
        for (DatasetColumn c : columns) {
            colMeta.put(c.getColumnName(), c);
        }

        List<Association> associations = new ArrayList<>();
        int numCols = columns.size();

        for (int i = 0; i < numCols; i++) {
            DatasetColumn colA = columns.get(i);
            String nameA = colA.getColumnName();
            String typeA = colA.getDataType();
            List<String> rawA = colData.get(nameA);

            for (int j = i + 1; j < numCols; j++) {
                DatasetColumn colB = columns.get(j);
                String nameB = colB.getColumnName();
                String typeB = colB.getDataType();
                List<String> rawB = colData.get(nameB);

                // Skip non-informative ID columns (IDENTIFIER, 100% unique strings, or single constant values)
                if (typeA.equals("IDENTIFIER") || typeB.equals("IDENTIFIER")) continue;
                if (colA.getUniqueCount() <= 1 || colB.getUniqueCount() <= 1) continue;
                if ((typeA.equals("TEXT") && colA.getUniqueCount() > 50) || (typeB.equals("TEXT") && colB.getUniqueCount() > 50)) continue;

                StatisticalTestEngine.TestResult result = null;

                if (typeA.equals("NUMERIC") && typeB.equals("NUMERIC")) {
                    result = testEngine.computePearsonCorrelation(nameA, nameB, rawA, rawB);
                } else if (typeA.equals("NUMERIC") && (typeB.equals("CATEGORICAL") || typeB.equals("BOOLEAN"))) {
                    if (colB.getUniqueCount() == 2) {
                        result = testEngine.computeWelchTTest(nameA, nameB, rawA, rawB);
                    } else if (colB.getUniqueCount() > 2 && colB.getUniqueCount() <= 20) {
                        result = testEngine.computeOneWayAnova(nameA, nameB, rawA, rawB);
                    }
                } else if ((typeA.equals("CATEGORICAL") || typeA.equals("BOOLEAN")) && typeB.equals("NUMERIC")) {
                    if (colA.getUniqueCount() == 2) {
                        result = testEngine.computeWelchTTest(nameB, nameA, rawB, rawA);
                    } else if (colA.getUniqueCount() > 2 && colA.getUniqueCount() <= 20) {
                        result = testEngine.computeOneWayAnova(nameB, nameA, rawB, rawA);
                    }
                } else if ((typeA.equals("CATEGORICAL") || typeA.equals("BOOLEAN")) &&
                        (typeB.equals("CATEGORICAL") || typeB.equals("BOOLEAN"))) {
                    if (colA.getUniqueCount() <= 25 && colB.getUniqueCount() <= 25) {
                        result = testEngine.computeChiSquare(nameA, nameB, rawA, rawB);
                    }
                }

                if (result != null && result.getSampleSize() > 0) {
                    Association assoc = Association.builder()
                            .dataset(dataset)
                            .var1(nameA)
                            .var2(nameB)
                            .var1Type(typeA)
                            .var2Type(typeB)
                            .testType(result.getTestType())
                            .statistic(result.getStatistic())
                            .pValue(result.getPValue())
                            .effectSize(result.getEffectSize())
                            .sampleSize(result.getSampleSize())
                            .degreesOfFreedom(result.getDegreesOfFreedom())
                            .isSignificant(result.isSignificant())
                            .interpretation(result.getInterpretation())
                            .rankScore(result.getRankScore())
                            .build();

                    associations.add(assoc);
                }
            }
        }

        // Rank associations by rankScore descending
        associations.sort((a, b) -> Double.compare(b.getRankScore(), a.getRankScore()));

        return associations;
    }
}
