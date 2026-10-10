# AutoScientist AI — Technical Defense & Viva Examination Guide

## 1. Executive Project Summary (30-Second Elevator Pitch)
> "AutoScientist AI is an autonomous empirical data science and scientific discovery platform. It transforms raw tabular datasets into verifiable, evidence-based scientific findings. Unlike naive LLM wrappers that hallucinate statistical metrics, AutoScientist AI executes deterministic mathematical computations—including Pearson, Welch's t-test, ANOVA, and Chi-Square with aligned paired observation rules, leak-free AutoML with pure Java algorithms (Ridge, CART Trees, Random Forest), Permutation Explainability (XAI), Algorithmic Fairness auditing under the Four-Fifths rule, Population Stability Index drift tracking, and vector PDF report generation with OpenPDF—all backed by a DAG provenance evidence graph and strict causality disclaimers."

---

## 2. Key Architecture & Engineering Decisions

### Question: Why build a pure Java backend instead of Python (FastAPI/pandas/scikit-learn)?
- **Enterprise Robustness & Type Safety**: Java 17/21 provides strong static typing, memory predictability, and multi-threading execution without the Python Global Interpreter Lock (GIL).
- **Zero Hallucination Pipeline**: Building the statistical and ML algorithms natively with Apache Commons Math guarantees that all mathematical assumptions (aligned paired deletion, degrees of freedom, zero variance checks) are strictly enforced in deterministic code.
- **Enterprise Monorepo Architecture**: Clean separation between Spring Boot 3 REST APIs, Spring Security 6 JWT authentication, Hibernate JPA entities, and React 18 / Vite frontend.

### Question: How does AutoScientist AI prevent Data Leakage in AutoML?
- **Strict 70/15/15 Split-First Pipeline**: Data is partitioned into Train, Validation, and Test sets before any transformation is applied.
- **Train-Fitted Imputation & Scaling**: Imputation medians and z-score standardizers ($\mu, \sigma$) are computed exclusively on the 70% training split, preventing validation and test information from bleeding into training.
- **Independent Validation Assessment**: Validation sets are used for hyperparameter selection and Permutation Feature Importance, while the Test set is kept untouched until final evaluation.

### Question: What is Aligned Paired Deletion and why does it matter?
- When evaluating the correlation between variables $X$ and $Y$, if an analyst filters missing values in $X$ and then independently filters missing values in $Y$, the remaining values lose their observation alignment ($X_i$ no longer corresponds to $Y_i$).
- AutoScientist AI enforces **joint pair alignment**: if either $X_i$ or $Y_i$ is missing, the entire pair $(X_i, Y_i)$ is eliminated, preserving covariance integrity.

### Question: How is Algorithmic Fairness Audited?
- The system evaluates the **Demographic Parity Ratio (DPR)** and the **EEOC Four-Fifths (80%) Rule**:
  $$\text{Disparate Impact Ratio} = \frac{\min_g \text{SelectionRate}_g}{\max_g \text{SelectionRate}_g}$$
- If the ratio is $\ge 0.80$, the model passes; if $< 0.80$, the system flags disparate impact and emits actionable ethical guidance.

### Question: How does Data Drift Detection work?
- The system computes the **Population Stability Index (PSI)** using 10 quantile bins derived from baseline reference data.
- Proportions $P_i$ and $Q_i$ are compared with Laplace smoothing ($\epsilon = 10^{-4}$):
  $$\text{PSI} = \sum_{i=1}^{10} (Q_i - P_i) \cdot \ln(Q_i / P_i)$$
- Coupled with a two-sample Welch t-test for distribution shift, it categorizes features into `NO_DRIFT`, `MODERATE_DRIFT`, or `SIGNIFICANT_DRIFT`.

### Question: Why are Causality Disclaimers strictly mandated?
- Correlation does not imply causation. Observational data is susceptible to unmeasured confounding, selection bias, and reverse causality.
- AutoScientist AI attaches explicit causality notices to all findings and reports, advising researchers that causal claims require randomized controlled trials (RCTs) or formal causal graphical identification.

---

## 3. Demonstration Script (Step-by-Step Viva Walkthrough)
1. **User Authentication**: Register/Login via JWT authentication.
2. **Dataset Ingestion**: Upload `test-data/student_performance.csv`; observe RFC-4180 parsing and storage path traversal protection.
3. **Automated Profiling & Quality Audit**: Show inferred types (NUMERIC vs CATEGORICAL), mean/median/std dev, and 0-100 quality score.
4. **Bivariate Associations**: Click "[Find Associations]" to display Pearson ($r=0.985, p < 10^{-6}$) and Welch t-tests.
5. **Research Questions & Hypotheses**: Formulate research question; show automatic $H_0$ and $H_1$ generation.
6. **AutoML & Explainability**: Run AutoML benchmark (Ridge SVD, Trees, Random Forest); inspect validation metrics and Permutation Feature Importance bar chart.
7. **What-If Simulator**: Adjust feature values in real-time to simulate predictions and view marginal impact deltas.
8. **Cohort Drift Analysis**: Compare dataset cohorts; view PSI metrics and stability diagnostics.
9. **Ask Dataset**: Enter natural language query; inspect exact mathematical evidence returned without hallucinations.
10. **Scientific Report & Vector PDF Export**: Generate comprehensive Markdown report and download publication-quality PDF via OpenPDF.
