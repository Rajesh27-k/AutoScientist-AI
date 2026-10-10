import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { datasetService } from '../services/datasetService';
import { researchService } from '../services/researchService';
import WorkflowNavigator from '../components/WorkflowNavigator';
import { useWorkflow } from '../context/WorkflowContext';

const FindingsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { datasetId: workflowDatasetId, selectDataset } = useWorkflow();
  const activeId = id || workflowDatasetId;

  const [datasets, setDatasets] = useState([]);
  const [selectedDatasetId, setSelectedDatasetId] = useState(activeId || '');
  const [dataset, setDataset] = useState(null);
  const [findings, setFindings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = useCallback(async (targetId) => {
    if (!targetId) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError('');
      setSelectedDatasetId(targetId);
      if (selectDataset) selectDataset(targetId);

      const [dsRes, fRes] = await Promise.all([
        datasetService.getDataset(targetId).catch(() => null),
        researchService.getDatasetFindings(targetId).catch(() => ({ data: [] })),
      ]);

      if (dsRes && dsRes.data) {
        setDataset(dsRes.data);
      }

      if (fRes && fRes.data) {
        setFindings(fRes.data);
      }
    } catch (err) {
      console.error('Failed to load findings:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load scientific findings.');
    } finally {
      setLoading(false);
    }
  }, [selectDataset]);

  useEffect(() => {
    if (activeId) {
      loadData(activeId);
    } else {
      datasetService
        .listDatasets()
        .then((res) => {
          if (res.data && res.data.length > 0) {
            loadData(res.data[0].id);
          } else {
            setLoading(false);
          }
        })
        .catch(() => setLoading(false));
    }

    datasetService
      .listDatasets()
      .then((res) => {
        if (res.data) setDatasets(res.data);
      })
      .catch(() => {});
  }, [activeId, loadData]);

  const handleDatasetChange = (dsId) => {
    navigate(`/datasets/${dsId}/findings`);
  };

  const getConfidenceBadge = (level) => {
    switch (level) {
      case 'HIGH':
        return <span className="badge bg-success">High Confidence</span>;
      case 'MODERATE':
        return <span className="badge bg-primary">Moderate Confidence</span>;
      case 'WEAK':
        return <span className="badge bg-warning text-dark">Weak Confidence</span>;
      default:
        return <span className="badge bg-secondary">Insufficient Evidence</span>;
    }
  };

  return (
    <div className="container py-4">
      <WorkflowNavigator currentStage="findings">
        <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
          <div>
            <h2 className="fw-bold mb-1">
              <i className="bi bi-award-fill text-warning me-2"></i>Scientific Findings & Evidence Repository
            </h2>
            <p className="text-muted small mb-0">
              Validated empirical discoveries linked to dataset versions and statistical provenance nodes.
            </p>
          </div>

          <div className="d-flex align-items-center gap-2">
            {dataset && (
              <span className="badge bg-light text-dark border p-2">
                <i className="bi bi-database me-1 text-primary"></i>
                <strong>{dataset.name}</strong> ({dataset.rowCount} rows)
              </span>
            )}

            {datasets.length > 1 && (
              <select
                className="form-select form-select-sm"
                value={selectedDatasetId}
                onChange={(e) => handleDatasetChange(e.target.value)}
                style={{ width: '180px' }}
              >
                {datasets.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>

        {error && (
          <div className="alert alert-danger shadow-sm d-flex justify-content-between align-items-center mb-4" role="alert">
            <div>
              <i className="bi bi-exclamation-triangle-fill me-2"></i>
              {error}
            </div>
            <button
              className="btn btn-sm btn-outline-danger"
              onClick={() => loadData(selectedDatasetId || activeId)}
            >
              <i className="bi bi-arrow-clockwise me-1"></i> Retry
            </button>
          </div>
        )}

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status"></div>
            <p className="text-muted small mt-2">Loading evidence trail...</p>
          </div>
        ) : findings.length === 0 ? (
          <div className="card text-center py-5 border-dashed bg-white shadow-sm">
            <div className="card-body">
              <i className="bi bi-journal-x display-4 text-muted mb-2 d-block"></i>
              <h5 className="fw-bold">No Evidence-Backed Findings Generated Yet</h5>
              <p className="text-muted small max-w-md mx-auto mb-3">
                Findings are synthesized from statistical hypothesis testing decisions or predictive models.
              </p>
              {selectedDatasetId && (
                <Link to={`/datasets/${selectedDatasetId}/research`} className="btn btn-primary fw-semibold px-4 py-2">
                  <i className="bi bi-question-circle me-1"></i> Formulate Research Question
                </Link>
              )}
            </div>
          </div>
        ) : (
          <div className="row g-4">
            {findings.map((f) => (
              <div key={f.id} className="col-12">
                <div className="card shadow-sm border-0 p-4 bg-white">
                  <div className="d-flex justify-content-between align-items-start mb-2 flex-wrap gap-2">
                    <div>
                      <div className="d-flex align-items-center gap-2 mb-2">
                        {getConfidenceBadge(f.confidenceLevel)}
                        <span className="text-muted small">
                          Recorded on {new Date(f.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      <h4 className="fw-bold text-dark mb-1">{f.title}</h4>
                    </div>
                  </div>

                  <p className="lead text-secondary fs-6 mb-3">{f.summary}</p>

                  <div className="bg-light p-3 rounded border mb-3">
                    <h6 className="fw-bold mb-2 small text-muted">EMPIRICAL STATISTICAL METRICS</h6>
                    <pre className="mb-0 small text-dark font-monospace" style={{ whiteSpace: 'pre-wrap' }}>
                      {f.evidenceText}
                    </pre>
                  </div>

                  {/* Evidence Nodes Provenance Trail */}
                  {f.evidenceNodes && f.evidenceNodes.length > 0 && (
                    <>
                      <h6 className="fw-bold mb-2 small text-muted">PROVENANCE GRAPH VERTICES</h6>
                      <div className="row g-2 mb-3">
                        {f.evidenceNodes.map((node) => (
                          <div key={node.id} className="col-md-4">
                            <div className="p-2 border rounded bg-white small h-100 shadow-sm">
                              <span className="badge bg-dark mb-1">{node.nodeType}</span>
                              <div className="text-secondary small">{node.description}</div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </>
                  )}

                  {/* Causality disclaimer */}
                  <div className="alert alert-light border small text-muted mb-0 py-2 px-3">
                    <i className="bi bi-info-circle me-1"></i>
                    <strong>Disclaimer: </strong>{f.causalityDisclaimer || 'Observational finding only; confounding unaddressed.'}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </WorkflowNavigator>
    </div>
  );
};

export default FindingsPage;
