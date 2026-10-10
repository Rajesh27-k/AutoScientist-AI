import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useLocation, useNavigate, Link } from 'react-router-dom';
import { datasetService } from '../services/datasetService';
import { analysisService } from '../services/analysisService';
import WorkflowNavigator from '../components/WorkflowNavigator';
import { useWorkflow } from '../context/WorkflowContext';
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

export default function AutoMlPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { datasetId: workflowDatasetId, selectDataset, refreshWorkflowStatus } = useWorkflow();

  const currentStage = location.pathname.includes('/xai') ? 'xai' : 'ml';
  const effectiveId = id || workflowDatasetId;

  const [datasets, setDatasets] = useState([]);
  const [selectedDatasetId, setSelectedDatasetId] = useState(effectiveId || '');
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [targetColumn, setTargetColumn] = useState('');
  const [problemType, setProblemType] = useState('AUTO');
  const [sensitiveColumn, setSensitiveColumn] = useState('');
  const [initialLoading, setInitialLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);

  // What-If Simulator state
  const [whatIfInputs, setWhatIfInputs] = useState({});
  const [whatIfResult, setWhatIfResult] = useState(null);
  const [whatIfLoading, setWhatIfLoading] = useState(false);

  // 1. Load active dataset directly from route parameter or workflow context
  const loadActiveDataset = useCallback(async (dsId) => {
    if (!dsId) {
      setInitialLoading(false);
      return;
    }
    setInitialLoading(true);
    setError(null);
    try {
      setSelectedDatasetId(dsId);
      if (selectDataset) selectDataset(dsId);

      const [dsRes, mlRes] = await Promise.all([
        datasetService.getDataset(dsId),
        analysisService.getLatestAutoMl(dsId).catch(() => null),
      ]);

      if (dsRes && dsRes.data) {
        const ds = dsRes.data;
        setSelectedDataset(ds);

        const cols = ds.columns || [];
        const nonIdCols = cols.filter((c) => c.dataType !== 'IDENTIFIER');
        const defaultTarget =
          nonIdCols.length > 0 ? nonIdCols[nonIdCols.length - 1].columnName : cols[0]?.columnName || '';

        setTargetColumn(defaultTarget);

        // Auto-detect problem type from default target
        const targetMeta = cols.find((c) => c.columnName === defaultTarget);
        if (targetMeta) {
          if (targetMeta.dataType === 'NUMERIC' && targetMeta.uniqueCount > 10) {
            setProblemType('REGRESSION');
          } else {
            setProblemType('CLASSIFICATION');
          }
        }

        // Auto-detect suggested sensitive column
        const sens = cols.find((c) => {
          const n = c.columnName.toLowerCase();
          return (
            n.includes('gender') ||
            n.includes('sex') ||
            n.includes('race') ||
            n.includes('ethnicity') ||
            n.includes('tutoring') ||
            n.includes('smoker')
          );
        });
        if (sens) {
          setSensitiveColumn(sens.columnName);
        }
      }

      if (mlRes && mlRes.data) {
        setResults(mlRes.data);
        if (mlRes.data.targetColumn) setTargetColumn(mlRes.data.targetColumn);
        if (mlRes.data.problemType) setProblemType(mlRes.data.problemType);
        if (mlRes.data.featureImportances) {
          const initialInputs = {};
          mlRes.data.featureImportances.forEach((f) => {
            initialInputs[f.featureName] = 0;
          });
          setWhatIfInputs(initialInputs);
        }
      }
    } catch (err) {
      console.error('Failed to load active dataset details:', err);
      setError(
        err.response?.data?.message ||
          err.message ||
          'Unable to load the active dataset from server.'
      );
    } finally {
      setInitialLoading(false);
    }
  }, [selectDataset]);

  useEffect(() => {
    if (effectiveId) {
      loadActiveDataset(effectiveId);
    } else {
      // If no ID in route or context, try fetching available datasets
      datasetService
        .listDatasets()
        .then((res) => {
          if (res.data && res.data.length > 0) {
            setDatasets(res.data);
            loadActiveDataset(res.data[0].id);
          } else {
            setInitialLoading(false);
          }
        })
        .catch(() => {
          setInitialLoading(false);
        });
    }

    // Background fetch dataset list for switcher if multiple exist
    datasetService
      .listDatasets()
      .then((res) => {
        if (res.data) setDatasets(res.data);
      })
      .catch(() => {});
  }, [effectiveId, loadActiveDataset]);

  const handleTargetChange = (newTarget) => {
    setTargetColumn(newTarget);
    if (!selectedDataset || !selectedDataset.columns) return;
    const colMeta = selectedDataset.columns.find((c) => c.columnName === newTarget);
    if (colMeta) {
      if (colMeta.dataType === 'NUMERIC' && colMeta.uniqueCount > 10) {
        setProblemType('REGRESSION');
      } else {
        setProblemType('CLASSIFICATION');
      }
    }
  };

  const handleTrainAutoMl = async (e) => {
    e.preventDefault();
    if (!selectedDatasetId || !targetColumn) return;

    setLoading(true);
    setError(null);
    try {
      const res = await analysisService.trainAutoMl(selectedDatasetId, {
        targetColumn,
        problemType,
        sensitiveColumn: sensitiveColumn || null,
        experimentName: `AutoML Benchmark - ${targetColumn}`,
      });
      setResults(res.data);
      refreshWorkflowStatus();

      if (res.data.featureImportances) {
        const initialInputs = {};
        res.data.featureImportances.forEach((f) => {
          initialInputs[f.featureName] = 0;
        });
        setWhatIfInputs(initialInputs);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'AutoML training failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleWhatIfSubmit = async (e) => {
    e.preventDefault();
    setWhatIfLoading(true);
    try {
      const res = await analysisService.simulateWhatIf(selectedDatasetId, {
        targetColumn,
        featureValues: whatIfInputs,
      });
      setWhatIfResult(res.data);
    } catch (err) {
      setError('What-If simulation failed.');
    } finally {
      setWhatIfLoading(false);
    }
  };

  // Chart configuration for Permutation Feature Importance
  const featureChartData = results?.featureImportances
    ? {
        labels: results.featureImportances.map((f) => f.featureName),
        datasets: [
          {
            label: 'Relative Importance (%)',
            data: results.featureImportances.map((f) => f.relativePercentage),
            backgroundColor: 'rgba(59, 130, 246, 0.75)',
            borderColor: 'rgb(37, 99, 235)',
            borderWidth: 1,
            borderRadius: 4,
          },
        ],
      }
    : null;

  const candidateTargetCols =
    selectedDataset?.columns?.filter((c) => c.dataType !== 'IDENTIFIER') || [];

  if (initialLoading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary mb-3" role="status"></div>
        <p className="text-muted fw-semibold">Loading active dataset and model state...</p>
      </div>
    );
  }

  return (
    <div className="container py-4">
      <WorkflowNavigator currentStage={currentStage}>
        {/* Header Banner */}
        <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
          <div>
            <h2 className="fw-bold mb-1">
              {currentStage === 'xai' ? (
                <>
                  <i className="bi bi-eye-fill text-warning me-2"></i>
                  Explainability (XAI) & Algorithmic Fairness
                </>
              ) : (
                <>
                  <i className="bi bi-cpu-fill text-primary me-2"></i>
                  AutoML Benchmarking & Model Evaluation
                </>
              )}
            </h2>
            <p className="text-muted mb-0 small">
              {currentStage === 'xai'
                ? 'Permutation feature importance, demographic parity audits, and counterfactual scenario simulations.'
                : 'Leak-free 70/15/15 train/val/test splits, multi-algorithm benchmarking, and empirical leaderboard ranking.'}
            </p>
          </div>

          {selectedDataset && (
            <div className="d-flex align-items-center gap-2">
              <span className="badge bg-light text-dark border p-2">
                <i className="bi bi-database me-1 text-primary"></i>
                <strong>{selectedDataset.name}</strong> ({selectedDataset.rowCount} rows &bull;{' '}
                {selectedDataset.columnCount} columns)
              </span>
              {datasets.length > 1 && (
                <select
                  className="form-select form-select-sm"
                  style={{ width: '160px' }}
                  value={selectedDatasetId}
                  onChange={(e) => {
                    const newId = e.target.value;
                    navigate(`/datasets/${newId}/${currentStage}`);
                  }}
                >
                  {datasets.map((d) => (
                    <option key={d.id} value={d.id}>
                      Switch: {d.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}
        </div>

        {/* Error Alert with Retry */}
        {error && (
          <div className="alert alert-danger shadow-sm d-flex justify-content-between align-items-center mb-4" role="alert">
            <div className="d-flex align-items-center gap-2">
              <i className="bi bi-exclamation-triangle-fill fs-5"></i>
              <div>
                <strong>Error:</strong> {error}
              </div>
            </div>
            <div className="d-flex gap-2">
              <button
                type="button"
                className="btn btn-sm btn-outline-danger"
                onClick={() => loadActiveDataset(selectedDatasetId || effectiveId)}
              >
                <i className="bi bi-arrow-clockwise me-1"></i> Retry
              </button>
              <button type="button" className="btn-close" onClick={() => setError(null)}></button>
            </div>
          </div>
        )}

        {/* XAI PREREQUISITE VIEW: If on XAI stage and no model is trained */}
        {currentStage === 'xai' && !results ? (
          <div className="card border-warning border-2 bg-warning bg-opacity-10 shadow-sm p-5 mb-4 text-center">
            <div className="text-warning fs-1 mb-2">
              <i className="bi bi-lightbulb-off-fill"></i>
            </div>
            <h4 className="fw-bold text-dark mb-2">Train a Model First</h4>
            <p className="text-secondary mb-4 mx-auto" style={{ maxWidth: '620px' }}>
              XAI requires a successfully trained model before explanations, permutation feature importance,
              and demographic fairness audits can be generated.
            </p>
            <div>
              <Link
                to={`/datasets/${selectedDatasetId || effectiveId}/ml`}
                className="btn btn-primary fw-semibold px-4 py-2 shadow-sm"
              >
                Go to Machine Learning <i className="bi bi-arrow-right ms-1"></i>
              </Link>
            </div>
          </div>
        ) : null}

        {/* STAGE 9: AutoML Training Configuration Card (shown when on ML stage) */}
        {currentStage === 'ml' && (
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-header bg-white py-3 border-0">
              <h5 className="fw-bold mb-0">
                <i className="bi bi-gear-wide-connected text-primary me-2"></i>
                AutoML Training Configuration
              </h5>
            </div>
            <div className="card-body p-4">
              <form onSubmit={handleTrainAutoMl}>
                <div className="row g-3">
                  <div className="col-md-4">
                    <label className="form-label fw-semibold">Target Variable (Y)</label>
                    <select
                      className="form-select"
                      value={targetColumn}
                      onChange={(e) => handleTargetChange(e.target.value)}
                      disabled={loading || !selectedDataset}
                    >
                      {candidateTargetCols.map((c) => (
                        <option key={c.columnName} value={c.columnName}>
                          {c.columnName} ({c.dataType})
                        </option>
                      ))}
                    </select>
                    <small className="text-muted d-block mt-1">
                      Identifier variables are automatically filtered from candidate targets.
                    </small>
                  </div>

                  <div className="col-md-4">
                    <label className="form-label fw-semibold">Problem Type</label>
                    <select
                      className="form-select"
                      value={problemType}
                      onChange={(e) => setProblemType(e.target.value)}
                      disabled={loading}
                    >
                      <option value="AUTO">Auto-Detect from Target</option>
                      <option value="REGRESSION">Regression (Continuous Numerical)</option>
                      <option value="CLASSIFICATION">Classification (Categorical / Binary)</option>
                    </select>
                    <small className="text-muted d-block mt-1">
                      Determined automatically based on target variable distribution.
                    </small>
                  </div>

                  <div className="col-md-4">
                    <label className="form-label fw-semibold">Sensitive Column (Fairness Audit)</label>
                    <select
                      className="form-select"
                      value={sensitiveColumn}
                      onChange={(e) => setSensitiveColumn(e.target.value)}
                      disabled={loading || !selectedDataset}
                    >
                      <option value="">Auto-Detect / None</option>
                      {selectedDataset?.columns?.map((c) => (
                        <option key={c.columnName} value={c.columnName}>
                          {c.columnName} ({c.dataType})
                        </option>
                      ))}
                    </select>
                    <small className="text-muted d-block mt-1">
                      Audits EEOC 80% Four-Fifths rule across groups.
                    </small>
                  </div>
                </div>

                <div className="mt-4 d-flex justify-content-end">
                  <button
                    type="submit"
                    className="btn btn-primary px-4 py-2 fw-semibold"
                    disabled={loading || !selectedDatasetId || !targetColumn}
                  >
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                        Benchmarking Models (70/15/15)...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-play-circle-fill me-2"></i>
                        Run AutoML Benchmark
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* RESULTS VIEW: Rendered if models have been benchmarked */}
        {results && (
          <>
            {/* Scientific Summary Banner */}
            <div className="alert alert-primary shadow-sm border-0 mb-4 p-4">
              <h5 className="alert-heading fw-bold mb-2">
                <i className="bi bi-journal-check me-2"></i>
                Empirical AutoML Summary
              </h5>
              <p className="mb-0 fs-6">{results.scientificSummary}</p>
            </div>

            {/* Candidate Model Leaderboard (Shown on ML and XAI) */}
            <div className="row g-4 mb-4">
              <div className="col-lg-7">
                <div className="card shadow-sm border-0 h-100">
                  <div className="card-header bg-white py-3 border-0">
                    <h5 className="fw-bold mb-0">
                      <i className="bi bi-trophy-fill text-warning me-2"></i>
                      Candidate Model Benchmark Leaderboard
                    </h5>
                    <small className="text-muted">
                      Evaluated on holdout validation & test splits without target leakage.
                    </small>
                  </div>
                  <div className="card-body p-0">
                    <div className="table-responsive">
                      <table className="table table-hover align-middle mb-0">
                        <thead className="table-light">
                          <tr>
                            <th>Rank</th>
                            <th>Model</th>
                            <th>Algorithm</th>
                            <th>Validation Metric</th>
                            <th>Test Metric</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {results.models?.map((m, idx) => (
                            <tr key={m.id || idx} className={m.isBestModel ? 'table-primary bg-opacity-25' : ''}>
                              <td className="fw-bold text-center">{idx + 1}</td>
                              <td>
                                <span className="fw-semibold">{m.modelName}</span>
                              </td>
                              <td>
                                <span className="badge bg-light text-dark border">{m.algorithm}</span>
                              </td>
                              <td>
                                {results.problemType === 'REGRESSION' ? (
                                  <span>
                                    R² = <strong>{m.valMetrics?.r2?.toFixed(3) ?? 'N/A'}</strong> (RMSE ={' '}
                                    {m.valMetrics?.rmse?.toFixed(2) ?? 'N/A'})
                                  </span>
                                ) : (
                                  <span>
                                    F1 = <strong>{m.valMetrics?.f1?.toFixed(3) ?? 'N/A'}</strong> (Acc ={' '}
                                    {((m.valMetrics?.accuracy ?? 0) * 100).toFixed(1)}%)
                                  </span>
                                )}
                              </td>
                              <td>
                                {results.problemType === 'REGRESSION' ? (
                                  <span>R² = {m.testMetrics?.r2?.toFixed(3) ?? 'N/A'}</span>
                                ) : (
                                  <span>F1 = {m.testMetrics?.f1?.toFixed(3) ?? 'N/A'}</span>
                                )}
                              </td>
                              <td>
                                {m.isBestModel ? (
                                  <span className="badge bg-success">
                                    <i className="bi bi-star-fill me-1"></i> Best Model
                                  </span>
                                ) : (
                                  <span className="badge bg-secondary">Candidate</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>

              {/* Fairness Audit Card */}
              <div className="col-lg-5">
                <div className="card shadow-sm border-0 h-100">
                  <div className="card-header bg-white py-3 border-0">
                    <h5 className="fw-bold mb-0">
                      <i className="bi bi-shield-check text-success me-2"></i>
                      Algorithmic Fairness & Bias Audit
                    </h5>
                    <small className="text-muted">
                      EEOC 80% Four-Fifths disparate impact verification
                    </small>
                  </div>
                  <div className="card-body p-4">
                    {results.fairness ? (
                      <div>
                        <div className="d-flex justify-content-between align-items-center mb-3">
                          <span className="text-muted">Sensitive Attribute:</span>
                          <span className="fw-bold text-dark">{results.fairness.sensitiveColumn}</span>
                        </div>

                        <div className="d-flex justify-content-between align-items-center mb-3">
                          <span className="text-muted">Demographic Parity Ratio:</span>
                          <span className="fw-bold fs-5 text-primary">
                            {results.fairness.demographicParityRatio?.toFixed(3)}
                          </span>
                        </div>

                        <div className="d-flex justify-content-between align-items-center mb-3">
                          <span className="text-muted">Four-Fifths (80%) Rule:</span>
                          <span
                            className={`badge ${
                              results.fairness.disparateImpactStatus === 'PASS'
                                ? 'bg-success'
                                : results.fairness.disparateImpactStatus === 'FLAGGED'
                                ? 'bg-danger'
                                : 'bg-secondary'
                            } px-3 py-2`}
                          >
                            {results.fairness.disparateImpactStatus}
                          </span>
                        </div>

                        <div className="alert alert-light border p-3 mt-3">
                          <small className="text-muted d-block fw-semibold mb-1">Audit Finding & Guidance:</small>
                          <small>{results.fairness.recommendation}</small>
                        </div>
                      </div>
                    ) : (
                      <div className="text-center py-4 text-muted">
                        <i className="bi bi-shield-slash fs-2 d-block mb-2"></i>
                        <span>No sensitive demographic attribute specified for fairness audit.</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Explainability (XAI): Permutation Feature Importance */}
            <div className="card shadow-sm border-0 mb-4">
              <div className="card-header bg-white py-3 border-0 d-flex justify-content-between align-items-center">
                <div>
                  <h5 className="fw-bold mb-0">
                    <i className="bi bi-lightbulb-fill text-warning me-2"></i>
                    Explainability (XAI): Permutation Feature Importance
                  </h5>
                  <small className="text-muted">
                    Methodology: Permutation feature importance on test partition (Leak-free, not SHAP)
                  </small>
                </div>
                <span className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 px-2 py-1">
                  Empirical XAI
                </span>
              </div>
              <div className="card-body p-4">
                <div className="row g-4 align-items-center">
                  <div className="col-lg-6">
                    {featureChartData && (
                      <div style={{ height: '320px' }}>
                        <Bar
                          data={featureChartData}
                          options={{
                            responsive: true,
                            maintainAspectRatio: false,
                            indexAxis: 'y',
                            plugins: {
                              legend: { display: false },
                              tooltip: {
                                callbacks: {
                                  label: (ctx) => `Importance: ${ctx.parsed.x}%`,
                                },
                              },
                            },
                            scales: {
                              x: {
                                title: { display: true, text: 'Relative Contribution (%)' },
                                max: 100,
                              },
                            },
                          }}
                        />
                      </div>
                    )}
                  </div>

                  <div className="col-lg-6">
                    <div className="table-responsive">
                      <table className="table table-sm align-middle">
                        <thead className="table-light">
                          <tr>
                            <th>Rank</th>
                            <th>Feature</th>
                            <th>Importance</th>
                            <th>Direction</th>
                            <th>Scientific Interpretation</th>
                          </tr>
                        </thead>
                        <tbody>
                          {results.featureImportances?.map((f) => (
                            <tr key={f.featureName}>
                              <td className="fw-bold text-center">#{f.rank}</td>
                              <td className="fw-semibold text-primary">{f.featureName}</td>
                              <td>{f.relativePercentage?.toFixed(1)}%</td>
                              <td>
                                <span
                                  className={`badge ${
                                    f.direction === 'POSITIVE'
                                      ? 'bg-success'
                                      : f.direction === 'NEGATIVE'
                                      ? 'bg-danger'
                                      : 'bg-secondary'
                                  }`}
                                >
                                  {f.direction}
                                </span>
                              </td>
                              <td>
                                <small className="text-muted">{f.interpretation}</small>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Interactive Counterfactual What-If Simulator */}
            <div className="card shadow-sm border-0 mb-4">
              <div className="card-header bg-white py-3 border-0">
                <h5 className="fw-bold mb-0">
                  <i className="bi bi-sliders text-primary me-2"></i>
                  Counterfactual "What-If" Scenario Simulator
                </h5>
                <small className="text-muted">
                  Simulate outcome predictions dynamically using the trained best model.
                </small>
              </div>
              <div className="card-body p-4">
                <form onSubmit={handleWhatIfSubmit}>
                  <div className="row g-3">
                    {results.featureImportances?.slice(0, 6).map((f) => (
                      <div key={f.featureName} className="col-md-4">
                        <label className="form-label small fw-semibold text-truncate d-block">
                          {f.featureName}
                        </label>
                        <input
                          type="number"
                          step="any"
                          className="form-control"
                          value={whatIfInputs[f.featureName] ?? 0}
                          onChange={(e) =>
                            setWhatIfInputs({
                              ...whatIfInputs,
                              [f.featureName]: parseFloat(e.target.value) || 0,
                            })
                          }
                        />
                      </div>
                    ))}
                  </div>

                  <div className="mt-4 d-flex justify-content-end">
                    <button type="submit" className="btn btn-outline-primary fw-semibold" disabled={whatIfLoading}>
                      {whatIfLoading ? 'Computing Simulation...' : 'Simulate Counterfactual Prediction'}
                    </button>
                  </div>
                </form>

                {whatIfResult && (
                  <div className="mt-4 p-4 bg-light rounded border">
                    <div className="row align-items-center">
                      <div className="col-md-4 text-center border-end">
                        <span className="text-muted d-block small mb-1">Simulated Prediction</span>
                        <h3 className="fw-bold text-primary mb-0">
                          {whatIfResult.problemType === 'REGRESSION'
                            ? whatIfResult.predictedValue?.toFixed(3)
                            : whatIfResult.predictedClass}
                        </h3>
                      </div>
                      <div className="col-md-4 text-center border-end">
                        <span className="text-muted d-block small mb-1">Baseline Dataset Mean</span>
                        <h4 className="fw-semibold text-secondary mb-0">
                          {whatIfResult.baselineMean?.toFixed(3)}
                        </h4>
                      </div>
                      <div className="col-md-4 text-center">
                        <span className="text-muted d-block small mb-1">Marginal Impact (Delta)</span>
                        <h4
                          className={`fw-bold mb-0 ${
                            whatIfResult.deltaFromBaseline >= 0 ? 'text-success' : 'text-danger'
                          }`}
                        >
                          {whatIfResult.deltaFromBaseline >= 0 ? '+' : ''}
                          {whatIfResult.deltaFromBaseline?.toFixed(3)}
                        </h4>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </WorkflowNavigator>
    </div>
  );
}
