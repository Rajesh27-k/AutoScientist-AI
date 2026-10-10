# AutoScientist AI — Final System Audit Report



**Date & Time**: 2026-10-08T22:30:00Z

**Audit Scope**: Full end-to-end trace from React Page → Frontend Service → REST Endpoint → Controller → Service → Analysis Engine → Database/File Storage → DTO → Frontend State → UI.



---



## 1. System-Wide Feature Audit Matrix



| Feature | Backend Component | Frontend Component | REST API Endpoint(s) | Integration | Runtime | Tests | Status | Known Issue(s) |

| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |

| **Authentication & Auth Context** | `AuthController`, `AuthService`, `JwtUtils`, `User` | `LoginPage`, `RegisterPage`, `AuthContext` | `POST /api/auth/login`<br>`POST /api/auth/register` | Fully connected, stores JWT token & user | Online | Passing (`AuthControllerTest`, `AuthServiceTest`) | **WORKING** | None. Session persistence and guards functional. |

| **Dataset Ingestion & Storage** | `DatasetController`, `DatasetService`, `FileStorageService`, `CsvParser` | `DatasetsPage`, `datasetService` | `POST /api/datasets/upload`<br>`GET /api/datasets`<br>`DELETE /api/datasets/{id}` | Form-data upload, file persistence in `/uploads`, metadata in DB | Online | Passing (`DatasetProfilerTest`) | **WORKING** | `datasetService.getDatasets()` alias missing (named `listDatasets`). |

| **Dataset Understanding & Profiling** | `DatasetController`, `DatasetProfiler`, `DatasetColumn` | `DatasetDetailPage` (Stage 2: Overview) | `GET /api/datasets/{id}` | Deserializes columns, null-counts, cardinality, stats | Online | Passing (`DatasetProfilerTest`) | **PARTIALLY WORKING** | Identifier columns (`customer_id`, `id`, `user_id`) not yet tagged as `IDENTIFIER` type. |

| **Data Quality Audit (100-Point)** | `DataQualityEngine`, `DataQualityReport`, `DataQualityIssue` | `DatasetDetailPage` (Stage 3: Quality) | `GET /api/datasets/{id}` (included in detail DTO) | Quality scores & structural issues rendered | Online | Passing (`DataQualityEngineTest`) | **WORKING** | None. Missing, duplicate, outlier, and consistency subscores computed. |

| **Automated EDA (Distributions & Correlations)** | `EdaController`, `EdaService`, `EdaEngine`, `StatisticalTestEngine` | `DatasetDetailPage` (Stage 4: EDA), `VisualizationsPage` | `GET /api/datasets/{id}/eda/correlations`<br>`GET /api/datasets/{id}/eda/boxplots`<br>`GET /api/datasets/{id}/eda/grouped` | Computes Pearson correlation matrix & box plot distributions | Online | Passing (`EndToEndPipelineTest`) | **PARTIALLY WORKING** | High-cardinality identifier columns (e.g. `customer_id`) not excluded from default correlation/box plot matrices. |

| **Bivariate Association Engine** | `AssociationController`, `AssociationService`, `AssociationEngine` | `AssociationsPage` (Stage 5) | `GET /api/datasets/{id}/associations`<br>`POST /api/datasets/{id}/associations/refresh` | Tests Pearson, Welch t-test, ANOVA, Chi-Square with strict paired deletion | Online | Passing (`StatisticalTestEngineTest`) | **PARTIALLY WORKING** | Needs default exclusion for identifier columns; investigation deep-links need clean variable pre-selection. |

| **Research Question Formulation** | `ResearchQuestionController`, `ResearchService`, `ResearchQuestion` | `ResearchWorkflowPage` (Stage 6) | `POST /api/datasets/{id}/research/questions`<br>`GET /api/datasets/{id}/research/questions` | Auto-generates formal scientific questions from significant associations | Online | Passing (`QuestionAnalysisEngineTest`) | **PARTIALLY WORKING** | Default question pre-fill can select ID columns if first column is `customer_id`. |

| **Hypothesis Generation** | `ResearchQuestionController`, `ResearchService`, `Hypothesis` | `ResearchWorkflowPage` (Stage 7) | Included in Research Question creation | Generates mathematical $H_0$ and $H_a$ with rationale and suggested statistical test | Online | Passing (`QuestionAnalysisEngineTest`) | **WORKING** | None. Formal null/alternative hypotheses correctly generated. |

| **Statistical Testing & Decisions** | `ResearchQuestionController`, `ResearchService`, `StatisticalTestEngine` | `ResearchWorkflowPage` (Stage 8) | `POST /api/research/hypotheses/{id}/test` | Evaluates hypothesis against raw data, produces test statistic, p-value, effect size, and decision | Online | Passing (`StatisticalTestEngineTest`) | **WORKING** | None. REJECT vs FAIL TO REJECT decisions strictly computed. |

| **AutoML Benchmarking** | `AutoMlController`, `AutoMlService`, `AutoMlEngine`, `FeatureEngineeringEngine` | `AutoMlPage` (Stage 9: ML) | `POST /api/datasets/{id}/automl/train`<br>`GET /api/datasets/{id}/automl/latest` | Benchmarks Ridge/OLS, Decision Tree, Logistic Regression, Random Forest on 70/15/15 splits | Online | Passing (`AutoMlEngineTest`, `BiomedicalClassificationPipelineTest`) | **BROKEN** | Frontend called non-existent `datasetService.getDatasets()`, resulting in "Failed to load datasets." error banner; did not load active dataset directly. |

| **Visualizations Gallery** | `EdaController`, `EdaEngine` | `VisualizationsPage` (Stage 10: Visuals) | `GET /api/datasets/{id}/eda/correlations`<br>`GET /api/datasets/{id}/eda/boxplots`<br>`GET /api/datasets/{id}/eda/grouped` | Interactive Chart.js charts for correlation, grouped bar charts, box plots | Online | Passing (`EndToEndPipelineTest`) | **PARTIALLY WORKING** | Defaulted to `customer_id` vs `gender`, rendering uninformative chart with huge axis; missing empty state fallback when no valid observations. |

| **Explainability (XAI) & Fairness** | `ExplainabilityEngine`, `FairnessEngine`, `AutoMlService` | `AutoMlPage` (Stage 11: XAI) | Embedded in `/automl/train` and `/automl/latest` | Permutation feature importance, EEOC 80% four-fifths rule, counterfactual what-if simulation | Online | Passing (`AutoMlEngineTest`) | **PARTIALLY WORKING** | Contradictory recommendation banner ("Explain Trained Model" shown when no model trained); XAI route should have explicit dedicated view and prerequisite guards. |

| **Scientific Findings Repository** | `FindingController`, `FindingService`, `Finding`, `EvidenceNode` | `FindingsPage` (Stage 12: Findings) | `GET /api/datasets/{id}/findings`<br>`POST /api/datasets/{id}/findings/synthesize` | Provenance-backed empirical findings linked to statistical evidence nodes | Online | Passing (`EndToEndPipelineTest`) | **WORKING** | Can be completed from statistics alone without requiring ML; dataset filtering needs URL-first resolution. |

| **Scientific Report & Vector PDF** | `ReportController`, `ReportService`, `ReportGeneratorService`, `PdfExportService` | `ReportsPage` (Stage 13: Report) | `POST /api/datasets/{id}/reports`<br>`GET /api/reports`<br>`GET /api/reports/{id}/pdf` | Synthesizes full markdown report and exports vector PDF | Online | Passing (`ReportAndPdfTest`) | **BROKEN** | `ReportsPage` failed on `datasetService.getDatasets()`; missing `@GetMapping("/datasets/{id}/reports")` endpoint; report generation needed conditional sections when ML/XAI not run. |

| **Workflow Navigation (Mode 1 & Mode 2)** | `WorkflowController`, `WorkflowService` | `WorkflowNavigator`, `WorkflowContext`, `PrerequisiteAlert`, `RecommendedNextStep` | `GET /api/datasets/{id}/workflow-status` | 13-stage persistent stepper + top/bottom Prev/Next buttons | Online | Passing (`WorkflowServiceTest`) | **PARTIALLY WORKING** | Stage recommendations sometimes contradicted prerequisite state (e.g. XAI recommending model explanation when no model trained). |

| **Data Drift Engine** | `DriftController`, `DataDriftService`, `DataDriftEngine` | `DriftPage` | `GET /api/drift/compare` | Population Stability Index (PSI) and Kolmogorov-Smirnov test between dataset versions | Online | Passing (`DataDriftAndAskTest`) | **WORKING** | Operational for comparing baseline and target datasets. |

| **Ask Dataset (NLP)** | `AskDatasetController`, `AskDatasetService`, `AskDatasetEngine` | `AskDatasetPage` | `POST /api/datasets/{id}/ask` | Rule-based semantic NL query engine on active dataset | Online | Passing (`DataDriftAndAskTest`) | **WORKING** | Requires active dataset context synchronization. |



---



## 2. Summary of Identified Defects to Address



1. **Defect 1 (Issue A - ML Page):**

   - **Root Cause:** `AutoMlPage.jsx` invoked undefined `datasetService.getDatasets()`, throwing a client-side JavaScript TypeError caught into `setError('Failed to load datasets.')`. Also, it did not resolve `/datasets/:id/ml` route ID directly, relying on a global dataset fetch.

   - **Fix:** Add `getDatasets()` alias to `datasetService.js`, load active dataset directly from route `id` and `WorkflowContext`, populate columns and target variable immediately, detect problem type dynamically, suggest sensitive columns, and surface actual backend error + retry button if failed.



2. **Defect 2 (Issue B - XAI Page):**

   - **Root Cause:** `WorkflowContext.jsx` unconditionally recommended `Explain Model` when on Stage 11 (XAI) or Stage 9 (ML), even if `isStageCompleted('ml')` was false. `PrerequisiteAlert` displayed "Model explanation unavailable", while `RecommendedNextStep` simultaneously showed "Explain Trained Model".

   - **Fix:** In `WorkflowContext.jsx` and `RecommendedNextStep.jsx`, dynamically check `isStageCompleted('ml')`. If no model exists: display "Train a Model First" with description and "Go to Machine Learning →" button. If model exists: display "Explain Trained Model" with "Explain Model →" button.



3. **Defect 3 (Issue C - Visualizations Page):**

   - **Root Cause:** Backend `DatasetProfiler.java` classified ID columns (like `customer_id`, `id`, `user_id`) as regular `NUMERIC`. Consequently, `VisualizationsPage.jsx` selected `numVars[0]` (`customer_id`) and `catVars[0]` (`gender`), rendering an uninformative chart with a massive axis.

   - **Fix:**

     - Update backend `DatasetProfiler.java` to detect identifier columns by pattern (`id`, `ID`, `*_id`, `*Id`, `id_*`, high cardinality sequential/unique values) and classify them as `IDENTIFIER`.

     - Exclude `IDENTIFIER` from default numeric analysis in `EdaEngine`, `AssociationEngine`, `FeatureEngineeringEngine`.

     - Update `VisualizationsPage.jsx` to filter out identifier columns from default numeric selection, prefer meaningful continuous variables (e.g. `age`, `salary`, `score`, etc.), and display "No valid observations available for this visualization." when no observations exist.



4. **Defect 4 (Issue D - Reports Page):**

   - **Root Cause:**

     - `ReportsPage.jsx` called `datasetService.getDatasets()` which failed with TypeError.

     - Backend had no `@GetMapping("/datasets/{id}/reports")` endpoint.

     - `ReportsPage.jsx` failed entirely if `listReports()` failed or was empty.

     - `ReportGeneratorService.java` omitted conditional sections when ML/XAI were not performed.

   - **Fix:**

     - Add `@GetMapping("/datasets/{id}/reports")` to `ReportController.java` and `ReportService.java`.

     - Update `ReportsPage.jsx` to load active dataset and its reports independently using `id` from route.

     - Ensure `ReportGeneratorService.java` generates conditional sections ("Machine Learning: Not performed", "Explainability: Not available — no trained model", "Fairness: Not performed", "Data Drift: Not performed") so that reports work completely from statistical tests/findings alone.
