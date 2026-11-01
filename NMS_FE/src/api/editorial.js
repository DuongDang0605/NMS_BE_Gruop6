import api from '../services/api';

export const authorApi = {
  getMyArticles: async (params = {}) => {
    const { data } = await api.get('/author/articles', { params });
    return data;
  },

  getArticleById: async (id) => {
    const { data } = await api.get(`/author/articles/${id}`);
    return data;
  },

  createDraft: async (payload) => {
    const { data } = await api.post('/author/articles', payload);
    return data;
  },

  updateDraft: async (id, payload) => {
    const { data } = await api.put(`/author/articles/${id}`, payload);
    return data;
  },

  submitForReview: async (id) => {
    const { data } = await api.post(`/author/articles/${id}/submit`);
    return data;
  },

  getFeedbackNotes: async (id) => {
    const { data } = await api.get(`/author/articles/${id}/feedback`);
    return data;
  },

  deleteDraft: async (id) => {
    const { data } = await api.delete(`/author/articles/${id}`);
    return data;
  },
};

export const editorialApi = {
  getReviewQueue: async (params = {}) => {
    const { data } = await api.get('/editorial/queue', { params });
    return data;
  },

  getArticleDetail: async (id) => {
    const { data } = await api.get(`/editorial/articles/${id}`);
    return data;
  },

  requestChanges: async (id, feedbackContent) => {
    const { data } = await api.post(`/editorial/articles/${id}/request-changes`, { feedbackContent });
    return data;
  },

  publishNow: async (id) => {
    const { data } = await api.post(`/editorial/articles/${id}/publish-now`);
    return data;
  },

  schedulePublication: async (id, scheduledPublishDate) => {
    const { data } = await api.post(`/editorial/articles/${id}/schedule`, { scheduledPublishDate });
    return data;
  },

  archiveArticle: async (id) => {
    const { data } = await api.post(`/editorial/articles/${id}/archive`);
    return data;
  },

  restoreArticle: async (id) => {
    const { data } = await api.post(`/editorial/articles/${id}/restore`);
    return data;
  },

  getAuditTrail: async (id) => {
    const { data } = await api.get(`/editorial/articles/${id}/audit-trail`);
    return data;
  },
};

