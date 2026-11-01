import api from '../services/api';

/**
 * Tăng 1 lượt xem khi đọc bài báo (Public, không yêu cầu đăng nhập)
 * @param {number|string} articleId
 */
export async function recordView(articleId) {
  const { data } = await api.post(`/news/${articleId}/views`);
  return data; // { articleId, viewCount }
}

/**
 * Thả tim hoặc bỏ tim (Yêu cầu đăng nhập - JWT)
 * @param {number|string} articleId
 */
export async function toggleLike(articleId) {
  const { data } = await api.post(`/news/${articleId}/reactions`);
  return data; // { articleId, isLiked, likeCount }
}
