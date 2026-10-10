import React, { useState, useEffect } from 'react';
import { datasetService } from '../services/datasetService';
import { analysisService } from '../services/analysisService';

export default function DriftPage() {
  const [datasets, setDatasets] = useState([]);
  const [referenceId, setReferenceId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [loading, setLoading] = useState(false);
  const [driftReport, setDriftReport] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDatasets();
  }, []);

  const loadDatasets = async () => {
    try {
      const res = await datasetService.getDatasets();
      if (res.data && res.data.length >= 1) {
        setDatasets(res.data);
        setReferenceId(res.data[0].id);
        if (res.data.length > 1) {
          setTargetId(res.data[1].id);
        } else {
          setTargetId(res.data[0].id);
        }
      }
    } catch (err) {
      setError('Failed to load datasets.');
    }
  };

  const handleRunDrift = async (e) => {
    e.preventDefault();
    if (!referenceId || !targetId) return;

    setLoading(true);
    setError(null);
    try {
      const res = await analysisService.compareDrift(referenceId, targetId);
      setDriftReport(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Drift analysis failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container-fluid py-4 px-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-water text-primary me-2"></i>
            Data Drift & Cohort Stability Engine
          </h2>
          <p className="text-muted mb-0">
            Calculate Population Stability Index (PSI), distribution shift, and Welch-t tests across datasets.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
          <button type="button" className="btn-close" onClick={() => setError(null)}></button>
        </div>
      )}

      {/* Dataset Selection */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-body p-4">
          <form onSubmit={handleRunDrift}>
            <div className="row g-3 align-items-end">
              <div className="col-md-5">
                <label className="form-label fw-semibold">Baseline / Reference Dataset</label>
                <select
                  className="form-select"
                  value={referenceId}
                  onChange={(e) => setReferenceId(e.target.value)}
                  disabled={loading}
                >
                  {datasets.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name} ({d.rowCount} rows)
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-md-5">
                <label className="form-label fw-semibold">Comparison / Target Dataset</label>
                <select
                  className="form-select"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  disabled={loading}
                >
                  {datasets.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name} ({d.rowCount} rows)
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-md-2">
                <button
                  type="submit"
                  className="btn btn-primary w-100 py-2 fw-semibold"
                  disabled={loading || !referenceId || !targetId}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                      Evaluating...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-speedometer2 me-2"></i>
                      Analyze Drift
                    </>
                  )}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      {/* Drift Report */}
      {driftReport && (
        <>
          {/* Summary Overview Cards */}
          <div className="row g-4 mb-4">
            <div className="col-md-4">
              <div className="card shadow-sm border-0 p-3 text-center h-100">
                <span className="text-muted small fw-semibold">Overall Population Stability Index (PSI)</span>
                <h2 className="fw-bold my-2 text-primary">
                  {driftReport.overallDriftIndex?.toFixed(4)}
                </h2>
                <div>
                  <span
                    className={`badge ${
                      driftReport.overallStatus === 'STABLE'
                        ? 'bg-success'
                        : driftReport.overallStatus === 'LOW_RISK'
                        ? 'bg-warning text-dark'
                        : 'bg-danger'
                    } px-3 py-2`}
                  >
                    {driftReport.overallStatus}
                  </span>
                </div>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card shadow-sm border-0 p-3 text-center h-100">
                <span className="text-muted small fw-semibold">Features Evaluated</span>
                <h2 className="fw-bold my-2 text-dark">
                  {driftReport.totalFeaturesAnalyzed}
                </h2>
                <small className="text-muted">
                  Common aligned columns across both cohorts
                </small>
              </div>
            </div>

            <div className="col-md-4">
              <div className="card shadow-sm border-0 p-3 text-center h-100">
                <span className="text-muted small fw-semibold">Drifted Feature Count</span>
                <h2
                  className={`fw-bold my-2 ${
                    driftReport.driftedFeaturesCount > 0 ? 'text-danger' : 'text-success'
                  }`}
                >
                  {driftReport.driftedFeaturesCount}
                </h2>
                <small className="text-muted">Features with PSI &ge; 0.10</small>
              </div>
            </div>
          </div>

          {/* Recommendation Banner */}
          <div className="alert alert-light border shadow-sm p-4 mb-4">
            <h5 className="alert-heading fw-bold mb-2">
              <i className="bi bi-info-circle-fill text-primary me-2"></i>
              System Recommendation & Stability Diagnosis
            </h5>
            <p className="mb-0 fs-6">{driftReport.summaryRecommendation}</p>
          </div>

          {/* Per-Column Drift Breakdown Table */}
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-header bg-white py-3 border-0">
              <h5 className="fw-bold mb-0">Feature-Level Distribution Shift Breakdown</h5>
            </div>
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Feature Name</th>
                      <th>Type</th>
                      <th>PSI Score</th>
                      <th>Drift Status</th>
                      <th>Ref Mean</th>
                      <th>Target Mean</th>
                      <th>Mean Shift</th>
                      <th>Empirical Interpretation</th>
                    </tr>
                  </thead>
                  <tbody>
                    {driftReport.columnDrifts?.map((c) => (
                      <tr key={c.columnName}>
                        <td className="fw-semibold text-primary">{c.columnName}</td>
                        <td>
                          <span className="badge bg-light text-dark border">{c.dataType}</span>
                        </td>
                        <td className="fw-bold">{c.psiScore?.toFixed(4)}</td>
                        <td>
                          <span
                            className={`badge ${
                              c.driftStatus === 'NO_DRIFT'
                                ? 'bg-success'
                                : c.driftStatus === 'MODERATE_DRIFT'
                                ? 'bg-warning text-dark'
                                : 'bg-danger'
                            }`}
                          >
                            {c.driftStatus}
                          </span>
                        </td>
                        <td>{c.referenceMean != null ? c.referenceMean.toFixed(2) : '-'}</td>
                        <td>{c.targetMean != null ? c.targetMean.toFixed(2) : '-'}</td>
                        <td>
                          {c.meanShiftPct != null ? (
                            <span
                              className={
                                Math.abs(c.meanShiftPct) > 10 ? 'text-danger fw-semibold' : 'text-muted'
                              }
                            >
                              {c.meanShiftPct > 0 ? '+' : ''}
                              {c.meanShiftPct.toFixed(1)}%
                            </span>
                          ) : (
                            '-'
                          )}
                        </td>
                        <td>
                          <small className="text-muted">{c.interpretation}</small>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
