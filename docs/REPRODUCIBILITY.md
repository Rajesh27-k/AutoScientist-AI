# AutoScientist AI — Scientific Reproducibility & Provenance Standard

## 1. Principles of Scientific Reproducibility
A scientific discovery platform must ensure that any research conclusion can be replicated byte-for-byte by external peer reviewers. AutoScientist AI implements a deterministic, multi-layered provenance architecture.

---

## 2. Deterministic Execution Architecture

### 2.1 Seeded Randomness
- Preprocessing and feature engineering use fixed random seeds (`Random(42)`) for train/val/test splits.
- Permutation feature importance algorithms utilize deterministic shuffling seeds so that feature rankings and importance percentages remain constant across runs on the same dataset.
- Random forest bootstrap sampling uses constant seed sequences.

### 2.2 Leak-Free Partitioning
- In observational data science, a frequent source of irreproducible claims is **data leakage** (e.g. standardizing or imputing the entire dataset prior to splitting).
- AutoScientist AI computes all imputation values (medians/modes) and scaling parameters ($\mu, \sigma$) strictly on the **Training Partition (70%)**.
- The exact transformation mapping is serialized as JSON in `experiment_versions.preprocessing_json`, allowing external pipelines to apply identical transforms to out-of-sample data.

---

## 3. Directed Acyclic Graph (DAG) Evidence Provenance

Every scientific finding created in AutoScientist AI is backed by an explicit Directed Acyclic Graph (DAG) stored in `evidence_nodes`:

```
               [ Dataset Entity ]
                 (SHA-256 Hash)
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
[ Statistical Test ]        [ AutoML Model Run ]
 (p-value, test stat)        (Hyperparameters, SVD)
         │                           │
         └─────────────┬─────────────┘
                       ▼
              [ Finding Entity ]
         (Confidence, Provenance Nodes)
                       │
                       ▼
            [ Scientific Report ]
          (Markdown / Vector PDF)
```

Each `evidence_nodes` entry records:
- `node_type`: `DATASET`, `STAT_TEST`, `ML_MODEL`, `FEATURE`.
- `reference_id`: Foreign key link to the exact database record.
- `description`: Plain-language empirical assertion.
- `data_payload_json`: Verifiable mathematical parameters (p-value, degrees of freedom, sample size, test statistic).

---

## 4. Audit Trail & Verification Checklist for Peer Review
When sharing a report or finding with external evaluators:
1. **Verify Raw Data**: Confirm the dataset row count, column count, and column data types match the Section 2 Profiling Table.
2. **Recompute Bivariate Tests**: Execute independent Pearson / Welch / ANOVA computations; test statistics must match within $\pm 0.001$.
3. **Verify Model Metrics**: Inspect validation $R^2$ or F1 score in `models` table against the AutoML benchmark table.
4. **Audit Causality Disclaimers**: Ensure non-experimental claims disclaim causal directionality.
