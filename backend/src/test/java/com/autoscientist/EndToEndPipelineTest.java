package com.autoscientist;

import com.autoscientist.analysis.association.AssociationEngine;
import com.autoscientist.analysis.drift.DataDriftEngine;
import com.autoscientist.analysis.feature.FeatureEngineeringEngine;
import com.autoscientist.analysis.feature.FeatureEngineeringEngine.DatasetSplit;
import com.autoscientist.analysis.findings.QuestionAnalysisEngine;
import com.autoscientist.analysis.ml.AutoMlEngine;
import com.autoscientist.analysis.ml.AutoMlEngine.ModelBenchmarkResult;
import com.autoscientist.analysis.profiling.DatasetProfiler;
import com.autoscientist.analysis.quality.DataQualityEngine;
import com.autoscientist.analysis.report.PdfExportService;
import com.autoscientist.analysis.report.ReportGeneratorService;
import com.autoscientist.analysis.statistics.StatisticalTestEngine;
import com.autoscientist.analysis.xai.ExplainabilityEngine;
import com.autoscientist.dto.drift.DatasetDriftReportDto;
import com.autoscientist.dto.ml.FeatureImportanceDto;
import com.autoscientist.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndPipelineTest {

    private final DatasetProfiler profiler = new DatasetProfiler();
    private final DataQualityEngine qualityEngine = new DataQualityEngine();
    private final StatisticalTestEngine testEngine = new StatisticalTestEngine();
    private final AssociationEngine associationEngine = new AssociationEngine(testEngine);
    private final QuestionAnalysisEngine questionEngine = new QuestionAnalysisEngine();
    private final FeatureEngineeringEngine featureEngineeringEngine = new FeatureEngineeringEngine();
    private final AutoMlEngine autoMlEngine = new AutoMlEngine();
    private final ExplainabilityEngine explainabilityEngine = new ExplainabilityEngine();
    private final DataDriftEngine driftEngine = new DataDriftEngine();
    private final ReportGeneratorService reportGeneratorService = new ReportGeneratorService();
    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    @DisplayName("Complete End-to-End Scientific Pipeline on Real Student Performance Dataset")
    void testEndToEndStudentPerformancePipeline() throws Exception {
        Path csvPath = Paths.get("../test-data/student_performance.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("test-data/student_performance.csv");
        }
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("/Applications/AutoScientist AI/test-data/student_performance.csv");
        }
        byte[] csvBytes = Files.readAllBytes(csvPath);

        Dataset dataset = Dataset.builder()
                .id(1L)
                .name("Student Performance")
                .originalFilename("student_performance.csv")
                .datasetType("REGRESSION")
                .build();

        // 1. Profile Dataset
        DatasetProfiler.ProfilingResult profResult;
        try (InputStream is = new ByteArrayInputStream(csvBytes)) {
            profResult = profiler.profile(is, dataset);
        }
        assertNotNull(profResult);
        assertTrue(profResult.getRowCount() >= 30, "Dataset should have at least 30 observations");
        assertEquals(7, profResult.getColumnCount());
        dataset.setRowCount(profResult.getRowCount());
        dataset.setColumnCount(profResult.getColumnCount());

        List<DatasetColumn> columns = profResult.getColumns();
        columns.forEach(c -> c.setDataset(dataset));
        dataset.setColumns(columns);

        DatasetColumn studyHoursCol = columns.stream()
                .filter(c -> c.getColumnName().equals("study_hours"))
                .findFirst().orElseThrow();
        assertEquals("NUMERIC", studyHoursCol.getDataType());
        assertTrue(studyHoursCol.getMeanVal() > 5.0);

        // 2. Quality Audit
        DataQualityReport qualityReport;
        try (InputStream is = new ByteArrayInputStream(csvBytes)) {
            qualityReport = qualityEngine.evaluate(is, dataset, columns);
        }
        assertNotNull(qualityReport);
        assertTrue(qualityReport.getOverallScore() >= 80.0, "Clean dataset should have quality >= 80");

        // 3. Bivariate Associations
        List<Association> associations;
        try (InputStream is = new ByteArrayInputStream(csvBytes)) {
            associations = associationEngine.discoverAssociations(is, dataset, columns);
        }
        assertNotNull(associations);
        assertFalse(associations.isEmpty());

        Association studyScoreAssoc = associations.stream()
                .filter(a -> (a.getVar1().equals("study_hours") && a.getVar2().equals("exam_score")) ||
                             (a.getVar2().equals("study_hours") && a.getVar1().equals("exam_score")))
                .findFirst().orElseThrow();
        assertTrue(studyScoreAssoc.getIsSignificant(), "Study hours and exam score must be significantly correlated");
        assertTrue(studyScoreAssoc.getStatistic() > 0.85, "Correlation should be strongly positive (r > 0.85)");

        // 4. Research Question & Hypothesis Generation
        ResearchQuestion rqTemplate = ResearchQuestion.builder().build();
        var rqResult = questionEngine.analyzeQuestion(
                "Does study_hours linearly predict exam_score?",
                "study_hours", "exam_score", columns, rqTemplate);
        assertNotNull(rqResult);
        assertEquals("study_hours", rqResult.getPredictorVariable());
        assertEquals("exam_score", rqResult.getTargetVariable());
        assertEquals("PEARSON", rqResult.getSuggestedTest());

        // 5. Feature Engineering & Split
        DatasetSplit split;
        try (InputStream is = new ByteArrayInputStream(csvBytes)) {
            split = featureEngineeringEngine.prepareDataset(is, columns, "exam_score", "REGRESSION");
        }
        assertNotNull(split);
        assertEquals("REGRESSION", split.getProblemType());
        assertTrue(split.getXTrain().length > 0);
        assertTrue(split.getXVal().length > 0);
        assertTrue(split.getXTest().length > 0);

        // 6. AutoML Benchmarking
        List<ModelBenchmarkResult> modelResults = autoMlEngine.runAutoMlBenchmark(split);
        assertNotNull(modelResults);
        assertEquals(3, modelResults.size());
        ModelBenchmarkResult bestModel = modelResults.get(0);
        assertTrue(bestModel.isBest());
        assertTrue(bestModel.getValMetrics().get("r2") > 0.80, "Validation R2 should be high");

        // 7. Explainability (XAI)
        List<FeatureImportanceDto> importances = explainabilityEngine.computeFeatureImportance(split, bestModel);
        assertNotNull(importances);
        assertFalse(importances.isEmpty());
        assertEquals("study_hours", importances.get(0).getFeatureName(), "study_hours should be top predictive driver");

        // 8. Data Drift
        DatasetDriftReportDto driftReport = driftEngine.analyzeDrift(
                dataset, new ByteArrayInputStream(csvBytes),
                dataset, new ByteArrayInputStream(csvBytes));
        assertNotNull(driftReport);
        assertEquals("STABLE", driftReport.getOverallStatus(), "Self-comparison should be STABLE with 0 drift");

        // 9. Report Generation & Vector PDF Export
        ResearchQuestion rq = ResearchQuestion.builder()
                .id(1L)
                .dataset(dataset)
                .questionText("Does study_hours linearly predict exam_score?")
                .targetVariable("exam_score")
                .predictorVariable("study_hours")
                .suggestedTest("PEARSON")
                .build();

        Hypothesis hyp = Hypothesis.builder()
                .id(1L)
                .researchQuestion(rq)
                .nullHypothesis("There is no linear association between study_hours and exam_score (r = 0)")
                .altHypothesis("There is a significant positive linear association between study_hours and exam_score (r > 0)")
                .suggestedTest("PEARSON")
                .rationale("Higher study hours facilitate concept retention.")
                .build();

        StatisticalTest statTest = StatisticalTest.builder()
                .id(1L)
                .hypothesis(hyp)
                .dataset(dataset)
                .testType("PEARSON")
                .var1("study_hours")
                .var2("exam_score")
                .statistic(studyScoreAssoc.getStatistic())
                .pValue(studyScoreAssoc.getPValue())
                .effectSize(studyScoreAssoc.getEffectSize())
                .effectSizeType("r_squared")
                .sampleSize(studyScoreAssoc.getSampleSize())
                .isStatisticallySignificant(true)
                .status("REJECT_NULL")
                .interpretation(studyScoreAssoc.getInterpretation())
                .build();

        Finding finding = Finding.builder()
                .id(1L)
                .dataset(dataset)
                .researchQuestion(rq)
                .title("Decisive Linear Association Between Study Hours and Exam Performance")
                .summary("Empirical evidence indicates that study hours are strongly predictive of exam scores.")
                .evidenceText("Pearson correlation r=" + studyScoreAssoc.getStatistic() + ", p=" + studyScoreAssoc.getPValue())
                .confidenceLevel("VERY_HIGH")
                .causalityDisclaimer("Observational association only; causal direction requires controlled intervention.")
                .build();

        String reportMarkdown = reportGeneratorService.generateScientificReportMarkdown(
                dataset, qualityReport, associations, rq, List.of(hyp), List.of(statTest), List.of(), List.of(finding));

        assertNotNull(reportMarkdown);
        assertTrue(reportMarkdown.contains("Does study_hours linearly predict exam_score?"));
        assertTrue(reportMarkdown.contains("REJECT $H_0$"));
        assertTrue(reportMarkdown.contains("Strict Causality Disclaimer"));

        byte[] pdfBytes = pdfExportService.generatePdfReport("Scientific Discovery Report", reportMarkdown);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));
    }
}
