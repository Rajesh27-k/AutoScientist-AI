package com.autoscientist.analysis.ml;

import com.autoscientist.analysis.feature.FeatureEngineeringEngine.DatasetSplit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AutoMlEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ModelBenchmarkResult {
        private final String modelName;
        private final String algorithm;
        private final Map<String, Object> hyperparameters;
        private final Map<String, Double> trainMetrics;
        private final Map<String, Double> valMetrics;
        private final Map<String, Double> testMetrics;
        private final boolean isBest;
        private final PredictiveModel trainedModel;

        public ModelBenchmarkResult(String modelName, String algorithm,
                                    Map<String, Object> hyperparameters,
                                    Map<String, Double> trainMetrics,
                                    Map<String, Double> valMetrics,
                                    Map<String, Double> testMetrics,
                                    boolean isBest,
                                    PredictiveModel trainedModel) {
            this.modelName = modelName;
            this.algorithm = algorithm;
            this.hyperparameters = hyperparameters;
            this.trainMetrics = trainMetrics;
            this.valMetrics = valMetrics;
            this.testMetrics = testMetrics;
            this.isBest = isBest;
            this.trainedModel = trainedModel;
        }

        public String getModelName() { return modelName; }
        public String getAlgorithm() { return algorithm; }
        public Map<String, Object> getHyperparameters() { return hyperparameters; }
        public Map<String, Double> getTrainMetrics() { return trainMetrics; }
        public Map<String, Double> getValMetrics() { return valMetrics; }
        public Map<String, Double> getTestMetrics() { return testMetrics; }
        public boolean isBest() { return isBest; }
        public PredictiveModel getTrainedModel() { return trainedModel; }
    }

    public interface PredictiveModel {
        double predictSingle(double[] x);
        double[] predict(double[][] X);
        double[] getWeights(); // for linear models
    }

    public List<ModelBenchmarkResult> runAutoMlBenchmark(DatasetSplit split) {
        String problemType = split.getProblemType();
        List<ModelBenchmarkResult> results = new ArrayList<>();

        if (problemType.equals("REGRESSION")) {
            // 1. OLS / Ridge Linear Regression
            ModelBenchmarkResult ridge = trainRidgeRegression(split, 0.1);
            results.add(ridge);

            // 2. Ridge with higher regularization
            ModelBenchmarkResult ridge2 = trainRidgeRegression(split, 1.0);
            results.add(ridge2);

            // 3. Decision Tree Regressor
            ModelBenchmarkResult dt = trainDecisionTreeRegressor(split, 4);
            results.add(dt);

            // Rank by validation R-squared descending
            results.sort((a, b) -> Double.compare(
                    b.getValMetrics().getOrDefault("r2", -999.0),
                    a.getValMetrics().getOrDefault("r2", -999.0)
            ));

        } else {
            // Classification
            // 1. Logistic Regression
            ModelBenchmarkResult logReg = trainLogisticRegression(split, 0.05, 300);
            results.add(logReg);

            // 2. Decision Tree Classifier
            ModelBenchmarkResult dt = trainDecisionTreeClassifier(split, 4);
            results.add(dt);

            // 3. Random Forest Classifier
            ModelBenchmarkResult rf = trainRandomForestClassifier(split, 10, 4);
            results.add(rf);

            // Rank by validation F1-score descending
            results.sort((a, b) -> Double.compare(
                    b.getValMetrics().getOrDefault("f1", 0.0),
                    a.getValMetrics().getOrDefault("f1", 0.0)
            ));
        }

        // Tag best model
        List<ModelBenchmarkResult> finalResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ModelBenchmarkResult res = results.get(i);
            finalResults.add(new ModelBenchmarkResult(
                    res.getModelName(),
                    res.getAlgorithm(),
                    res.getHyperparameters(),
                    res.getTrainMetrics(),
                    res.getValMetrics(),
                    res.getTestMetrics(),
                    i == 0, // Top model is best
                    res.getTrainedModel()
            ));
        }

        return finalResults;
    }

    // --- REGRESSION MODELS ---

    private ModelBenchmarkResult trainRidgeRegression(DatasetSplit split, double lambda) {
        int n = split.getXTrain().length;
        int p = split.getFeatureNames().size();

        // Solve (X^T X + lambda I) w = X^T y
        RealMatrix X = MatrixUtils.createRealMatrix(split.getXTrain());
        RealVector y = MatrixUtils.createRealVector(split.getYTrain());

        RealMatrix XtX = X.transpose().multiply(X);
        RealMatrix lambdaI = MatrixUtils.createRealIdentityMatrix(p).scalarMultiply(lambda * n);
        RealMatrix A = XtX.add(lambdaI);

        DecompositionSolver solver = new SingularValueDecomposition(A).getSolver();
        RealVector w = solver.solve(X.transpose().operate(y));
        double[] weights = w.toArray();

        PredictiveModel model = new PredictiveModel() {
            @Override
            public double predictSingle(double[] x) {
                double pred = 0;
                for (int i = 0; i < x.length; i++) pred += x[i] * weights[i];
                return pred;
            }
            @Override
            public double[] predict(double[][] X_in) {
                double[] preds = new double[X_in.length];
                for (int i = 0; i < X_in.length; i++) preds[i] = predictSingle(X_in[i]);
                return preds;
            }
            @Override
            public double[] getWeights() { return weights; }
        };

        Map<String, Object> params = new HashMap<>();
        params.put("regularization_lambda", lambda);
        params.put("solver", "SVD Ridge");

        Map<String, Double> trainMetrics = evalRegression(model.predict(split.getXTrain()), split.getYTrain());
        Map<String, Double> valMetrics = evalRegression(model.predict(split.getXVal()), split.getYVal());
        Map<String, Double> testMetrics = evalRegression(model.predict(split.getXTest()), split.getYTest());

        String name = String.format("Ridge Regression (lambda=%.2f)", lambda);
        return new ModelBenchmarkResult(name, "RidgeRegression", params, trainMetrics, valMetrics, testMetrics, false, model);
    }

    private ModelBenchmarkResult trainDecisionTreeRegressor(DatasetSplit split, int maxDepth) {
        DecisionTreeNode root = buildRegressionTree(split.getXTrain(), split.getYTrain(), 0, maxDepth);

        PredictiveModel model = new PredictiveModel() {
            @Override
            public double predictSingle(double[] x) {
                return predictRegressionTree(root, x);
            }
            @Override
            public double[] predict(double[][] X_in) {
                double[] preds = new double[X_in.length];
                for (int i = 0; i < X_in.length; i++) preds[i] = predictSingle(X_in[i]);
                return preds;
            }
            @Override
            public double[] getWeights() { return new double[0]; }
        };

        Map<String, Object> params = new HashMap<>();
        params.put("max_depth", maxDepth);
        params.put("criterion", "variance_reduction");

        Map<String, Double> trainMetrics = evalRegression(model.predict(split.getXTrain()), split.getYTrain());
        Map<String, Double> valMetrics = evalRegression(model.predict(split.getXVal()), split.getYVal());
        Map<String, Double> testMetrics = evalRegression(model.predict(split.getXTest()), split.getYTest());

        return new ModelBenchmarkResult("Decision Tree Regressor", "DecisionTreeRegressor", params,
                trainMetrics, valMetrics, testMetrics, false, model);
    }

    // --- CLASSIFICATION MODELS ---

    private ModelBenchmarkResult trainLogisticRegression(DatasetSplit split, double lr, int epochs) {
        double[][] X = split.getXTrain();
        double[] y = split.getYTrain();
        int n = X.length;
        int p = split.getFeatureNames().size();

        double[] weights = new double[p];
        double bias = 0.0;

        for (int ep = 0; ep < epochs; ep++) {
            double[] gradW = new double[p];
            double gradB = 0.0;

            for (int i = 0; i < n; i++) {
                double z = bias;
                for (int j = 0; j < p; j++) z += weights[j] * X[i][j];
                double pred = sigmoid(z);
                double err = pred - y[i];

                for (int j = 0; j < p; j++) gradW[j] += err * X[i][j];
                gradB += err;
            }

            for (int j = 0; j < p; j++) {
                weights[j] -= lr * (gradW[j] / n + 0.01 * weights[j]); // L2 regularization
            }
            bias -= lr * (gradB / n);
        }

        final double finalBias = bias;
        PredictiveModel model = new PredictiveModel() {
            @Override
            public double predictSingle(double[] x) {
                double z = finalBias;
                for (int j = 0; j < x.length; j++) z += weights[j] * x[j];
                return sigmoid(z) >= 0.5 ? 1.0 : 0.0;
            }
            @Override
            public double[] predict(double[][] X_in) {
                double[] preds = new double[X_in.length];
                for (int i = 0; i < X_in.length; i++) preds[i] = predictSingle(X_in[i]);
                return preds;
            }
            @Override
            public double[] getWeights() { return weights; }
        };

        Map<String, Object> params = new HashMap<>();
        params.put("learning_rate", lr);
        params.put("epochs", epochs);
        params.put("l2_penalty", 0.01);

        Map<String, Double> trainMetrics = evalClassification(model.predict(split.getXTrain()), split.getYTrain());
        Map<String, Double> valMetrics = evalClassification(model.predict(split.getXVal()), split.getYVal());
        Map<String, Double> testMetrics = evalClassification(model.predict(split.getXTest()), split.getYTest());

        return new ModelBenchmarkResult("Logistic Regression (L2)", "LogisticRegression", params,
                trainMetrics, valMetrics, testMetrics, false, model);
    }

    private ModelBenchmarkResult trainDecisionTreeClassifier(DatasetSplit split, int maxDepth) {
        DecisionTreeNode root = buildClassificationTree(split.getXTrain(), split.getYTrain(), 0, maxDepth);

        PredictiveModel model = new PredictiveModel() {
            @Override
            public double predictSingle(double[] x) {
                return predictClassificationTree(root, x);
            }
            @Override
            public double[] predict(double[][] X_in) {
                double[] preds = new double[X_in.length];
                for (int i = 0; i < X_in.length; i++) preds[i] = predictSingle(X_in[i]);
                return preds;
            }
            @Override
            public double[] getWeights() { return new double[0]; }
        };

        Map<String, Object> params = new HashMap<>();
        params.put("max_depth", maxDepth);
        params.put("criterion", "gini_impurity");

        Map<String, Double> trainMetrics = evalClassification(model.predict(split.getXTrain()), split.getYTrain());
        Map<String, Double> valMetrics = evalClassification(model.predict(split.getXVal()), split.getYVal());
        Map<String, Double> testMetrics = evalClassification(model.predict(split.getXTest()), split.getYTest());

        return new ModelBenchmarkResult("Decision Tree Classifier", "DecisionTreeClassifier", params,
                trainMetrics, valMetrics, testMetrics, false, model);
    }

    private ModelBenchmarkResult trainRandomForestClassifier(DatasetSplit split, int numTrees, int maxDepth) {
        List<DecisionTreeNode> forest = new ArrayList<>();
        Random rand = new Random(42);
        int n = split.getXTrain().length;

        for (int t = 0; t < numTrees; t++) {
            // Bootstrap sample
            double[][] subX = new double[n][];
            double[] subY = new double[n];
            for (int i = 0; i < n; i++) {
                int pick = rand.nextInt(n);
                subX[i] = split.getXTrain()[pick];
                subY[i] = split.getYTrain()[pick];
            }
            forest.add(buildClassificationTree(subX, subY, 0, maxDepth));
        }

        PredictiveModel model = new PredictiveModel() {
            @Override
            public double predictSingle(double[] x) {
                int vote1 = 0;
                for (DecisionTreeNode tree : forest) {
                    if (predictClassificationTree(tree, x) >= 0.5) vote1++;
                }
                return (vote1 >= (forest.size() / 2.0)) ? 1.0 : 0.0;
            }
            @Override
            public double[] predict(double[][] X_in) {
                double[] preds = new double[X_in.length];
                for (int i = 0; i < X_in.length; i++) preds[i] = predictSingle(X_in[i]);
                return preds;
            }
            @Override
            public double[] getWeights() { return new double[0]; }
        };

        Map<String, Object> params = new HashMap<>();
        params.put("n_estimators", numTrees);
        params.put("max_depth", maxDepth);

        Map<String, Double> trainMetrics = evalClassification(model.predict(split.getXTrain()), split.getYTrain());
        Map<String, Double> valMetrics = evalClassification(model.predict(split.getXVal()), split.getYVal());
        Map<String, Double> testMetrics = evalClassification(model.predict(split.getXTest()), split.getYTest());

        return new ModelBenchmarkResult(String.format("Random Forest (%d Trees)", numTrees), "RandomForestClassifier", params,
                trainMetrics, valMetrics, testMetrics, false, model);
    }

    // --- DECISION TREE DATA STRUCTURE & LOGIC ---

    private static class DecisionTreeNode {
        boolean isLeaf;
        double value;
        int featureIndex;
        double threshold;
        DecisionTreeNode left;
        DecisionTreeNode right;
    }

    private DecisionTreeNode buildRegressionTree(double[][] X, double[] y, int depth, int maxDepth) {
        DecisionTreeNode node = new DecisionTreeNode();
        if (X.length == 0) return node;

        double mean = 0;
        for (double v : y) mean += v;
        mean /= y.length;

        if (depth >= maxDepth || X.length <= 3) {
            node.isLeaf = true;
            node.value = mean;
            return node;
        }

        int bestFeat = -1;
        double bestThresh = 0;
        double bestVarRed = -1;

        int p = X[0].length;
        double currentVar = calcVariance(y);

        for (int j = 0; j < p; j++) {
            for (int i = 0; i < X.length; i++) {
                double thresh = X[i][j];
                List<Double> leftY = new ArrayList<>();
                List<Double> rightY = new ArrayList<>();

                for (int k = 0; k < X.length; k++) {
                    if (X[k][j] <= thresh) leftY.add(y[k]);
                    else rightY.add(y[k]);
                }

                if (leftY.size() < 1 || rightY.size() < 1) continue;

                double varL = calcVarianceList(leftY);
                double varR = calcVarianceList(rightY);
                double weightedVar = ((double) leftY.size() / y.length * varL) + ((double) rightY.size() / y.length * varR);
                double varRed = currentVar - weightedVar;

                if (varRed > bestVarRed) {
                    bestVarRed = varRed;
                    bestFeat = j;
                    bestThresh = thresh;
                }
            }
        }

        if (bestFeat == -1 || bestVarRed <= 0) {
            node.isLeaf = true;
            node.value = mean;
            return node;
        }

        node.featureIndex = bestFeat;
        node.threshold = bestThresh;

        List<double[]> leftX = new ArrayList<>();
        List<Double> leftY = new ArrayList<>();
        List<double[]> rightX = new ArrayList<>();
        List<Double> rightY = new ArrayList<>();

        for (int i = 0; i < X.length; i++) {
            if (X[i][bestFeat] <= bestThresh) {
                leftX.add(X[i]);
                leftY.add(y[i]);
            } else {
                rightX.add(X[i]);
                rightY.add(y[i]);
            }
        }

        node.left = buildRegressionTree(leftX.toArray(new double[0][]), toArray(leftY), depth + 1, maxDepth);
        node.right = buildRegressionTree(rightX.toArray(new double[0][]), toArray(rightY), depth + 1, maxDepth);
        return node;
    }

    private DecisionTreeNode buildClassificationTree(double[][] X, double[] y, int depth, int maxDepth) {
        DecisionTreeNode node = new DecisionTreeNode();
        if (X.length == 0) return node;

        int count1 = 0;
        for (double v : y) if (v >= 0.5) count1++;
        double majority = (count1 >= (y.length / 2.0)) ? 1.0 : 0.0;

        if (depth >= maxDepth || X.length <= 3 || count1 == 0 || count1 == y.length) {
            node.isLeaf = true;
            node.value = majority;
            return node;
        }

        int bestFeat = -1;
        double bestThresh = 0;
        double bestGiniGain = -1;
        double currentGini = calcGini(y);

        int p = X[0].length;
        for (int j = 0; j < p; j++) {
            for (int i = 0; i < X.length; i++) {
                double thresh = X[i][j];
                List<Double> leftY = new ArrayList<>();
                List<Double> rightY = new ArrayList<>();

                for (int k = 0; k < X.length; k++) {
                    if (X[k][j] <= thresh) leftY.add(y[k]);
                    else rightY.add(y[k]);
                }

                if (leftY.size() < 1 || rightY.size() < 1) continue;

                double giniL = calcGiniList(leftY);
                double giniR = calcGiniList(rightY);
                double weightedGini = ((double) leftY.size() / y.length * giniL) + ((double) rightY.size() / y.length * giniR);
                double gain = currentGini - weightedGini;

                if (gain > bestGiniGain) {
                    bestGiniGain = gain;
                    bestFeat = j;
                    bestThresh = thresh;
                }
            }
        }

        if (bestFeat == -1 || bestGiniGain <= 0) {
            node.isLeaf = true;
            node.value = majority;
            return node;
        }

        node.featureIndex = bestFeat;
        node.threshold = bestThresh;

        List<double[]> leftX = new ArrayList<>();
        List<Double> leftY = new ArrayList<>();
        List<double[]> rightX = new ArrayList<>();
        List<Double> rightY = new ArrayList<>();

        for (int i = 0; i < X.length; i++) {
            if (X[i][bestFeat] <= bestThresh) {
                leftX.add(X[i]);
                leftY.add(y[i]);
            } else {
                rightX.add(X[i]);
                rightY.add(y[i]);
            }
        }

        node.left = buildClassificationTree(leftX.toArray(new double[0][]), toArray(leftY), depth + 1, maxDepth);
        node.right = buildClassificationTree(rightX.toArray(new double[0][]), toArray(rightY), depth + 1, maxDepth);
        return node;
    }

    private double predictRegressionTree(DecisionTreeNode node, double[] x) {
        if (node == null) return 0.0;
        if (node.isLeaf) return node.value;
        if (x[node.featureIndex] <= node.threshold) return predictRegressionTree(node.left, x);
        return predictRegressionTree(node.right, x);
    }

    private double predictClassificationTree(DecisionTreeNode node, double[] x) {
        if (node == null) return 0.0;
        if (node.isLeaf) return node.value;
        if (x[node.featureIndex] <= node.threshold) return predictClassificationTree(node.left, x);
        return predictClassificationTree(node.right, x);
    }

    // --- EVALUATION METRICS ---

    private Map<String, Double> evalRegression(double[] preds, double[] actuals) {
        int n = Math.min(preds.length, actuals.length);
        if (n == 0) return Collections.emptyMap();

        double sse = 0;
        double sae = 0;
        double actualSum = 0;

        for (int i = 0; i < n; i++) {
            double err = preds[i] - actuals[i];
            sse += err * err;
            sae += Math.abs(err);
            actualSum += actuals[i];
        }

        double mse = sse / n;
        double rmse = Math.sqrt(mse);
        double mae = sae / n;
        double meanY = actualSum / n;

        double sst = 0;
        for (int i = 0; i < n; i++) {
            sst += Math.pow(actuals[i] - meanY, 2);
        }

        double r2 = (sst > 0) ? 1.0 - (sse / sst) : 0.0;

        Map<String, Double> map = new LinkedHashMap<>();
        map.put("r2", round(r2, 4));
        map.put("rmse", round(rmse, 4));
        map.put("mae", round(mae, 4));
        map.put("mse", round(mse, 4));
        return map;
    }

    private Map<String, Double> evalClassification(double[] preds, double[] actuals) {
        int n = Math.min(preds.length, actuals.length);
        if (n == 0) return Collections.emptyMap();

        int tp = 0, fp = 0, fn = 0, tn = 0;
        for (int i = 0; i < n; i++) {
            boolean p = preds[i] >= 0.5;
            boolean a = actuals[i] >= 0.5;
            if (p && a) tp++;
            else if (p && !a) fp++;
            else if (!p && a) fn++;
            else tn++;
        }

        double accuracy = (double) (tp + tn) / n;
        double precision = (tp + fp > 0) ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn > 0) ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall > 0) ? 2.0 * (precision * recall) / (precision + recall) : 0.0;

        Map<String, Double> map = new LinkedHashMap<>();
        map.put("accuracy", round(accuracy, 4));
        map.put("precision", round(precision, 4));
        map.put("recall", round(recall, 4));
        map.put("f1", round(f1, 4));
        map.put("true_positives", (double) tp);
        map.put("false_positives", (double) fp);
        map.put("false_negatives", (double) fn);
        map.put("true_negatives", (double) tn);
        return map;
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-Math.max(-20.0, Math.min(20.0, z))));
    }

    private double calcVariance(double[] y) {
        if (y.length < 2) return 0.0;
        double mean = 0;
        for (double v : y) mean += v;
        mean /= y.length;
        double ss = 0;
        for (double v : y) ss += Math.pow(v - mean, 2);
        return ss / (y.length - 1);
    }

    private double calcVarianceList(List<Double> y) {
        if (y.size() < 2) return 0.0;
        double mean = 0;
        for (double v : y) mean += v;
        mean /= y.size();
        double ss = 0;
        for (double v : y) ss += Math.pow(v - mean, 2);
        return ss / (y.size() - 1);
    }

    private double calcGini(double[] y) {
        if (y.length == 0) return 0.0;
        int count1 = 0;
        for (double v : y) if (v >= 0.5) count1++;
        double p1 = (double) count1 / y.length;
        double p0 = 1.0 - p1;
        return 1.0 - (p1 * p1 + p0 * p0);
    }

    private double calcGiniList(List<Double> y) {
        if (y.isEmpty()) return 0.0;
        int count1 = 0;
        for (double v : y) if (v >= 0.5) count1++;
        double p1 = (double) count1 / y.size();
        double p0 = 1.0 - p1;
        return 1.0 - (p1 * p1 + p0 * p0);
    }

    private double[] toArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private double round(double val, int places) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        double factor = Math.pow(10, places);
        return Math.round(val * factor) / factor;
    }
}
