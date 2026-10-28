import api from '../services/api';

/**
 * Lấy danh sách bài báo đã xuất bản (Status = 1)
 * @param {Object} params { keyword, categoryId, tagId, page, size }
 */
export async function getPublishedArticles(params = {}) {
  const { data } = await api.get('/portal/articles', { params });
  return data; // { content: [...], totalElements, totalPages, number, size }
}

/**
 * Lấy chi tiết bài báo kèm lượt view, lượt like và trạng thái isLikedByCurrentUser
 * @param {number|string} id
 * @param {Object} options Axios request config (signal, etc.)
 */
export async function getArticleDetail(id, options = {}) {
  const { data } = await api.get(`/portal/articles/${id}`, options);
  return data;
}
