package com.autoscientist;

import com.autoscientist.analysis.report.PdfExportService;
import com.autoscientist.analysis.report.ReportGeneratorService;
import com.autoscientist.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportAndPdfTest {

    private final ReportGeneratorService reportGeneratorService = new ReportGeneratorService();
    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    @DisplayName("ReportGeneratorService & PdfExportService: Generates Markdown and valid PDF binary")
    void testReportAndPdfGeneration() {
        DatasetColumn col1 = DatasetColumn.builder()
                .columnName("age")
                .dataType("NUMERIC")
                .typeConfidence(1.0)
                .missingCount(0)
                .missingPct(0.0)
                .uniqueCount(40)
                .meanVal(35.2)
                .medianVal(34.0)
                .stdDev(8.4)
                .minVal(18.0)
                .maxVal(65.0)
                .build();

        Dataset dataset = Dataset.builder()
                .id(1L)
                .name("Health Metrics Cohort")
                .originalFilename("health.csv")
                .rowCount(500)
                .columnCount(1)
                .datasetType("REGRESSION")
                .columns(List.of(col1))
                .build();

        DataQualityReport qualityReport = DataQualityReport.builder()
                .overallScore(94.5)
                .missingValueScore(100.0)
                .duplicateScore(98.0)
                .outlierScore(88.0)
                .consistencyScore(92.0)
                .totalIssues(0)
                .build();

        Finding finding = Finding.builder()
                .id(1L)
                .title("Elevated Biomarker Association with Age")
                .summary("Statistically significant linear correlation observed.")
                .evidenceText("Pearson r=0.48, p=1.2e-5, N=500")
                .confidenceLevel("HIGH")
                .causalityDisclaimer("Observational finding only; confounding unaddressed.")
                .build();

        String markdown = reportGeneratorService.generateScientificReportMarkdown(
                dataset, qualityReport, List.of(), null, List.of(), List.of(), List.of(), List.of(finding));

        assertNotNull(markdown);
        assertTrue(markdown.contains("Comprehensive Empirical Discovery Report"));
        assertTrue(markdown.contains("Health Metrics Cohort"));
        assertTrue(markdown.contains("Data Quality & Hygiene Assessment"));
        assertTrue(markdown.contains("Strict Causality Disclaimer"));
        assertTrue(markdown.contains("Machine Learning: Not performed"));
        assertTrue(markdown.contains("Explainability: Not available because no model was trained"));
        assertTrue(markdown.contains("Fairness Analysis: Not performed"));
        assertTrue(markdown.contains("Data Drift: Not performed"));
        assertTrue(markdown.contains("Conclusion & Reproducibility"));
        assertTrue(markdown.contains("Methodological Limitations"));

        // Generate PDF
        byte[] pdfBytes = pdfExportService.generatePdfReport("Scientific Discovery Report", markdown);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should contain valid PDF structure");

        // Verify PDF Magic Bytes %PDF
        String magic = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", magic);
    }
}
