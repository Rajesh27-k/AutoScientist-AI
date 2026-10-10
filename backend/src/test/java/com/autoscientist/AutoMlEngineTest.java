package com.autoscientist;

import com.autoscientist.analysis.fairness.FairnessEngine;
import com.autoscientist.analysis.feature.FeatureEngineeringEngine.DatasetSplit;
import com.autoscientist.analysis.ml.AutoMlEngine;
import com.autoscientist.analysis.ml.AutoMlEngine.ModelBenchmarkResult;
import com.autoscientist.analysis.xai.ExplainabilityEngine;
import com.autoscientist.dto.ml.FairnessDto;
import com.autoscientist.dto.ml.FeatureImportanceDto;
import com.autoscientist.entity.DatasetColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AutoMlEngineTest {

    private final AutoMlEngine autoMlEngine = new AutoMlEngine();
    private final ExplainabilityEngine explainabilityEngine = new ExplainabilityEngine();
    private final FairnessEngine fairnessEngine = new FairnessEngine();

    @Test
    @DisplayName("AutoML Regression Benchmark: Trains Ridge & Decision Tree, Ranks Correctly")
    void testRegressionBenchmark() {
        // Create synthetic regression dataset: y = 2*x0 + 0.5*x1
        int nTrain = 60, nVal = 20, nTest = 20;
        double[][] xTrain = new double[nTrain][2];
        double[] yTrain = new double[nTrain];
        Random rnd = new Random(123);

        for (int i = 0; i < nTrain; i++) {
            xTrain[i][0] = rnd.nextDouble() * 10;
            xTrain[i][1] = rnd.nextDouble() * 5;
            yTrain[i] = 2.0 * xTrain[i][0] + 0.5 * xTrain[i][1] + (rnd.nextDouble() * 0.1);
        }

        double[][] xVal = new double[nVal][2];
        double[] yVal = new double[nVal];
        for (int i = 0; i < nVal; i++) {
            xVal[i][0] = rnd.nextDouble() * 10;
            xVal[i][1] = rnd.nextDouble() * 5;
            yVal[i] = 2.0 * xVal[i][0] + 0.5 * xVal[i][1];
        }

        double[][] xTest = new double[nTest][2];
        double[] yTest = new double[nTest];
        for (int i = 0; i < nTest; i++) {
            xTest[i][0] = rnd.nextDouble() * 10;
            xTest[i][1] = rnd.nextDouble() * 5;
            yTest[i] = 2.0 * xTest[i][0] + 0.5 * xTest[i][1];
        }

        DatasetSplit split = new DatasetSplit(
                xTrain, yTrain, xVal, yVal, xTest, yTest,
                List.of("feature_important", "feature_secondary"),
                "target_score", "REGRESSION",
                null, null, Map.of()
        );

        List<ModelBenchmarkResult> results = autoMlEngine.runAutoMlBenchmark(split);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.get(0).isBest());
        assertNotNull(results.get(0).getValMetrics());
        assertTrue(results.get(0).getValMetrics().get("r2") > 0.8, "R-squared should be high on clean linear data");

        // Explainability check
        List<FeatureImportanceDto> importances = explainabilityEngine.computeFeatureImportance(split, results.get(0));
        assertNotNull(importances);
        assertEquals(2, importances.size());
        assertEquals("feature_important", importances.get(0).getFeatureName(), "feature_important with weight 2.0 should rank #1");
        assertTrue(importances.get(0).getRelativePercentage() >= importances.get(1).getRelativePercentage());

        // Fairness check
        FairnessDto fairness = fairnessEngine.evaluateFairness(List.of(), split, results.get(0), null);
        assertNotNull(fairness);
        assertEquals("NOT_APPLICABLE", fairness.getDisparateImpactStatus());
    }

    @Test
    @DisplayName("AutoML Classification Benchmark: Trains Logistic Regression & Random Forest")
    void testClassificationBenchmark() {
        int nTrain = 60, nVal = 20, nTest = 20;
        double[][] xTrain = new double[nTrain][2];
        double[] yTrain = new double[nTrain];
        Random rnd = new Random(456);

        for (int i = 0; i < nTrain; i++) {
            xTrain[i][0] = rnd.nextDouble() * 10 - 5;
            xTrain[i][1] = rnd.nextDouble() * 10 - 5;
            yTrain[i] = (xTrain[i][0] + xTrain[i][1] > 0) ? 1.0 : 0.0;
        }

        double[][] xVal = new double[nVal][2];
        double[] yVal = new double[nVal];
        for (int i = 0; i < nVal; i++) {
            xVal[i][0] = rnd.nextDouble() * 10 - 5;
            xVal[i][1] = rnd.nextDouble() * 10 - 5;
            yVal[i] = (xVal[i][0] + xVal[i][1] > 0) ? 1.0 : 0.0;
        }

        double[][] xTest = new double[nTest][2];
        double[] yTest = new double[nTest];
        for (int i = 0; i < nTest; i++) {
            xTest[i][0] = rnd.nextDouble() * 10 - 5;
            xTest[i][1] = rnd.nextDouble() * 10 - 5;
            yTest[i] = (xTest[i][0] + xTest[i][1] > 0) ? 1.0 : 0.0;
        }

        DatasetSplit split = new DatasetSplit(
                xTrain, yTrain, xVal, yVal, xTest, yTest,
                List.of("signal_x", "signal_y"),
                "pass_fail", "CLASSIFICATION",
                Map.of("0", 0, "1", 1), Map.of(0, "0", 1, "1"), Map.of()
        );

        List<ModelBenchmarkResult> results = autoMlEngine.runAutoMlBenchmark(split);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.get(0).isBest());
        assertNotNull(results.get(0).getValMetrics().get("accuracy"));
        assertTrue(results.get(0).getValMetrics().get("accuracy") >= 0.7);

        // Test Sensitive column fairness
        DatasetColumn sensitiveCol = DatasetColumn.builder()
                .columnName("gender")
                .dataType("CATEGORICAL")
                .build();

        FairnessDto fairness = fairnessEngine.evaluateFairness(List.of(sensitiveCol), split, results.get(0), "gender");
        assertNotNull(fairness);
        assertNotNull(fairness.getDisparateImpactStatus());
    }
}
