import api from '../services/api';

/**
 * Lấy cây bình luận đa cấp của một bài viết (Public, không cần đăng nhập)
 * @param {number|string} articleId
 * @param {Object} options Axios request config (signal, ...)
 * @returns {Promise<Array>} [{ commentId, accountId, authorName, parentCommentId, content,
 *                              status, createdDate, replies: [...] }]
 */
export async function getArticleComments(articleId, options = {}) {
    const { data } = await api.get(`/comments/article/${articleId}`, options);
    return data;
}

/**
 * Đăng bình luận mới hoặc trả lời một bình luận (Yêu cầu đăng nhập)
 * @param {{ newsArticleId: number|string, parentCommentId?: number|null, content: string }} payload
 * @returns {Promise<Object>} { commentId, status, ... }
 *   status = 'PENDING_MODERATION' khi tài khoản chưa xác minh, dính từ FLAG hoặc bật pre-moderation;
 *   status = 'APPROVED' khi hiển thị ngay.
 *   Lỗi 400 (từ cấm BLOCK, bài viết tắt bình luận...) có thông báo ở err.response.data.message
 */
export async function postComment({ newsArticleId, parentCommentId = null, content }) {
    const { data } = await api.post('/comments', {
        newsArticleId: Number(newsArticleId),
        parentCommentId,
        content,
    });
    return data;
}

/**
 * Báo cáo bình luận vi phạm (Yêu cầu đăng nhập)
 * @param {number} commentId
 * @param {{ reason: 'SPAM'|'HARASSMENT'|'OFFENSIVE'|'MISINFORMATION'|'OTHER', note?: string }} report
 */
export async function reportComment(commentId, { reason, note = '' }) {
    await api.post(`/comments/${commentId}/report`, { reason, note: note || null });
}