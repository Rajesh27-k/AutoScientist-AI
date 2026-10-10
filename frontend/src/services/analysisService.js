import api from './api';

export const analysisService = {
  // EDA & Associations
  async getAssociations(datasetId) {
    const res = await api.get(`/datasets/${datasetId}/associations`);
    return res.data;
  },

  async refreshAssociations(datasetId) {
    const res = await api.post(`/datasets/${datasetId}/associations/refresh`);
    return res.data;
  },

  async getCorrelationMatrix(datasetId) {
    const res = await api.get(`/datasets/${datasetId}/eda/correlations`);
    return res.data;
  },

  async getBoxPlots(datasetId) {
    const res = await api.get(`/datasets/${datasetId}/eda/boxplots`);
    return res.data;
  },

  async getGroupedSummary(datasetId, numericVar, categoricalVar) {
    const res = await api.get(`/datasets/${datasetId}/eda/grouped`, {
      params: { numericVar, categoricalVar },
    });
    return res.data;
  },

  // AutoML, XAI, Fairness & What-If
  async trainAutoMl(datasetId, data) {
    const res = await api.post(`/datasets/${datasetId}/automl/train`, data);
    return res.data;
  },

  async simulateWhatIf(datasetId, data) {
    const res = await api.post(`/datasets/${datasetId}/automl/whatif`, data);
    return res.data;
  },

  async getLatestAutoMl(datasetId) {
    const res = await api.get(`/datasets/${datasetId}/automl/latest`);
    return res.data;
  },

  // Data Drift & Comparison
  async compareDrift(referenceId, targetId) {
    const res = await api.get('/drift/compare', {
      params: { referenceId, targetId },
    });
    return res.data;
  },

  // Ask Dataset Natural Language Query
  async askDataset(datasetId, query) {
    const res = await api.post(`/datasets/${datasetId}/ask`, { query });
    return res.data;
  },

  // Scientific Reports
  async generateReport(datasetId, data = {}) {
    const res = await api.post(`/datasets/${datasetId}/reports`, data);
    return res.data;
  },

  async listReports() {
    const res = await api.get('/reports');
    return res.data;
  },

  async getDatasetReports(datasetId) {
    const res = await api.get(`/datasets/${datasetId}/reports`);
    return res.data;
  },

  async getReport(reportId) {
    const res = await api.get(`/reports/${reportId}`);
    return res.data;
  },

  async downloadReportPdf(reportId) {
    const res = await api.get(`/reports/${reportId}/pdf`, {
      responseType: 'blob',
    });
    return res.data;
  },

  // Live Dashboard Analytics
  async getDashboardStats() {
    const res = await api.get('/dashboard/stats');
    return res.data;
  },
};
