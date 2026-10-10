package com.autoscientist;

import com.autoscientist.analysis.findings.QuestionAnalysisEngine;
import com.autoscientist.entity.DatasetColumn;
import com.autoscientist.entity.ResearchQuestion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionAnalysisEngineTest {

    private final QuestionAnalysisEngine engine = new QuestionAnalysisEngine();

    @Test
    void testAnalyzeContinuousQuestion() {
        List<DatasetColumn> columns = Arrays.asList(
                DatasetColumn.builder().columnName("study_hours").dataType("NUMERIC").uniqueCount(25).build(),
                DatasetColumn.builder().columnName("exam_score").dataType("NUMERIC").uniqueCount(28).build(),
                DatasetColumn.builder().columnName("tutoring").dataType("CATEGORICAL").uniqueCount(2).build()
        );

        ResearchQuestion rq = ResearchQuestion.builder().build();
        QuestionAnalysisEngine.QuestionAnalysisResult result = engine.analyzeQuestion(
                "Is study_hours linearly associated with exam_score?",
                null, null, columns, rq
        );

        assertNotNull(result);
        assertEquals("study_hours", result.getPredictorVariable());
        assertEquals("exam_score", result.getTargetVariable());
        assertEquals("PEARSON", result.getSuggestedTest());
        assertEquals(1, result.getHypotheses().size());
        assertTrue(result.getHypotheses().get(0).getNullHypothesis().contains("no statistically significant"));
        assertTrue(result.getHypotheses().get(0).getAltHypothesis().contains("statistically significant"));
    }

    @Test
    void testAnalyzeGroupDifferenceQuestion() {
        List<DatasetColumn> columns = Arrays.asList(
                DatasetColumn.builder().columnName("tutoring").dataType("CATEGORICAL").uniqueCount(2).build(),
                DatasetColumn.builder().columnName("exam_score").dataType("NUMERIC").uniqueCount(28).build()
        );

        ResearchQuestion rq = ResearchQuestion.builder().build();
        QuestionAnalysisEngine.QuestionAnalysisResult result = engine.analyzeQuestion(
                "Does tutoring make a difference in exam_score?",
                "tutoring", "exam_score", columns, rq
        );

        assertNotNull(result);
        assertEquals("tutoring", result.getPredictorVariable());
        assertEquals("exam_score", result.getTargetVariable());
        assertEquals("WELCH_T_TEST", result.getSuggestedTest());
        assertEquals("DIFFERENCE", result.getQuestionType());
    }
}
