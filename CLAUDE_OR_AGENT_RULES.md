# AutoScientist AI - Agent & Engineering Guidelines

## Core Principles
1. **Real Data & Real Computations Only**: Never fabricate data, statistical results, or machine learning evaluations. Every finding must stem from reproducible computation.
2. **Never Claim Causality from Correlation**: Rigorously distinguish between correlation, association, predictive importance, and causation. Use evidence-based phrasing such as "evidence of an association" or "statistically significant linear correlation".
3. **Handle Edge Cases Gracefully**: Handle zero variance, single-class variables, missing values (maintaining paired alignment), empty groups, and insufficient sample sizes without crashing.
4. **Security & Ownership**: Every dataset, hypothesis, test, experiment, and finding must belong to a verified user. Enforce IDOR protection on every API endpoint.
5. **No Python Rule**: The backend is strictly Java 17+ (Spring Boot 3, Apache Commons Math, OpenPDF, Java ML). Frontend is strictly React + Vite + Bootstrap + Chart.js.
6. **Milestone Verification**: Never declare a milestone complete without compiling, testing, and verifying the functionality.
