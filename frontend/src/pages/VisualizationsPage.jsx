import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
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

const isIdentifierColumn = (col) => {
  if (!col) return false;
  if (col.dataType === 'IDENTIFIER') return true;
  const n = col.columnName.toLowerCase();
  return (
    n === 'id' ||
    n === 'uuid' ||
    n.endsWith('_id') ||
    n.startsWith('id_') ||
    (col.columnName.endsWith('Id') && col.columnName.length > 2) ||
    n.includes('customer_id') ||
    n.includes('customerid') ||
    n.includes('user_id') ||
    n.includes('userid') ||
    n.includes('record_id') ||
    n.includes('recordid') ||
    n.includes('patient_id') ||
    n.includes('patientid') ||
    n.includes('student_id') ||
    n.includes('studentid') ||
    n.includes('transaction_id') ||
    n.includes('claim_id')
  );
};

export default function VisualizationsPage() {
  const { id } = useParams();
  const { datasetId: workflowDatasetId, selectDataset } = useWorkflow();
  const activeId = id || workflowDatasetId;

  const [dataset, setDataset] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [corrMatrix, setCorrMatrix] = useState(null);
  const [boxPlots, setBoxPlots] = useState([]);
  const [selectedNumVar, setSelectedNumVar] = useState('');
  const [selectedCatVar, setSelectedCatVar] = useState('');
  const [groupedSummary, setGroupedSummary] = useState([]);
  const [loadingGrouped, setLoadingGrouped] = useState(false);

  const loadData = useCallback(async (currentId) => {
    if (!currentId) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError('');
      if (selectDataset) selectDataset(currentId);

      const [dsRes, corrRes, boxRes] = await Promise.all([
        datasetService.getDataset(currentId),
        analysisService.getCorrelationMatrix(currentId).catch(() => null),
        analysisService.getBoxPlots(currentId).catch(() => null),
      ]);

      if (dsRes && dsRes.data) {
        setDataset(dsRes.data);
        const cols = dsRes.data.columns || [];

        // Filter and rank meaningful numeric variables, excluding identifiers
        const numVars = cols
          .filter((c) => c.dataType === 'NUMERIC' && !isIdentifierColumn(c))
          .sort((a, b) => {
            const highValueTerms = ['age', 'salary', 'income', 'score', 'hours', 'amount', 'bp', 'rate', 'balance'];
            const aHas = highValueTerms.some((t) => a.columnName.toLowerCase().includes(t));
            const bHas = highValueTerms.some((t) => b.columnName.toLowerCase().includes(t));
            if (aHas && !bHas) return -1;
            if (!aHas && bHas) return 1;
            return 0;
          });

        const catVars = cols.filter(
          (c) => (c.dataType === 'CATEGORICAL' || c.dataType === 'BOOLEAN') && !isIdentifierColumn(c)
        );

        const initialNum = numVars.length > 0 ? numVars[0].columnName : '';
        const initialCat = catVars.length > 0 ? catVars[0].columnName : '';

        setSelectedNumVar(initialNum);
        setSelectedCatVar(initialCat);

        if (initialNum && initialCat) {
          const grp = await analysisService
            .getGroupedSummary(currentId, initialNum, initialCat)
            .catch(() => null);
          if (grp && grp.data) setGroupedSummary(grp.data);
        }
      }

      if (corrRes && corrRes.data) setCorrMatrix(corrRes.data);
      if (boxRes && boxRes.data) setBoxPlots(boxRes.data);
    } catch (err) {
      console.error('Failed to load visualization data:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load visualization data.');
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
  }, [activeId, loadData]);

  const handleGroupedChange = async (num, cat) => {
    setSelectedNumVar(num);
    setSelectedCatVar(cat);
    if (!num || !cat || !activeId) return;
    try {
      setLoadingGrouped(true);
      const res = await analysisService.getGroupedSummary(activeId, num, cat);
      if (res && res.data) {
        setGroupedSummary(res.data);
      } else {
        setGroupedSummary([]);
      }
    } catch (err) {
      console.error('Failed to update grouped summary:', err);
      setGroupedSummary([]);
    } finally {
      setLoadingGrouped(false);
    }
  };

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="text-muted mt-2 small">Rendering scientific visualizations...</p>
      </div>
    );
  }

  const columns = dataset?.columns || [];
  const numCols = columns.filter((c) => c.dataType === 'NUMERIC' && !isIdentifierColumn(c));
  const catCols = columns.filter(
    (c) => (c.dataType === 'CATEGORICAL' || c.dataType === 'BOOLEAN') && !isIdentifierColumn(c)
  );

  // Grouped chart data
  const hasValidObservations =
    groupedSummary &&
    groupedSummary.length > 0 &&
    groupedSummary.some((g) => g.count > 0 && g.mean != null && !isNaN(g.mean));

  const groupedChartData = hasValidObservations
    ? {
        labels: groupedSummary.map((g) => g.groupValue || g.groupName),
        datasets: [
          {
            label: `Mean of ${selectedNumVar}`,
            data: groupedSummary.map((g) => g.mean),
            backgroundColor: 'rgba(54, 162, 235, 0.75)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
          {
            label: `Median of ${selectedNumVar}`,
            data: groupedSummary.map((g) => g.median),
            backgroundColor: 'rgba(255, 159, 64, 0.75)',
            borderColor: 'rgba(255, 159, 64, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
        ],
      }
    : null;

  return (
    <div className="container py-4">
      <WorkflowNavigator currentStage="visualizations">
        <div className="mb-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h3 className="fw-bold mb-1">
              <i className="bi bi-pie-chart-fill text-primary me-2"></i>
              Scientific Visualizations & Distribution Gallery
            </h3>
            <p className="text-secondary small mb-0">
              Pairwise correlation structures, demographic group comparisons, and box plot distributions.
            </p>
          </div>
          {dataset && (
            <span className="badge bg-light text-dark border p-2">
              <i className="bi bi-database me-1 text-primary"></i>
              <strong>{dataset.name}</strong> ({dataset.rowCount} rows)
            </span>
          )}
        </div>

        {error && (
          <div className="alert alert-danger mb-4 shadow-sm d-flex justify-content-between align-items-center" role="alert">
            <div>
              <i className="bi bi-exclamation-triangle-fill me-2"></i>
              {error}
            </div>
            <button
              className="btn btn-sm btn-outline-danger"
              onClick={() => loadData(activeId)}
            >
              <i className="bi bi-arrow-clockwise me-1"></i> Retry
            </button>
          </div>
        )}

        {/* 1. Grouped Feature Breakdown Chart */}
        <div className="card shadow-sm border-0 mb-4">
          <div className="card-header bg-white py-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <h5 className="fw-bold mb-0">
                <i className="bi bi-bar-chart-steps text-primary me-2"></i>
                Bivariate Distribution & Group Breakdown
              </h5>
              <small className="text-muted">Compare continuous variable metrics partitioned across categorical cohorts.</small>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <select
                className="form-select form-select-sm"
                value={selectedNumVar}
                onChange={(e) => handleGroupedChange(e.target.value, selectedCatVar)}
                style={{ width: 'auto' }}
              >
                {numCols.length === 0 ? (
                  <option value="">No continuous variables</option>
                ) : (
                  numCols.map((c) => (
                    <option key={c.columnName} value={c.columnName}>
                      Numeric: {c.columnName}
                    </option>
                  ))
                )}
              </select>
              <select
                className="form-select form-select-sm"
                value={selectedCatVar}
                onChange={(e) => handleGroupedChange(selectedNumVar, e.target.value)}
                style={{ width: 'auto' }}
              >
                {catCols.length === 0 ? (
                  <option value="">No categorical variables</option>
                ) : (
                  catCols.map((c) => (
                    <option key={c.columnName} value={c.columnName}>
                      Group: {c.columnName}
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>
          <div className="card-body p-4">
            {loadingGrouped ? (
              <div className="text-center py-4">
                <div className="spinner-border spinner-border-sm text-primary mb-2"></div>
                <div className="small text-muted">Computing cohort distributions...</div>
              </div>
            ) : groupedChartData ? (
              <div style={{ height: '340px' }}>
                <Bar
                  data={groupedChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: { position: 'top' },
                    },
                    scales: {
                      y: {
                        title: { display: true, text: selectedNumVar },
                      },
                      x: {
                        title: { display: true, text: selectedCatVar },
                      },
                    },
                  }}
                />
              </div>
            ) : (
              <div className="p-5 text-center bg-light rounded border">
                <i className="bi bi-bar-chart text-muted fs-1 d-block mb-2"></i>
                <h6 className="fw-semibold text-secondary mb-1">No valid observations available for this visualization.</h6>
                <p className="text-muted small mb-0">
                  Select alternative continuous and grouping features above to render empirical distribution comparisons.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 2. Correlation Matrix Heatmap */}
        {corrMatrix && corrMatrix.variables && corrMatrix.variables.length > 0 && (
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-header bg-white py-3 border-bottom">
              <h5 className="fw-bold mb-0">
                <i className="bi bi-grid-3x3-gap-fill text-success me-2"></i>
                Pearson Correlation Structure
              </h5>
              <small className="text-muted">Empirical pairwise correlation coefficients (-1.0 to +1.0) between numeric features (Identifiers excluded).</small>
            </div>
            <div className="card-body p-3">
              <div className="table-responsive">
                <table className="table table-sm table-bordered text-center align-middle mb-0 font-monospace small">
                  <thead className="table-light">
                    <tr>
                      <th className="text-start">Feature</th>
                      {corrMatrix.variables.map((v) => (
                        <th key={v} className="text-truncate" style={{ maxWidth: '110px' }}>
                          {v}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {corrMatrix.variables.map((rowVar, rIdx) => (
                      <tr key={rowVar}>
                        <td className="text-start fw-bold text-dark">{rowVar}</td>
                        {corrMatrix.matrix[rIdx].map((val, cIdx) => {
                          let bg = '#ffffff';
                          let text = '#000000';
                          if (rIdx === cIdx) {
                            bg = '#e9ecef';
                          } else if (val > 0.5) {
                            bg = '#d1e7dd';
                            text = '#0f5132';
                          } else if (val > 0.2) {
                            bg = '#e8f5e9';
                          } else if (val < -0.5) {
                            bg = '#f8d7da';
                            text = '#842029';
                          } else if (val < -0.2) {
                            bg = '#fce4ec';
                          }
                          return (
                            <td key={cIdx} style={{ backgroundColor: bg, color: text, padding: '8px' }}>
                              {val.toFixed(2)}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* 3. Five-Number Distribution Summary (Box Plots) */}
        {boxPlots && boxPlots.length > 0 && (
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-header bg-white py-3 border-bottom">
              <h5 className="fw-bold mb-0">
                <i className="bi bi-symmetry-vertical text-info me-2"></i>
                Five-Number Distribution Summary
              </h5>
              <small className="text-muted">Min, Q1, Median, Q3, Max, IQR, and Outlier frequency for continuous variables.</small>
            </div>
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Variable</th>
                      <th>Min</th>
                      <th>Q1 (25%)</th>
                      <th>Median (50%)</th>
                      <th>Q3 (75%)</th>
                      <th>Max</th>
                      <th>IQR</th>
                      <th>Outliers Detected</th>
                    </tr>
                  </thead>
                  <tbody>
                    {boxPlots.map((bp) => {
                      const vName = bp.variable || bp.variableName;
                      return (
                        <tr key={vName}>
                          <td className="fw-bold text-primary">{vName}</td>
                          <td>{bp.min?.toFixed(2)}</td>
                          <td>{bp.q1?.toFixed(2)}</td>
                          <td className="fw-bold text-dark">{bp.median?.toFixed(2)}</td>
                          <td>{bp.q3?.toFixed(2)}</td>
                          <td>{bp.max?.toFixed(2)}</td>
                          <td>{bp.iqr?.toFixed(2)}</td>
                          <td>
                            {bp.outliers && bp.outliers.length > 0 ? (
                              <span className="badge bg-warning text-dark">
                                {bp.outliers.length} outliers ({bp.outliers.slice(0, 3).map((o) => o.toFixed(1)).join(', ')}...)
                              </span>
                            ) : (
                              <span className="badge bg-success-subtle text-success">None detected</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </WorkflowNavigator>
    </div>
  );
}
