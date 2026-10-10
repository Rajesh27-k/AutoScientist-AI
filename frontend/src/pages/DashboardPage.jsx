import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { analysisService } from '../services/analysisService';

const DashboardPage = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardStats();
  }, []);

  const loadDashboardStats = async () => {
    try {
      const res = await analysisService.getDashboardStats();
      setStats(res.data);
    } catch (err) {
      console.error('Failed to load dashboard stats', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container-fluid py-4 px-4">
      {/* Banner */}
      <div className="row mb-4">
        <div className="col-12">
          <div className="card bg-dark text-white p-4 shadow-sm border-0 position-relative overflow-hidden">
            <div className="position-absolute end-0 top-0 opacity-10 p-4 d-none d-md-block">
              <i className="bi bi-diagram-3-fill" style={{ fontSize: '10rem' }}></i>
            </div>
            <div className="position-relative">
              <span className="badge bg-primary mb-2 px-3 py-2">AutoScientist AI Platform</span>
              <h2 className="fw-bold mb-1">Welcome back, {user?.name || 'Researcher'}!</h2>
              <p className="text-secondary mb-3">
                Transform raw empirical datasets into verifiable, evidence-based scientific findings.
              </p>
              <div className="d-flex flex-wrap gap-2">
                <Link to="/datasets" className="btn btn-primary d-flex align-items-center gap-2">
                  <i className="bi bi-upload"></i>
                  <span>Upload Dataset</span>
                </Link>
                <Link to="/automl" className="btn btn-outline-light d-flex align-items-center gap-2">
                  <i className="bi bi-cpu"></i>
                  <span>AutoML & XAI</span>
                </Link>
                <Link to="/ask" className="btn btn-outline-light d-flex align-items-center gap-2">
                  <i className="bi bi-chat-left-dots"></i>
                  <span>Ask Dataset</span>
                </Link>
                <Link to="/reports" className="btn btn-outline-light d-flex align-items-center gap-2">
                  <i className="bi bi-file-earmark-pdf"></i>
                  <span>Scientific Reports</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Live Metric Cards */}
      <div className="row g-4 mb-4">
        <div className="col-md-3">
          <div className="card shadow-sm border-0 h-100 p-3">
            <div className="d-flex align-items-center justify-content-between mb-2">
              <span className="text-muted small fw-semibold">DATASETS & ROWS</span>
              <div className="p-2 bg-primary bg-opacity-10 text-primary rounded">
                <i className="bi bi-database"></i>
              </div>
            </div>
            <h3 className="fw-bold mb-0 text-primary">
              {loading ? '...' : stats?.totalDatasets ?? 0}
            </h3>
            <span className="text-muted small">
              {stats?.totalObservations ? `${stats.totalObservations.toLocaleString()} rows profiled` : 'No rows yet'}
            </span>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card shadow-sm border-0 h-100 p-3">
            <div className="d-flex align-items-center justify-content-between mb-2">
              <span className="text-muted small fw-semibold">BIVARIATE ASSOCIATIONS</span>
              <div className="p-2 bg-success bg-opacity-10 text-success rounded">
                <i className="bi bi-diagram-2"></i>
              </div>
            </div>
            <h3 className="fw-bold mb-0 text-success">
              {loading ? '...' : stats?.totalAssociations ?? 0}
            </h3>
            <span className="text-muted small">Pearson, Welch, ANOVA, χ²</span>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card shadow-sm border-0 h-100 p-3">
            <div className="d-flex align-items-center justify-content-between mb-2">
              <span className="text-muted small fw-semibold">MODELS BENCHMARKED</span>
              <div className="p-2 bg-warning bg-opacity-10 text-warning rounded">
                <i className="bi bi-cpu"></i>
              </div>
            </div>
            <h3 className="fw-bold mb-0 text-warning">
              {loading ? '...' : stats?.totalModelsTrained ?? 0}
            </h3>
            <span className="text-muted small">Ridge, Trees, Forests</span>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card shadow-sm border-0 h-100 p-3">
            <div className="d-flex align-items-center justify-content-between mb-2">
              <span className="text-muted small fw-semibold">AVERAGE DATA QUALITY</span>
              <div className="p-2 bg-info bg-opacity-10 text-info rounded">
                <i className="bi bi-shield-check"></i>
              </div>
            </div>
            <h3 className="fw-bold mb-0 text-info">
              {loading ? '...' : `${stats?.averageQualityScore ?? 0} / 100`}
            </h3>
            <span className="text-muted small">Audited Quality Index</span>
          </div>
        </div>
      </div>

      {/* Provenance & Activity Timeline */}
      <div className="card shadow-sm border-0 mb-4">
        <div className="card-header bg-white py-3 border-0 d-flex justify-content-between align-items-center">
          <h5 className="fw-bold mb-0">
            <i className="bi bi-clock-history text-secondary me-2"></i>
            Autonomous Research Provenance & Audit Trail
          </h5>
          <span className="badge bg-light text-dark border">
            {stats?.recentActivity?.length ?? 0} Events Recorded
          </span>
        </div>
        <div className="card-body p-0">
          <div className="list-group list-group-flush">
            {(!stats?.recentActivity || stats.recentActivity.length === 0) ? (
              <div className="p-4 text-center text-muted">
                No research events recorded yet. Start by uploading a dataset.
              </div>
            ) : (
              stats.recentActivity.map((event, idx) => (
                <div key={idx} className="list-group-item p-3 d-flex align-items-start gap-3">
                  <div className="mt-1">
                    {event.eventType === 'DATASET_UPLOAD' && (
                      <i className="bi bi-cloud-arrow-up-fill text-primary fs-5"></i>
                    )}
                    {event.eventType === 'FINDING_PRODUCED' && (
                      <i className="bi bi-award-fill text-success fs-5"></i>
                    )}
                    {event.eventType === 'MODEL_TRAINED' && (
                      <i className="bi bi-cpu-fill text-warning fs-5"></i>
                    )}
                    {event.eventType === 'QUESTION_ASKED' && (
                      <i className="bi bi-question-circle-fill text-info fs-5"></i>
                    )}
                    {event.eventType === 'REPORT_EXPORTED' && (
                      <i className="bi bi-file-earmark-pdf-fill text-danger fs-5"></i>
                    )}
                  </div>
                  <div className="flex-grow-1">
                    <div className="d-flex justify-content-between align-items-center">
                      <h6 className="fw-semibold mb-1">{event.title}</h6>
                      <small className="text-muted">
                        {event.timestamp ? new Date(event.timestamp).toLocaleString() : ''}
                      </small>
                    </div>
                    <p className="text-muted mb-0 small">{event.description}</p>
                    {event.datasetName && (
                      <small className="text-primary mt-1 d-inline-block">
                        <i className="bi bi-database me-1"></i>
                        {event.datasetName}
                      </small>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
