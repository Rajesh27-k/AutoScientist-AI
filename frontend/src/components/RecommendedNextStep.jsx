import React from 'react';
import { Link } from 'react-router-dom';
import { useWorkflow } from '../context/WorkflowContext';

const RecommendedNextStep = ({ stageKey, onCustomAction }) => {
  const { datasetId, getRecommendedNextStep, isStageCompleted } = useWorkflow();
  const step = getRecommendedNextStep(stageKey);

  if (!step || !datasetId) {
    return null;
  }

  // Only highlight recommended next step if current stage is completed (or on report)
  const completed = isStageCompleted(stageKey);

  return (
    <div className={`card border-0 shadow-sm mb-4 ${completed ? 'bg-primary bg-opacity-10 border border-primary border-opacity-25' : 'bg-light border'}`}>
      <div className="card-body p-4">
        <div className="d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3">
          <div className="d-flex align-items-center gap-3">
            <div className={`rounded-circle d-flex align-items-center justify-content-center p-3 fs-3 ${completed ? 'bg-primary text-white shadow-sm' : 'bg-secondary bg-opacity-10 text-secondary'}`} style={{ width: '56px', height: '56px' }}>
              <i className={`bi ${step.icon || 'bi-arrow-right-circle'}`}></i>
            </div>
            <div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <span className={`badge ${completed ? 'bg-primary' : 'bg-secondary'} text-uppercase small`}>
                  Recommended Next Step
                </span>
                {completed && (
                  <span className="badge bg-success-subtle text-success border border-success-subtle small">
                    <i className="bi bi-check-circle-fill me-1"></i> Current Stage Done
                  </span>
                )}
              </div>
              <h5 className="fw-bold mb-1 text-dark">{step.title}</h5>
              <p className="text-secondary mb-0 small">{step.description}</p>
            </div>
          </div>

          <div className="text-nowrap mt-2 mt-md-0">
            {step.isPdfDownload && onCustomAction ? (
              <button onClick={onCustomAction} className="btn btn-primary fw-semibold px-4 py-2 shadow-sm d-flex align-items-center gap-2">
                <i className="bi bi-file-earmark-pdf-fill"></i>
                <span>{step.actionText}</span>
              </button>
            ) : (
              <Link to={step.targetPath} className="btn btn-primary fw-semibold px-4 py-2 shadow-sm d-flex align-items-center gap-2">
                <span>{step.actionText}</span>
                <i className="bi bi-arrow-right"></i>
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default RecommendedNextStep;
