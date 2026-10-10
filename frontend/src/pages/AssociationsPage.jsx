import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { analysisService } from '../services/analysisService';
import { datasetService } from '../services/datasetService';
import WorkflowNavigator from '../components/WorkflowNavigator';
import { useWorkflow } from '../context/WorkflowContext';

const AssociationsPage = () => {
  const { id } = useParams();
  const { datasetId: workflowDatasetId, selectDataset, refreshWorkflowStatus } = useWorkflow();
  const currentId = id || workflowDatasetId;

  const [dataset, setDataset] = useState(null);
  const [associations, setAssociations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [filterSigOnly, setFilterSigOnly] = useState(false);
  const [filterTestType, setFilterTestType] = useState('ALL');

  const fetchData = async () => {
    try {
      setLoading(true);
      setError('');
      let targetId = currentId;
      if (!targetId) {
        const allDs = await datasetService.listDatasets();
        if (allDs && allDs.data && allDs.data.length > 0) {
          targetId = allDs.data[0].id;
        } else {
          setLoading(false);
          return;
        }
      }
      if (selectDataset) selectDataset(targetId);

      const [dsRes, assocRes] = await Promise.all([
        datasetService.getDataset(targetId).catch(() => null),
        analysisService.getAssociations(targetId).catch(() => null),
      ]);

      if (dsRes && dsRes.data) setDataset(dsRes.data);
      if (assocRes && assocRes.data) setAssociations(assocRes.data);
    } catch (err) {
      console.error('Failed to load associations:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load statistical associations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [currentId]);

  const handleRefresh = async () => {
    if (!currentId) return;
    try {
      setRefreshing(true);
      setError('');
      const res = await analysisService.refreshAssociations(currentId);
      if (res && res.data) {
        setAssociations(res.data);
        if (refreshWorkflowStatus) refreshWorkflowStatus();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to refresh associations.');
    } finally {
      setRefreshing(false);
    }
  };

  const handleInvestigate = (assoc) => {
    navigate(`/datasets/${currentId}/research?var1=${encodeURIComponent(assoc.var1)}&var2=${encodeURIComponent(assoc.var2)}&test=${encodeURIComponent(assoc.testType)}`);
  };

  const filteredAssociations = associations.filter((a) => {
    if (filterSigOnly && !a.isSignificant) return false;
    if (filterTestType !== 'ALL' && a.testType !== filterTestType) return false;
    return true;
  });

  const getTestBadge = (type) => {
    switch (type) {
      case 'PEARSON':
        return <span className="badge bg-primary">Pearson Correlation</span>;
      case 'WELCH_T_TEST':
        return <span className="badge bg-success">Welch's t-test</span>;
      case 'ONE_WAY_ANOVA':
        return <span className="badge bg-info text-dark">One-Way ANOVA</span>;
      case 'CHI_SQUARE':
        return <span className="badge bg-warning text-dark">Chi-Square Test</span>;
      default:
        return <span className="badge bg-secondary">{type}</span>;
    }
  };

  return (
    <div className="container py-4">
      <WorkflowNavigator currentStage="associations">
        {/* Breadcrumb */}
        <nav aria-label="breadcrumb" className="mb-3">
          <ol className="breadcrumb small">
            <li className="breadcrumb-item"><Link to="/datasets">Datasets</Link></li>
            <li className="breadcrumb-item"><Link to={`/datasets/${id}`}>{dataset?.name || 'Dataset'}</Link></li>
            <li className="breadcrumb-item active" aria-current="page">Associations Matrix</li>
          </ol>
        </nav>

      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-diagram-3-fill text-success me-2"></i>Statistical Associations Engine
          </h2>
          <p className="text-muted small mb-0">
            Empirical bivariate hypothesis tests: Pearson Correlation, Welch's t-test, ANOVA, and Chi-Square.
          </p>
        </div>
        <button
          className="btn btn-outline-primary d-flex align-items-center gap-2"
          onClick={handleRefresh}
          disabled={refreshing || loading}
        >
          {refreshing ? (
            <>
              <span className="spinner-border spinner-border-sm" role="status"></span>
              <span>Re-evaluating Pairs...</span>
            </>
          ) : (
            <>
              <i className="bi bi-arrow-clockwise"></i>
              <span>Re-run Association Engine</span>
            </>
          )}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
        </div>
      )}

      {/* Controls & Filter Bar */}
      <div className="card shadow-sm border-0 mb-4 p-3 bg-white">
        <div className="row g-3 align-items-center">
          <div className="col-md-4">
            <div className="form-check form-switch">
              <input
                className="form-check-input"
                type="checkbox"
                id="sigCheck"
                checked={filterSigOnly}
                onChange={(e) => setFilterSigOnly(e.target.checked)}
              />
              <label className="form-check-label fw-semibold small" htmlFor="sigCheck">
                Show Statistically Significant Only (p &lt; 0.05)
              </label>
            </div>
          </div>
          <div className="col-md-5">
            <div className="d-flex align-items-center gap-2">
              <label className="form-label small fw-semibold text-nowrap mb-0">Statistical Test:</label>
              <select
                className="form-select form-select-sm"
                value={filterTestType}
                onChange={(e) => setFilterTestType(e.target.value)}
              >
                <option value="ALL">All Test Architectures</option>
                <option value="PEARSON">Pearson Correlation (Num × Num)</option>
                <option value="WELCH_T_TEST">Welch's t-test (Num × 2-Cat)</option>
                <option value="ONE_WAY_ANOVA">One-Way ANOVA (Num × Multi-Cat)</option>
                <option value="CHI_SQUARE">Chi-Square Independence (Cat × Cat)</option>
              </select>
            </div>
          </div>
          <div className="col-md-3 text-md-end text-muted small">
            Showing <strong>{filteredAssociations.length}</strong> of {associations.length} pairings
          </div>
        </div>
      </div>

      {/* Associations Table */}
      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status"></div>
          <p className="text-muted small mt-2">Computing empirical pairwise statistics...</p>
        </div>
      ) : filteredAssociations.length === 0 ? (
        <div className="card text-center py-5 border-dashed">
          <div className="card-body">
            <i className="bi bi-info-circle display-4 text-muted mb-2 d-block"></i>
            <h6 className="fw-bold">No Associations Found Matching Filters</h6>
            <p className="text-muted small">Try disabling the filter to view all computed statistical pairs.</p>
          </div>
        </div>
      ) : (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light small">
                <tr>
                  <th className="ps-3">Variable 1</th>
                  <th>Variable 2</th>
                  <th>Test Architecture</th>
                  <th>Statistic</th>
                  <th>p-value</th>
                  <th>Effect Size</th>
                  <th>Sample Size (N)</th>
                  <th>Significance</th>
                  <th>Interpretation & Scientific Context</th>
                  <th className="text-end pe-3">Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredAssociations.map((a) => (
                  <tr key={a.id} className={a.isSignificant ? 'table-light' : ''}>
                    <td className="ps-3 fw-bold text-dark">{a.var1}</td>
                    <td className="fw-bold text-dark">{a.var2}</td>
                    <td>{getTestBadge(a.testType)}</td>
                    <td className="small font-monospace">
                      {a.statistic?.toFixed(3)}
                      {a.degreesOfFreedom != null && (
                        <span className="text-muted ms-1">(df={a.degreesOfFreedom?.toFixed(0)})</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${a.pValue < 0.05 ? 'bg-success' : 'bg-secondary'}`}>
                        {a.pValue < 0.0001 ? '< 0.0001' : a.pValue?.toFixed(4)}
                      </span>
                    </td>
                    <td className="small font-monospace fw-semibold">{a.effectSize?.toFixed(3)}</td>
                    <td className="small">{a.sampleSize}</td>
                    <td>
                      {a.isSignificant ? (
                        <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25">
                          <i className="bi bi-check-circle-fill me-1"></i>Significant
                        </span>
                      ) : (
                        <span className="badge bg-light text-muted border">
                          Not Significant
                        </span>
                      )}
                    </td>
                    <td className="small text-secondary" style={{ maxWidth: '320px' }}>
                      {a.interpretation}
                    </td>
                    <td className="text-end pe-3">
                      <button
                        className="btn btn-primary btn-sm d-flex align-items-center gap-1 ms-auto"
                        onClick={() => handleInvestigate(a)}
                        title="Formulate Hypothesis & Research Question"
                      >
                        <i className="bi bi-search"></i>
                        <span>Investigate</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      </WorkflowNavigator>
    </div>
  );
};

export default AssociationsPage;
