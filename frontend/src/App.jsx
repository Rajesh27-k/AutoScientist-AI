import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { WorkflowProvider } from './context/WorkflowContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import DatasetsPage from './pages/DatasetsPage';
import DatasetDetailPage from './pages/DatasetDetailPage';
import AssociationsPage from './pages/AssociationsPage';
import ResearchWorkflowPage from './pages/ResearchWorkflowPage';
import AutoMlPage from './pages/AutoMlPage';
import VisualizationsPage from './pages/VisualizationsPage';
import DriftPage from './pages/DriftPage';
import AskDatasetPage from './pages/AskDatasetPage';
import FindingsPage from './pages/FindingsPage';
import ExperimentsPage from './pages/ExperimentsPage';
import ReportsPage from './pages/ReportsPage';

function App() {
  return (
    <Router>
      <AuthProvider>
        <WorkflowProvider>
          <div className="d-flex flex-column min-vh-100">
            <Navbar />
            <main className="flex-grow-1">
              <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* Dashboard */}
                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute>
                      <DashboardPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 1: Upload Dataset / List */}
                <Route
                  path="/datasets"
                  element={
                    <ProtectedRoute>
                      <DatasetsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 2, 3, 4: Overview, Quality, EDA */}
                <Route
                  path="/datasets/:id"
                  element={
                    <ProtectedRoute>
                      <DatasetDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/overview"
                  element={
                    <ProtectedRoute>
                      <DatasetDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/quality"
                  element={
                    <ProtectedRoute>
                      <DatasetDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/eda"
                  element={
                    <ProtectedRoute>
                      <DatasetDetailPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 5: Associations */}
                <Route
                  path="/associations"
                  element={
                    <ProtectedRoute>
                      <AssociationsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/associations"
                  element={
                    <ProtectedRoute>
                      <AssociationsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 6, 7, 8: Research Questions, Hypotheses, Statistical Testing */}
                <Route
                  path="/research"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/hypotheses"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/statistics"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/research"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/hypotheses"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/statistics"
                  element={
                    <ProtectedRoute>
                      <ResearchWorkflowPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 9: Machine Learning */}
                <Route
                  path="/automl"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/ml"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/automl"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/ml"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 10: Visualizations */}
                <Route
                  path="/visualizations"
                  element={
                    <ProtectedRoute>
                      <VisualizationsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/visualizations"
                  element={
                    <ProtectedRoute>
                      <VisualizationsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 11: Explainability/XAI */}
                <Route
                  path="/xai"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/xai"
                  element={
                    <ProtectedRoute>
                      <AutoMlPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 12: Findings */}
                <Route
                  path="/findings"
                  element={
                    <ProtectedRoute>
                      <FindingsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/findings"
                  element={
                    <ProtectedRoute>
                      <FindingsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Stage 13: Report */}
                <Route
                  path="/reports"
                  element={
                    <ProtectedRoute>
                      <ReportsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/report"
                  element={
                    <ProtectedRoute>
                      <ReportsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/reports"
                  element={
                    <ProtectedRoute>
                      <ReportsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Additional Autonomous Tools */}
                <Route
                  path="/drift"
                  element={
                    <ProtectedRoute>
                      <DriftPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/ask"
                  element={
                    <ProtectedRoute>
                      <AskDatasetPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/datasets/:id/ask"
                  element={
                    <ProtectedRoute>
                      <AskDatasetPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/experiments"
                  element={
                    <ProtectedRoute>
                      <ExperimentsPage />
                    </ProtectedRoute>
                  }
                />

                {/* Catch-all redirect */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Routes>
            </main>
            <footer className="bg-white border-top py-3 text-center text-muted small">
              AutoScientist AI &copy; 2026 &bull; "From Raw Data to Evidence-Based Scientific Discovery."
            </footer>
          </div>
        </WorkflowProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
