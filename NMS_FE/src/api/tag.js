import api from '../services/api';

/**
 * Lấy danh sách thẻ (Tags), có thể tìm theo từ khóa
 * @param {string} [keyword]
 * @param {Object} [options]
 */
export async function getTags(keyword, options = {}) {
  const params = keyword && keyword.trim() ? { keyword: keyword.trim() } : {};
  const { data } = await api.get('/tags', { ...options, params });
  return data;
}

/**
 * Lấy chi tiết một tag theo ID
 * @param {number|string} id
 * @param {Object} [options]
 */
export async function getTagById(id, options = {}) {
  const { data } = await api.get(`/tags/${id}`, options);
  return data;
}

/**
 * Tạo mới một tag
 * @param {Object} payload { tagName, note }
 */
export async function createTag(payload) {
  const { data } = await api.post('/tags', payload);
  return data;
}

/**
 * Cập nhật một tag
 * @param {number|string} id
 * @param {Object} payload { tagName, note }
 */
export async function updateTag(id, payload) {
  const { data } = await api.put(`/tags/${id}`, payload);
  return data;
}

/**
 * Xoá một tag
 * @param {number|string} id
 */
export async function deleteTag(id) {
  await api.delete(`/tags/${id}`);
}
