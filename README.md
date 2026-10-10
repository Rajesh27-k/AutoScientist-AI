# AutoScientist AI

> *"From Raw Data to Evidence-Based Scientific Discovery."*

AutoScientist AI is an autonomous, full-stack scientific discovery and empirical data science platform. It ingests tabular datasets (CSV), performs automated profiling and data quality audits, runs comprehensive Exploratory Data Analysis (EDA), identifies statistically sound associations, guides hypothesis formulation, executes hypothesis tests, benchmarks machine learning algorithms with automated hyperparameter selection, extracts explainability and fairness metrics, detects data drift, constructs an audit-ready evidence graph, and generates full scientific reports and downloadable vector PDFs.

---

## Key Highlights

- **Pure Java + React Stack**: Built with Spring Boot 3, Apache Commons Math, OpenPDF, and React 18 (Vite, Bootstrap, Chart.js).
- **Strictly No Python**: Complies with the pure Java/MySQL constraint—no Flask, FastAPI, pandas, numpy, scipy, or scikit-learn.
- **Zero Hallucination / Pure Empirical Computing**: All statistical tests (Pearson, Welch's t-test, ANOVA, Chi-Square), metrics, and models are calculated directly on uploaded data.
- **Scientific Rigor**: Strict separation between correlation and causation; robust edge-case handling (zero variance, missing value paired alignment, empty classes, low sample size warnings).
- **AutoML & Explainability (XAI)**: End-to-end regression and classification pipelines with automated preprocessing, validation scoring, permutation importance, and what-if counterfactual scenario simulations.
- **Algorithmic Fairness Audit**: Evaluates demographic parity ratios and EEOC Four-Fifths disparate impact rules.
- **Data Drift & Cohort Stability**: Quantile-based Population Stability Index (PSI) and Welch-t distribution shift testing.
- **Ask Dataset NLP Engine**: Query dataset in natural language with mathematically verified answers.
- **Scientific Report & Vector PDF Export**: Automated narrative Markdown synthesis and publication-quality PDF generation via OpenPDF.
- **Enterprise Security**: JWT-based authentication, BCrypt password hashing, ownership checks on every endpoint, strict path traversal defense, and centralized exception handling.

---

## Monorepo Layout

```
AutoScientist AI/
├── backend/                  # Java 17+ Spring Boot 3 REST API
│   ├── src/main/java/com/autoscientist/
│   │   ├── config/           # Security and Web configurations
│   │   ├── controller/       # REST API endpoints (Auth, Datasets, AutoML, Drift, Ask, Reports)
│   │   ├── dto/              # Request / Response DTO models
│   │   ├── entity/           # JPA Entities (User, Dataset, Experiment, Report, Finding)
│   │   ├── repository/       # Spring Data JPA Repositories
│   │   ├── service/          # Business logic & ownership enforcement
│   │   ├── security/         # JWT filter, user details, token provider
│   │   ├── analysis/         # Statistical, ML, EDA, profiling, drift, XAI engines
│   │   ├── exception/        # Global exception handler & custom exceptions
│   │   └── util/             # CSV parser, math helpers, file storage
│   ├── pom.xml
│   └── .env.example
├── frontend/                 # React 18 + Vite SPA
│   ├── src/
│   │   ├── components/       # Navbar, ProtectedRoute, UI widgets
│   │   ├── pages/            # Dashboard, Datasets, Research, AutoML, Drift, Ask, Reports
│   │   ├── services/         # Axios API clients
│   │   ├── context/          # Auth context
│   │   └── index.css
│   ├── package.json
│   └── vite.config.js
├── database/                 # MySQL schemas and migration scripts
│   └── schema.sql
├── docs/                     # Comprehensive documentation suite
│   ├── DATABASE_SCHEMA.md
│   ├── API_DOCUMENTATION.md
│   ├── SCIENTIFIC_METHODS.md
│   ├── TESTING.md
│   ├── DEPLOYMENT.md
│   ├── REPRODUCIBILITY.md
│   ├── TROUBLESHOOTING.md
│   └── VIVA_GUIDE.md
├── test-data/                # Benchmark real-world CSV datasets
│   ├── student_performance.csv
│   └── biomedical_patient_data.csv
├── scripts/                  # Setup, build, and run automation scripts
│   ├── build_all.sh
│   ├── run_backend.sh
│   └── run_frontend.sh
├── ARCHITECTURE.md
├── CLAUDE_OR_AGENT_RULES.md
└── README.md
```

---

## Quick Start

### Prerequisites
- **Java 17 or higher** (`java -version`)
- **Maven 3.8+**
- **Node.js 18+ and npm**
- **MySQL 8.0+** (or use in-memory H2 profile)

### 1. Database Setup
```bash
mysql -u root -p < database/schema.sql
```

### 2. Backend Setup
```bash
cd backend
cp .env.example .env
# Adjust SPRING_DATASOURCE_PASSWORD or JWT_SECRET if needed in .env
./mvnw spring-boot:run
```
The backend API will start on `http://localhost:8080`.

### 3. Frontend Setup
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```
The frontend UI will be accessible at `http://localhost:5173`.

### 4. One-Click Build & Verification
```bash
./scripts/build_all.sh
```

---

## Running the Automated Test Suite

From the `backend` directory:
```bash
cd backend
./mvnw clean test
```
All **24 unit and integration tests** verify:
1. `DataQualityEngineTest`: Missing values, duplicates, outliers.
2. `QuestionAnalysisEngineTest`: NLP variable extraction and $H_0/H_1$ formulation.
3. `AutoMlEngineTest`: Pure Java Ridge regression, Decision Trees, Logistic regression, Random Forest.
4. `StatisticalTestEngineTest`: Pearson, Welch's t-test, ANOVA, Chi-Square, aligned paired deletion.
5. `EndToEndPipelineTest`: Full regression workflow on `student_performance.csv`.
6. `BiomedicalClassificationPipelineTest`: Full clinical classification workflow on `biomedical_patient_data.csv`.
7. `DataDriftAndAskTest`: Population Stability Index (PSI) & Ask Dataset NLP engine.
8. `ReportAndPdfTest`: Scientific report Markdown synthesis & OpenPDF vector export.
9. `AuthControllerTest` & `AuthServiceTest`: User registration, authentication, JWT tokens.
10. `DatasetProfilerTest`: Automated type inference and statistical distributions.

---

## Complete Documentation Index

For in-depth architectural and scientific documentation, refer to the `docs/` directory:
- [Database Schema Documentation](docs/DATABASE_SCHEMA.md)
- [REST API Reference](docs/API_DOCUMENTATION.md)
- [Scientific & Statistical Methods](docs/SCIENTIFIC_METHODS.md)
- [Comprehensive Testing Guide](docs/TESTING.md)
- [Production Deployment Guide](docs/DEPLOYMENT.md)
- [Scientific Reproducibility & Provenance Standard](docs/REPRODUCIBILITY.md)
- [Operational & Statistical Troubleshooting Guide](docs/TROUBLESHOOTING.md)
- [Technical Defense & Viva Examination Guide](docs/VIVA_GUIDE.md)

---

## Scientific Integrity & Causality Statement

> **CAUTION**: Statistical significance ($p < 0.05$) and empirical mathematical associations ($r \ne 0$) establish observational correlation, **NOT** causality. All findings generated by AutoScientist AI include strict provenance and causality disclaimers advising researchers that causal inferences require randomized controlled trials (RCTs) or formal causal graphical identification.
