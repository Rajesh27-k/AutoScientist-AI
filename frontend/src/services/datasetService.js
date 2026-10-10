import api from './api';

export const datasetService = {
  async uploadDataset(file, name, description) {
    const formData = new FormData();
    formData.append('file', file);
    if (name) formData.append('name', name);
    if (description) formData.append('description', description);

    const res = await api.post('/datasets/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  async listDatasets() {
    const res = await api.get('/datasets');
    return res.data;
  },

  async getDatasets() {
    return this.listDatasets();
  },

  async getDataset(id) {
    const res = await api.get(`/datasets/${id}`);
    return res.data;
  },

  async deleteDataset(id) {
    const res = await api.delete(`/datasets/${id}`);
    return res.data;
  },

  async getWorkflowStatus(id) {
    const res = await api.get(`/datasets/${id}/workflow-status`);
    return res.data;
  },
};
