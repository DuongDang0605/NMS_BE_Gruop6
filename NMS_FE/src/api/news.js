import api from '../services/api';

/**
 * Lấy danh sách bài viết tin tức với các bộ lọc tuỳ chọn
 * @param {Object} [params]
 * @param {string} [params.keyword]
 * @param {number|string} [params.categoryId]
 * @param {number|string} [params.newsStatus]
 * @param {number|string} [params.tagId]
 * @param {Object} [options]
 */
export async function getNewsArticles(params = {}, options = {}) {
  const cleanParams = {};
  if (params.keyword && params.keyword.trim()) cleanParams.keyword = params.keyword.trim();
  if (params.categoryId !== undefined && params.categoryId !== '' && params.categoryId !== null) {
    cleanParams.categoryId = Number(params.categoryId);
  }
  if (params.newsStatus !== undefined && params.newsStatus !== '' && params.newsStatus !== null) {
    cleanParams.newsStatus = Number(params.newsStatus);
  }
  if (params.tagId !== undefined && params.tagId !== '' && params.tagId !== null) {
    cleanParams.tagId = Number(params.tagId);
  }

  const { data } = await api.get('/news', { ...options, params: cleanParams });
  return data;
}

/**
 * Lấy chi tiết bài viết theo ID
 * @param {number|string} id
 * @param {Object} [options]
 */
export async function getNewsArticleById(id, options = {}) {
  const { data } = await api.get(`/news/${id}`, options);
  return data;
}

/**
 * Tạo mới một bài viết tin tức
 * @param {Object} payload { newsTitle, headline, newsContent, newsSource, categoryId, newsStatus, createdById, tagIds }
 */
export async function createNewsArticle(payload) {
  const { data } = await api.post('/news', payload);
  return data;
}

/**
 * Cập nhật bài viết tin tức
 * @param {number|string} id
 * @param {Object} payload { newsTitle, headline, newsContent, newsSource, categoryId, newsStatus, updatedById, tagIds }
 */
export async function updateNewsArticle(id, payload) {
  const { data } = await api.put(`/news/${id}`, payload);
  return data;
}

/**
 * Thay đổi nhanh trạng thái bài viết (0: Inactive / Lưu nháp, 1: Active / Đã xuất bản)
 * @param {number|string} id
 * @param {number} status
 */
export async function updateNewsArticleStatus(id, status) {
  const { data } = await api.patch(`/news/${id}/status`, null, {
    params: { status: Number(status) },
  });
  return data;
}

/**
 * Xoá một bài viết tin tức theo ID
 * @param {number|string} id
 */
export async function deleteNewsArticle(id) {
  await api.delete(`/news/${id}`);
}
