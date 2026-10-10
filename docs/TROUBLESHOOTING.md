# AutoScientist AI — Operational & Statistical Troubleshooting Guide

## 1. Operational & Infrastructure Issues

### Issue 1.1: Database Connection Refused
**Symptom**: `Communications link failure. The last packet sent successfully to the server was 0 milliseconds ago.`
**Resolution**:
1. Check that MySQL service is running:
   ```bash
   sudo systemctl status mysql # On Linux
   brew services list          # On macOS
   ```
2. Verify credentials match `backend/.env` or `application.yml`.
3. If running tests or development without a local MySQL instance, activate the in-memory H2 profile:
   ```bash
   SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   ```

### Issue 1.2: Path Traversal or File Upload Failure
**Symptom**: `Invalid path traversal attempt` or `File does not exist or access is restricted.`
**Resolution**:
- AutoScientist AI validates that stored files reside strictly within `app.upload.dir` (`./uploads/datasets`) using normalized paths (`.normalize()`).
- Ensure the upload directory has write permissions:
  ```bash
  mkdir -p uploads/datasets
  chmod 755 uploads/datasets
  ```

### Issue 1.3: JWT Token Expired / Unauthorized 401
**Symptom**: `UnauthorizedException: Full authentication is required to access this resource.`
**Resolution**:
- Tokens expire by default after 24 hours (`86400000 ms`).
- Log out and log back in, or adjust `app.jwt.expiration-ms` in `backend/src/main/resources/application.yml`.

---

## 2. Statistical & Machine Learning Edge Cases

### Issue 2.1: Zero Variance Detected in Feature Column
**Symptom**: Correlation returns `r = 0.0` or ANOVA returns non-significant status.
**Reason**:
- If all values in a column are identical (e.g. constant value `10.0`), the sample standard deviation $\sigma = 0$. Division by zero in Pearson $r$ or Welch's $t$ is mathematically undefined.
**Engine Handling**:
- The statistical engine detects $\sigma_x = 0$ or $\sigma_y = 0$, halts gracefully without crashing, and sets:
  ```json
  {
    "statistic": 0.0,
    "pValue": 1.0,
    "isSignificant": false,
    "interpretation": "Zero variance detected in variable; correlation is undefined."
  }
  ```

### Issue 2.2: Insufficient Paired Observations ($N < 3$)
**Symptom**: Warning issued: `Insufficient paired observations for correlation test.`
**Reason**:
- Degrees of freedom for Pearson correlation is $df = N - 2$. Calculating a valid Student's t-test p-value requires $N \ge 3$.
**Engine Handling**:
- The engine enforces aligned pair validation and requires $N \ge 3$ before calculating test statistics.

### Issue 2.3: Disparate Impact Ratio < 0.80 (Four-Fifths Rule Flagged)
**Symptom**: Fairness card displays status `FLAGGED` with disparate impact warning.
**Reason**:
- The ratio of selection rates between demographic subgroups $\frac{\min(\text{rate}_g)}{\max(\text{rate}_g)} < 0.80$.
**Resolution**:
- Inspect whether non-sensitive features act as proxies for the sensitive column.
- Consider balanced re-sampling or debiased thresholding.

### Issue 2.4: Significant Population Stability Index ($\text{PSI} \ge 0.25$)
**Symptom**: Drift report marks feature as `SIGNIFICANT_DRIFT`.
**Reason**:
- The quantile distribution of the target cohort has substantially shifted from the reference baseline cohort.
**Resolution**:
- Investigate feature schema shifts, seasonal trends, or sensor recalibrations.
- Retrain downstream AutoML models incorporating the new distribution.
