import React from 'react';
import { Link } from 'react-router-dom';
import { useWorkflow, WORKFLOW_STAGES } from '../context/WorkflowContext';

const PrerequisiteAlert = ({ stageKey }) => {
  const { datasetId, getPrerequisiteInfo } = useWorkflow();
  const prereq = getPrerequisiteInfo(stageKey);

  if (!prereq || prereq.met) {
    return null;
  }

  const targetStage = WORKFLOW_STAGES.find((s) => s.key === prereq.prerequisiteStage);
  const targetPath = targetStage && datasetId ? targetStage.getPath(datasetId) : '/datasets';
  const targetLabel = targetStage ? targetStage.name : 'Prerequisite Stage';

  return (
    <div className="card border-warning border-2 bg-warning bg-opacity-10 shadow-sm mb-4">
      <div className="card-body p-4 d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3">
        <div className="d-flex align-items-start gap-3">
          <div className="text-warning fs-2 lh-1 mt-1">
            <i className="bi bi-exclamation-triangle-fill"></i>
          </div>
          <div>
            <h5 className="fw-bold text-dark mb-1">Prerequisite Required</h5>
            <p className="text-secondary mb-0">
              {prereq.message || 'This analysis stage requires prior operations to be executed first.'}
            </p>
          </div>
        </div>
        <div className="text-nowrap">
          <Link to={targetPath} className="btn btn-warning fw-semibold px-4 py-2 shadow-sm">
            Go to {targetLabel} <i className="bi bi-arrow-right ms-1"></i>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default PrerequisiteAlert;
