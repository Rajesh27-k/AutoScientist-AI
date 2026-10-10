package com.autoscientist;

import com.autoscientist.analysis.core.AskDatasetEngine;
import com.autoscientist.analysis.drift.DataDriftEngine;
import com.autoscientist.dto.ask.AskDatasetResponseDto;
import com.autoscientist.dto.drift.DatasetDriftReportDto;
import com.autoscientist.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataDriftAndAskTest {

    private final DataDriftEngine dataDriftEngine = new DataDriftEngine();
    private final AskDatasetEngine askDatasetEngine = new AskDatasetEngine();

    @Test
    @DisplayName("DataDriftEngine: Accurately detects stable and drifted columns via PSI")
    void testDataDriftDetection() throws Exception {
        String refCsv = "feature1,category\n" +
                "10.0,A\n10.5,A\n11.0,B\n10.2,B\n10.8,A\n11.2,B\n10.1,A\n10.9,B\n10.4,A\n10.7,B\n";

        // Target with shifted values
        String tarCsv = "feature1,category\n" +
                "50.0,A\n50.5,A\n51.0,B\n50.2,B\n50.8,A\n51.2,B\n50.1,A\n50.9,B\n50.4,A\n50.7,B\n";

        DatasetColumn col1 = DatasetColumn.builder().columnName("feature1").dataType("NUMERIC").build();
        DatasetColumn col2 = DatasetColumn.builder().columnName("category").dataType("CATEGORICAL").build();

        Dataset ref = Dataset.builder().id(1L).name("Reference").columns(List.of(col1, col2)).build();
        Dataset tar = Dataset.builder().id(2L).name("Target").columns(List.of(col1, col2)).build();

        ByteArrayInputStream is1 = new ByteArrayInputStream(refCsv.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream is2 = new ByteArrayInputStream(tarCsv.getBytes(StandardCharsets.UTF_8));

        DatasetDriftReportDto report = dataDriftEngine.analyzeDrift(ref, is1, tar, is2);

        assertNotNull(report);
        assertEquals(2, report.getTotalFeaturesAnalyzed());
        assertTrue(report.getOverallDriftIndex() > 0.0);
        assertNotNull(report.getSummaryRecommendation());
    }

    @Test
    @DisplayName("AskDatasetEngine: Answers natural language questions with empirical proof")
    void testAskDatasetQueries() {
        DatasetColumn numCol = DatasetColumn.builder()
                .columnName("score")
                .dataType("NUMERIC")
                .meanVal(78.5)
                .medianVal(80.0)
                .stdDev(12.3)
                .minVal(40.0)
                .maxVal(100.0)
                .missingCount(0)
                .build();

        Dataset dataset = Dataset.builder()
                .id(1L)
                .name("Exam Scores")
                .rowCount(100)
                .columnCount(5)
                .datasetType("REGRESSION")
                .originalFilename("exams.csv")
                .fileSize(4096L)
                .columns(List.of(numCol))
                .build();

        DataQualityReport qualityReport = DataQualityReport.builder()
                .overallScore(92.0)
                .missingValueScore(100.0)
                .duplicateScore(95.0)
                .totalIssues(1)
                .build();

        // 1. Mean query
        AskDatasetResponseDto resp1 = askDatasetEngine.answerQuery(
                "What is the average score?", dataset, qualityReport, List.of(), List.of());
        assertNotNull(resp1);
        assertEquals("NUMERIC_CENTRAL_TENDENCY", resp1.getDetectedIntent());
        assertTrue(resp1.getAnswerMarkdown().contains("78.500"));

        // 2. Quality query
        AskDatasetResponseDto resp2 = askDatasetEngine.answerQuery(
                "How is data quality and missing values?", dataset, qualityReport, List.of(), List.of());
        assertNotNull(resp2);
        assertEquals("DATA_QUALITY_ASSESSMENT", resp2.getDetectedIntent());
        assertTrue(resp2.getAnswerMarkdown().contains("92.0"));
    }
}
