package com.autoscientist;

import com.autoscientist.analysis.profiling.DatasetProfiler;
import com.autoscientist.analysis.quality.DataQualityEngine;
import com.autoscientist.entity.DataQualityReport;
import com.autoscientist.entity.Dataset;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DataQualityEngineTest {

    private final DatasetProfiler profiler = new DatasetProfiler();
    private final DataQualityEngine qualityEngine = new DataQualityEngine();

    @Test
    void testQualityAuditWithDuplicatesAndMissingness() throws Exception {
        String csv = "id,category,value\n" +
                "1,A,10.0\n" +
                "2,A,20.0\n" +
                "3,B,NA\n" +
                "1,A,10.0\n"; // duplicate row

        Dataset dataset = Dataset.builder().name("Audit Test").build();
        ByteArrayInputStream in1 = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        DatasetProfiler.ProfilingResult profResult = profiler.profile(in1, dataset);

        ByteArrayInputStream in2 = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        DataQualityReport report = qualityEngine.evaluate(in2, dataset, profResult.getColumns());

        assertNotNull(report);
        assertTrue(report.getOverallScore() < 100.0, "Score should reflect duplicate row and missing value");
        assertTrue(report.getTotalIssues() > 0);

        boolean duplicateFound = report.getIssues().stream()
                .anyMatch(issue -> issue.getIssueType().equals("DUPLICATE_ROWS"));
        assertTrue(duplicateFound, "Should report duplicate row");
    }
}
