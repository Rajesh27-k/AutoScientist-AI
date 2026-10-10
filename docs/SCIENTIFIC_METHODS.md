# AutoScientist AI — Scientific & Statistical Methods Reference

## 1. Philosophical Foundation: Empirical Rigor & Provenance
AutoScientist AI transforms raw tabular datasets into evidence-based scientific findings without fabrication, hallucinations, or heuristic column assumptions. All metrics, test statistics, p-values, effect sizes, and predictive models are calculated deterministically on real data.

---

## 2. Statistical Testing Engine Formulations

### 2.1 Aligned Paired Deletion Rule
A core statistical requirement enforced in AutoScientist AI is **strictly aligned paired observation filtering**. When evaluating two variables $X$ and $Y$:
- Paired deletion is evaluated jointly: sample $i$ is dropped if and only if $X_i$ is null/empty **OR** $Y_i$ is null/empty.
- **NEVER** independently filter $X$ and $Y$, as this breaks row alignment and destroys observation-level covariance.
- If $N < 3$ or either variable exhibits zero variance ($\sigma^2 = 0$), the engine halts with an informative non-significant result rather than throwing a division-by-zero error.

### 2.2 Continuous vs. Continuous: Pearson Correlation ($r$)
For paired continuous observations $(X_i, Y_i)_{i=1}^N$:
$$\bar{X} = \frac{1}{N}\sum_{i=1}^N X_i, \quad \bar{Y} = \frac{1}{N}\sum_{i=1}^N Y_i$$
$$r = \frac{\sum_{i=1}^N (X_i - \bar{X})(Y_i - \bar{Y})}{\sqrt{\sum_{i=1}^N (X_i - \bar{X})^2 \sum_{i=1}^N (Y_i - \bar{Y})^2}}$$
- **Test Statistic**:
  $$t = r \sqrt{\frac{N - 2}{1 - r^2}}, \quad df = N - 2$$
- **p-value**: Evaluated via two-tailed Student's t-distribution:
  $$p = 2 \cdot (1 - F_t(|t|; df))$$
- **Effect Size**: Coefficient of determination $r^2 \in [0, 1]$ representing shared variance proportion.

### 2.3 Continuous vs. Binary Categorical: Welch's t-Test
Applied to compare continuous values between two distinct groups $G_1$ and $G_2$ without assuming equal variances (avoiding Student's t-test homoscedasticity bias):
$$t = \frac{\bar{X}_1 - \bar{X}_2}{\sqrt{\frac{s_1^2}{n_1} + \frac{s_2^2}{n_2}}}$$
- **Welch–Satterthwaite Degrees of Freedom**:
  $$\nu = \frac{\left(\frac{s_1^2}{n_1} + \frac{s_2^2}{n_2}\right)^2}{\frac{(s_1^2 / n_1)^2}{n_1 - 1} + \frac{(s_2^2 / n_2)^2}{n_2 - 1}}$$
- **Effect Size (Cohen's $d$)**:
  $$s_{\text{pooled}} = \sqrt{\frac{(n_1 - 1)s_1^2 + (n_2 - 1)s_2^2}{n_1 + n_2 - 2}}, \quad d = \frac{|\bar{X}_1 - \bar{X}_2|}{s_{\text{pooled}}}$$
  - Small: $d \approx 0.2$, Medium: $d \approx 0.5$, Large: $d \ge 0.8$.

### 2.4 Continuous vs. Multi-Group Categorical: One-Way ANOVA
Applied to compare continuous distributions across $k \ge 3$ categorical levels:
$$SS_{\text{total}} = \sum_{j=1}^k \sum_{i=1}^{n_j} (X_{ij} - \bar{X})^2$$
$$SS_{\text{between}} = \sum_{j=1}^k n_j (\bar{X}_j - \bar{X})^2, \quad SS_{\text{within}} = \sum_{j=1}^k \sum_{i=1}^{n_j} (X_{ij} - \bar{X}_j)^2$$
- **F-Statistic**:
  $$F = \frac{MS_{\text{between}}}{MS_{\text{within}}} = \frac{SS_{\text{between}} / (k - 1)}{SS_{\text{within}} / (N - k)}$$
- **p-value**: Evaluated using Snedecor's F-distribution with $df_1 = k - 1$ and $df_2 = N - k$.
- **Effect Size ($\eta^2$, Eta-Squared)**:
  $$\eta^2 = \frac{SS_{\text{between}}}{SS_{\text{total}}}$$

### 2.5 Categorical vs. Categorical: Pearson's $\chi^2$ Test of Independence
For an $R \times C$ contingency table with observed counts $O_{ij}$ and expected frequencies $E_{ij} = \frac{R_i \cdot C_j}{N}$:
$$\chi^2 = \sum_{i=1}^R \sum_{j=1}^C \frac{(O_{ij} - E_{ij})^2}{E_{ij}}, \quad df = (R - 1)(C - 1)$$
- **Effect Size (Cramér's $V$)**:
  $$V = \sqrt{\frac{\chi^2}{N \cdot \min(R - 1, C - 1)}}$$

---

## 3. Machine Learning & Preprocessing Methodology

### 3.1 Leak-Free 70/15/15 Data Partitioning
Data leakage is rigorously avoided by adhering to three rules:
1. **Splits Defined First**: Dataset rows are divided into 70% Train, 15% Validation, and 15% Test.
2. **Train-Only Imputation**: Numeric medians and categorical modes are estimated solely on the training split, then applied to validation and test partitions.
3. **Train-Only Standardization**: Z-score scaling parameters ($\mu_{\text{train}}, \sigma_{\text{train}}$) are computed exclusively on training data:
   $$z = \frac{x - \mu_{\text{train}}}{\sigma_{\text{train}}}$$

### 3.2 Pure Java Model Implementations
- **Ridge Regression**: Regularized SVD closed-form solver solving $(X^T X + \lambda N I)w = X^T y$.
- **CART Decision Trees**: Recursive binary splitting using variance reduction (regression) or Gini impurity (classification).
- **L2 Logistic Regression**: Mini-batch / gradient descent with sigmoid activation $P(y=1|x) = \frac{1}{1 + e^{-w^T x}}$.
- **Random Forest Classifier**: Bootstrap aggregating (bagging) of decorrelated decision trees.

---

## 4. Explainability (XAI) & Algorithmic Fairness

### 4.1 Permutation Feature Importance
Measures true generalization importance on held-out validation data ($X_{\text{val}}, y_{\text{val}}$):
1. Compute baseline validation performance score $S_{\text{base}}$ ($R^2$ or Macro-F1).
2. For each feature column $j \in \{1, \dots, p\}$:
   - Permute values of column $j$ using Fisher-Yates shuffle with fixed seed.
   - Evaluate model on permuted matrix: $S_{\text{perm}, j}$.
   - Importance: $\Delta_j = \max(0, S_{\text{base}} - S_{\text{perm}, j})$.
3. Normalize relative contribution:
   $$\text{ImportancePct}_j = \frac{\Delta_j}{\sum_{k=1}^p \Delta_k} \times 100\%$$

### 4.2 Algorithmic Fairness & Disparate Impact
Assesses prediction consistency across sensitive demographic attributes (e.g. sex, age bracket, group):
- **Demographic Parity Ratio**:
  $$\text{DPR} = \frac{\min_{g} P(\hat{Y}=1 | A=g)}{\max_{g} P(\hat{Y}=1 | A=g)}$$
- **EEOC Four-Fifths (80%) Rule**:
  - If $\text{DPR} \ge 0.80 \rightarrow$ **PASS** (Minimal disparate impact).
  - If $\text{DPR} < 0.80 \rightarrow$ **FLAGGED** (Potential disparate impact requiring mitigation).

---

## 5. Cohort Stability & Population Stability Index (PSI)
Used to detect data drift between baseline training cohorts and production/target datasets:
1. Divide continuous reference variable into 10 quantile bins: $B_1, \dots, B_{10}$.
2. Compute sample proportions $P_i$ (reference) and $Q_i$ (target) with Laplace smoothing ($\epsilon = 10^{-4}$).
3. **Population Stability Index**:
   $$\text{PSI} = \sum_{i=1}^{10} (Q_i - P_i) \cdot \ln\left(\frac{Q_i}{P_i}\right)$$
- $\text{PSI} < 0.10$: **NO_DRIFT** (Stable).
- $0.10 \le \text{PSI} < 0.25$: **MODERATE_DRIFT** (Monitor feature).
- $\text{PSI} \ge 0.25$: **SIGNIFICANT_DRIFT** (Retraining required).

---

## 6. Strict Scientific Causality Disclaimer
Every finding and report generated by AutoScientist AI enforces the following epistemological notice:
> **CAUSALITY DISCLAIMER**: Statistical significance ($p < 0.05$) and strong mathematical associations ($r \ne 0$) demonstrate observational correlation, **NOT** causal influence. Confounding variables, reverse causality, and selection bias cannot be ruled out without randomized controlled experiments (RCTs) or formal causal DAG identification.
