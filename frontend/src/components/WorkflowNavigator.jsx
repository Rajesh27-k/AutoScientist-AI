import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useWorkflow, WORKFLOW_STAGES } from '../context/WorkflowContext';
import PrerequisiteAlert from './PrerequisiteAlert';
import RecommendedNextStep from './RecommendedNextStep';

const WorkflowNavigator = ({
  currentStage,
  position = 'both', // 'top', 'bottom', or 'both' (when used as wrapper with children)
  children,
  onCustomNextAction,
}) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const {
    datasetId,
    dataset,
    selectDataset,
    isStageCompleted,
    getStageStatus,
    getPreviousStage,
    getNextStage,
  } = useWorkflow();

  // Synchronize URL id parameter with workflow context
  useEffect(() => {
    if (id && id !== datasetId) {
      selectDataset(id);
    }
  }, [id, datasetId, selectDataset]);

  const activeId = id || datasetId;
  const prevStage = getPreviousStage(currentStage);
  const nextStage = getNextStage(currentStage);
  const isFinalStage = currentStage === 'report';

  // Render Guided Next/Previous Navigation Buttons (Mode 2)
  const renderGuidedNavControls = (locationTag) => {
    return (
      <div
        className={`d-flex flex-wrap justify-content-between align-items-center gap-2 py-3 ${
          locationTag === 'top' ? 'mb-3 border-bottom' : 'mt-4 pt-3 border-top'
        }`}
      >
        <div>
          {prevStage ? (
            <Link
              to={prevStage.path}
              className="btn btn-outline-secondary d-inline-flex align-items-center gap-2 fw-semibold shadow-sm px-3 py-2"
            >
              <i className="bi bi-arrow-left"></i>
              <span>Previous: {prevStage.name}</span>
            </Link>
          ) : (
            <button
              className="btn btn-outline-secondary d-inline-flex align-items-center gap-2 opacity-50 px-3 py-2"
              disabled
            >
              <i className="bi bi-arrow-left"></i>
              <span>Start of Workflow</span>
            </button>
          )}
        </div>

        <div className="d-flex align-items-center gap-2">
          {isFinalStage ? (
            <button
              className="btn btn-success d-inline-flex align-items-center gap-2 fw-semibold px-4 py-2 shadow-sm"
              disabled
            >
              <i className="bi bi-check2-circle fs-5"></i>
              <span>Analysis Complete ✓</span>
            </button>
          ) : nextStage ? (
            <Link
              to={nextStage.path}
              className="btn btn-primary d-inline-flex align-items-center gap-2 fw-semibold shadow-sm px-4 py-2"
            >
              <span>Next: {nextStage.name}</span>
              <i className="bi bi-arrow-right"></i>
            </Link>
          ) : null}
        </div>
      </div>
    );
  };

  // Render Persistent Direct Stepper Bar (Mode 1)
  const renderDirectStepper = () => {
    return (
      <div className="card shadow-sm border-0 mb-4 bg-white overflow-hidden">
        {/* Dataset Workflow Header */}
        <div className="card-header bg-white py-2 px-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div className="d-flex align-items-center gap-2">
            <span className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 px-2 py-1">
              <i className="bi bi-diagram-2 me-1"></i> Scientific Workflow
            </span>
            {dataset && (
              <span className="fw-semibold text-dark small text-truncate" style={{ maxWidth: '280px' }}>
                <i className="bi bi-database me-1 text-secondary"></i>
                {dataset.name}
              </span>
            )}
            {dataset?.rowCount && (
              <span className="badge bg-light text-muted border small d-none d-md-inline">
                {dataset.rowCount.toLocaleString()} rows &bull; {dataset.columnCount} vars
              </span>
            )}
          </div>

          <div className="d-flex align-items-center gap-2">
            <span className="small text-muted d-none d-sm-inline">
              Stage {WORKFLOW_STAGES.find((s) => s.key === currentStage)?.stepNumber || 1} of 13
            </span>
            <select
              className="form-select form-select-sm d-md-none"
              style={{ width: 'auto' }}
              value={currentStage}
              onChange={(e) => {
                const target = WORKFLOW_STAGES.find((s) => s.key === e.target.value);
                if (target) navigate(target.getPath(activeId));
              }}
            >
              {WORKFLOW_STAGES.map((s) => {
                const completed = isStageCompleted(s.key);
                const mark = completed ? '✓' : s.key === currentStage ? '●' : '○';
                return (
                  <option key={s.key} value={s.key}>
                    {mark} {s.stepNumber}. {s.name}
                  </option>
                );
              })}
            </select>
          </div>
        </div>

        {/* 13-Stage Horizontal Stepper */}
        <div className="card-body p-2 bg-light">
          <div
            className="d-flex align-items-stretch gap-1 overflow-x-auto pb-1"
            style={{
              scrollbarWidth: 'thin',
              WebkitOverflowScrolling: 'touch',
            }}
          >
            {WORKFLOW_STAGES.map((stage) => {
              const isActive = currentStage === stage.key;
              const isDone = isStageCompleted(stage.key);
              const status = getStageStatus(stage.key);
              const targetPath = stage.getPath(activeId);

              // Indicator logic: ✓ = completed, ● = current, ○ = pending
              let indicatorSymbol = '○';
              let badgeColor = 'bg-secondary bg-opacity-10 text-muted';
              if (isActive) {
                indicatorSymbol = '●';
                badgeColor = 'bg-primary text-white';
              } else if (isDone) {
                indicatorSymbol = '✓';
                badgeColor = 'bg-success text-white';
              }

              return (
                <button
                  key={stage.key}
                  type="button"
                  onClick={() => navigate(targetPath)}
                  title={`${stage.stepNumber}. ${stage.name} - ${
                    isDone ? 'Completed ✓' : isActive ? 'Current Stage ●' : 'Pending ○'
                  }${status?.summary ? ` (${status.summary})` : ''}`}
                  className={`btn text-start p-2 rounded border-0 transition flex-shrink-0 d-flex flex-column justify-content-between ${
                    isActive
                      ? 'bg-white shadow-sm border border-primary text-primary'
                      : isDone
                      ? 'bg-white bg-opacity-75 hover-shadow text-dark'
                      : 'bg-transparent text-secondary'
                  }`}
                  style={{
                    minWidth: '105px',
                    maxWidth: '125px',
                    borderLeft: isActive ? '3px solid var(--bs-primary)' : '1px solid rgba(0,0,0,0.06)',
                  }}
                >
                  <div className="d-flex justify-content-between align-items-center w-100 mb-1">
                    <span className="small text-muted fw-bold" style={{ fontSize: '0.72rem' }}>
                      #{stage.stepNumber}
                    </span>
                    <span
                      className={`badge rounded-circle p-1 d-inline-flex align-items-center justify-content-center ${badgeColor}`}
                      style={{ width: '18px', height: '18px', fontSize: '0.65rem', lineHeight: 1 }}
                    >
                      {indicatorSymbol}
                    </span>
                  </div>
                  <div
                    className={`fw-semibold text-truncate small ${isActive ? 'text-primary' : 'text-dark'}`}
                    style={{ fontSize: '0.8rem' }}
                  >
                    {stage.shortName}
                  </div>
                  <div
                    className="text-muted text-truncate"
                    style={{ fontSize: '0.68rem', lineHeight: '1.1' }}
                  >
                    {isDone ? (
                      <span className="text-success fw-medium">
                        <i className="bi bi-check"></i> Done
                      </span>
                    ) : isActive ? (
                      <span className="text-primary fw-medium">Active</span>
                    ) : (
                      <span>Pending</span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    );
  };

  // If used purely as top bar
  if (position === 'top') {
    return (
      <div className="workflow-navigator-top">
        {renderDirectStepper()}
        {renderGuidedNavControls('top')}
        <PrerequisiteAlert stageKey={currentStage} />
      </div>
    );
  }

  // If used purely as bottom bar
  if (position === 'bottom') {
    return (
      <div className="workflow-navigator-bottom">
        <RecommendedNextStep stageKey={currentStage} onCustomAction={onCustomNextAction} />
        {renderGuidedNavControls('bottom')}
      </div>
    );
  }

  // Wrapper mode: wrap children with both top and bottom navigation
  return (
    <div className="workflow-container">
      {/* MODE 1 Direct Stepper + MODE 2 Top Guided Nav */}
      {renderDirectStepper()}
      {renderGuidedNavControls('top')}

      {/* Prerequisite Alert Banner */}
      <PrerequisiteAlert stageKey={currentStage} />

      {/* Actual Stage Content */}
      <div className="workflow-stage-content my-3">{children}</div>

      {/* Recommended Next Step Card + MODE 2 Bottom Guided Nav */}
      <RecommendedNextStep stageKey={currentStage} onCustomAction={onCustomNextAction} />
      {renderGuidedNavControls('bottom')}
    </div>
  );
};

export default WorkflowNavigator;
