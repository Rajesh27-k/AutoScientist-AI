package com.autoscientist;

import com.autoscientist.analysis.statistics.StatisticalTestEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticalTestEngineTest {

    private final StatisticalTestEngine engine = new StatisticalTestEngine();

    @Test
    void testPearsonCorrelationPositiveStrong() {
        List<String> x = Arrays.asList("1.0", "2.0", "3.0", "4.0", "5.0", "6.0", "7.0", "8.0");
        List<String> y = Arrays.asList("2.1", "4.2", "5.9", "8.1", "10.0", "12.2", "13.9", "16.1");

        StatisticalTestEngine.TestResult res = engine.computePearsonCorrelation("StudyHours", "Score", x, y);

        assertNotNull(res);
        assertEquals("PEARSON", res.getTestType());
        assertTrue(res.getStatistic() > 0.95, "Pearson r should be strongly positive > 0.95");
        assertTrue(res.getPValue() < 0.01, "p-value should be statistically significant < 0.01");
        assertTrue(res.isSignificant());
        assertEquals(8, res.getSampleSize());
        assertTrue(res.getInterpretation().contains("not causality") || res.getInterpretation().contains("linear association"));
    }

    @Test
    void testPearsonCorrelationZeroVariance() {
        List<String> x = Arrays.asList("5.0", "5.0", "5.0", "5.0", "5.0");
        List<String> y = Arrays.asList("10.0", "20.0", "30.0", "40.0", "50.0");

        StatisticalTestEngine.TestResult res = engine.computePearsonCorrelation("ConstantX", "VarY", x, y);

        assertNotNull(res);
        assertFalse(res.isSignificant());
        assertEquals(0.0, res.getStatistic());
        assertTrue(res.getInterpretation().contains("Zero variance"));
    }

    @Test
    void testWelchTTestIndependentGroups() {
        List<String> score = Arrays.asList("85.0", "88.0", "92.0", "90.0", "45.0", "50.0", "48.0", "52.0");
        List<String> group = Arrays.asList("A", "A", "A", "A", "B", "B", "B", "B");

        StatisticalTestEngine.TestResult res = engine.computeWelchTTest("Score", "Group", score, group);

        assertNotNull(res);
        assertEquals("WELCH_T_TEST", res.getTestType());
        assertTrue(res.getStatistic() > 5.0, "t-statistic should be large");
        assertTrue(res.getPValue() < 0.01, "p-value should be significant");
        assertTrue(res.getEffectSize() > 0.8, "Cohen's d should be large");
        assertTrue(res.isSignificant());
    }

    @Test
    void testOneWayAnovaThreeGroups() {
        List<String> score = Arrays.asList(
                "10.0", "12.0", "11.0",
                "25.0", "28.0", "27.0",
                "50.0", "55.0", "52.0"
        );
        List<String> treatment = Arrays.asList(
                "Ctrl", "Ctrl", "Ctrl",
                "Dose1", "Dose1", "Dose1",
                "Dose2", "Dose2", "Dose2"
        );

        StatisticalTestEngine.TestResult res = engine.computeOneWayAnova("Score", "Treatment", score, treatment);

        assertNotNull(res);
        assertEquals("ONE_WAY_ANOVA", res.getTestType());
        assertTrue(res.getStatistic() > 10.0, "F-statistic should be high");
        assertTrue(res.getPValue() < 0.01, "p-value should be significant");
        assertTrue(res.getEffectSize() > 0.5, "Eta squared should be substantial");
        assertTrue(res.isSignificant());
    }

    @Test
    void testChiSquareContingencyTable() {
        // 2x2 contingency table: Treatment vs Recovery
        List<String> treat = Arrays.asList(
                "Drug", "Drug", "Drug", "Drug", "Drug", "Drug", "Drug", "Drug", "Drug", "Drug",
                "Placebo", "Placebo", "Placebo", "Placebo", "Placebo", "Placebo", "Placebo", "Placebo", "Placebo", "Placebo"
        );
        List<String> outcome = Arrays.asList(
                "Cured", "Cured", "Cured", "Cured", "Cured", "Cured", "Cured", "Cured", "Sick", "Sick",
                "Sick", "Sick", "Sick", "Sick", "Sick", "Sick", "Sick", "Sick", "Cured", "Cured"
        );

        StatisticalTestEngine.TestResult res = engine.computeChiSquare("Treatment", "Outcome", treat, outcome);

        assertNotNull(res);
        assertEquals("CHI_SQUARE", res.getTestType());
        assertTrue(res.getStatistic() > 3.84, "Chi-square should exceed critical threshold");
        assertTrue(res.getPValue() < 0.05, "p-value should be significant");
        assertTrue(res.getEffectSize() > 0.3, "Cramer's V should indicate association");
        assertTrue(res.isSignificant());
    }
}
