import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { datasetService } from '../services/datasetService';

const DatasetsPage = () => {
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Upload modal state
  const [file, setFile] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [showUploadModal, setShowUploadModal] = useState(false);

  const navigate = useNavigate();

  const loadDatasets = async () => {
    try {
      setLoading(true);
      setError('');
      const res = await datasetService.listDatasets();
      if (res && res.data) {
        setDatasets(res.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load datasets.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDatasets();
  }, []);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);
      if (!name) {
        setName(selectedFile.name.replace(/\.[^/.]+$/, ''));
      }
    }
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a valid CSV file.');
      return;
    }

    setUploading(true);
    setError('');
    setSuccess('');

    try {
      const res = await datasetService.uploadDataset(file, name, description);
      setSuccess('Dataset uploaded and profiled successfully!');
      setShowUploadModal(false);
      setFile(null);
      setName('');
      setDescription('');
      loadDatasets();
      if (res && res.data && res.data.id) {
        navigate(`/datasets/${res.data.id}/overview`);
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id, datasetName) => {
    if (!window.confirm(`Are you sure you want to delete dataset "${datasetName}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await datasetService.deleteDataset(id);
      setSuccess(`Dataset "${datasetName}" deleted.`);
      loadDatasets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete dataset.');
    }
  };

  const getScoreBadge = (score) => {
    if (score == null) return <span className="badge bg-secondary">N/A</span>;
    if (score >= 80) return <span className="badge bg-success">{score.toFixed(1)} / 100</span>;
    if (score >= 50) return <span className="badge bg-warning text-dark">{score.toFixed(1)} / 100</span>;
    return <span className="badge bg-danger">{score.toFixed(1)} / 100</span>;
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-database text-primary me-2"></i>Dataset Repository
          </h2>
          <p className="text-muted small mb-0">
            Upload, profile, and inspect empirical datasets for automated scientific exploration.
          </p>
        </div>
        <button
          className="btn btn-primary d-flex align-items-center gap-2"
          onClick={() => setShowUploadModal(true)}
        >
          <i className="bi bi-cloud-arrow-up-fill"></i>
          <span>Upload Dataset</span>
        </button>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
          <button type="button" className="btn-close" onClick={() => setError('')}></button>
        </div>
      )}

      {success && (
        <div className="alert alert-success alert-dismissible fade show" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {success}
          <button type="button" className="btn-close" onClick={() => setSuccess('')}></button>
        </div>
      )}

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading datasets...</span>
          </div>
          <p className="text-muted mt-2 small">Scanning dataset repository...</p>
        </div>
      ) : datasets.length === 0 ? (
        <div className="card text-center py-5 border-dashed">
          <div className="card-body">
            <i className="bi bi-folder-x display-4 text-muted mb-3 d-block"></i>
            <h5 className="fw-bold">No Datasets Ingested Yet</h5>
            <p className="text-muted small max-w-md mx-auto">
              Upload your first empirical CSV file to trigger automated data quality profiling,
              hypothesis generation, and machine learning pipelines.
            </p>
            <button
              className="btn btn-outline-primary mt-2"
              onClick={() => setShowUploadModal(true)}
            >
              <i className="bi bi-upload me-1"></i> Upload First Dataset
            </button>
          </div>
        </div>
      ) : (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col" className="ps-3">Dataset Name</th>
                  <th scope="col">Rows</th>
                  <th scope="col">Columns</th>
                  <th scope="col">Macro Type</th>
                  <th scope="col">Quality Score</th>
                  <th scope="col">Created</th>
                  <th scope="col" className="text-end pe-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {datasets.map((d) => (
                  <tr key={d.id}>
                    <td className="ps-3">
                      <Link to={`/datasets/${d.id}/overview`} className="fw-semibold text-decoration-none text-primary">
                        {d.name}
                      </Link>
                      <div className="text-muted small text-truncate" style={{ maxWidth: '240px' }}>
                        {d.originalFilename}
                      </div>
                    </td>
                    <td><span className="badge bg-light text-dark border">{d.rowCount?.toLocaleString()}</span></td>
                    <td><span className="badge bg-light text-dark border">{d.columnCount} cols</span></td>
                    <td>
                      <span className="badge bg-info bg-opacity-10 text-info border border-info border-opacity-25">
                        {d.datasetType || 'MIXED'}
                      </span>
                    </td>
                    <td>{getScoreBadge(d.qualityScore)}</td>
                    <td className="text-muted small">
                      {d.createdAt ? new Date(d.createdAt).toLocaleDateString() : 'N/A'}
                    </td>
                    <td className="text-end pe-3">
                      <div className="btn-group btn-group-sm">
                        <Link
                          to={`/datasets/${d.id}/overview`}
                          className="btn btn-outline-primary"
                          title="Inspect Profile & EDA"
                        >
                          <i className="bi bi-graph-up me-1"></i> Inspect
                        </Link>
                        <Link
                          to={`/datasets/${d.id}/associations`}
                          className="btn btn-outline-success"
                          title="Find Associations"
                        >
                          <i className="bi bi-diagram-3 me-1"></i> Associations
                        </Link>
                        <button
                          onClick={() => handleDelete(d.id, d.name)}
                          className="btn btn-outline-danger"
                          title="Delete Dataset"
                        >
                          <i className="bi bi-trash"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow">
              <div className="modal-header">
                <h5 className="modal-title fw-bold">
                  <i className="bi bi-file-earmark-spreadsheet text-primary me-2"></i>
                  Upload Empirical Dataset
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowUploadModal(false)}
                  disabled={uploading}
                ></button>
              </div>
              <form onSubmit={handleUploadSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label fw-semibold small">CSV File (.csv)*</label>
                    <input
                      type="file"
                      className="form-control"
                      accept=".csv"
                      onChange={handleFileChange}
                      required
                      disabled={uploading}
                    />
                    <div className="form-text small">
                      Maximum 50MB. File must be valid RFC-4180 CSV with header row.
                    </div>
                  </div>

                  <div className="mb-3">
                    <label className="form-label fw-semibold small">Dataset Name</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Clinical Trial Cohort A"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      disabled={uploading}
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label fw-semibold small">Research Description (Optional)</label>
                    <textarea
                      className="form-control"
                      rows="3"
                      placeholder="Context regarding collection methodology, target variables, or research scope..."
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      disabled={uploading}
                    ></textarea>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setShowUploadModal(false)}
                    disabled={uploading}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary d-flex align-items-center gap-2"
                    disabled={uploading}
                  >
                    {uploading ? (
                      <>
                        <span className="spinner-border spinner-border-sm" role="status"></span>
                        <span>Profiling Dataset...</span>
                      </>
                    ) : (
                      <>
                        <i className="bi bi-cloud-arrow-up-fill"></i>
                        <span>Start Processing</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DatasetsPage;
