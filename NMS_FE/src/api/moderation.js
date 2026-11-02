import api from '../services/api';

// Toàn bộ API ở đây yêu cầu role MODERATOR (4) hoặc ADMIN (1).

/**
 * Hàng đợi kiểm duyệt: bình luận chờ duyệt hoặc đang bị báo cáo (bị báo cáo nhiều xếp trước)
 * @param {{ page?: number, size?: number }} params
 * @returns {Promise<Object>} { content: [...], totalElements, totalPages, page, size, first, last }
 */
export async function getPendingComments(params = {}, options = {}) {
    const { data } = await api.get('/moderation/comments/pending', { ...options, params });
    return data;
}

/**
 * Danh sách phiếu báo cáo theo trạng thái
 * @param {{ status?: 'PENDING'|'RESOLVED'|'DISMISSED', page?: number, size?: number }} params
 * @returns {Promise<Object>} { content: [...], totalElements, totalPages, page, size, first, last }
 */
export async function getReports(params = {}, options = {}) {
    const { data } = await api.get('/moderation/reports', { ...options, params });
    return data;
}

export async function approveComment(commentId) {
    await api.patch(`/moderation/comments/${commentId}/approve`);
}

export async function rejectComment(commentId) {
    await api.patch(`/moderation/comments/${commentId}/reject`);
}

/** "Giữ lại": bác bỏ các báo cáo, giữ nguyên bình luận */
export async function keepComment(commentId) {
    await api.patch(`/moderation/comments/${commentId}/keep`);
}

/** Xóa bình luận vi phạm (xóa mềm, status = DELETED) */
export async function deleteComment(commentId) {
    await api.delete(`/moderation/comments/${commentId}`);
}

// ---------------------------------------------------------------- Từ điển từ cấm

export async function getKeywords(options = {}) {
    const { data } = await api.get('/moderation/keywords', options);
    return data; // [{ keywordId, word, severity, isActive, createdDate }]
}

/** @param {{ word: string, severity: 'BLOCK'|'FLAG', isActive?: boolean }} keyword */
export async function createKeyword(keyword) {
    const { data } = await api.post('/moderation/keywords', keyword);
    return data;
}

export async function updateKeyword(keywordId, keyword) {
    const { data } = await api.put(`/moderation/keywords/${keywordId}`, keyword);
    return data;
}

export async function deleteKeyword(keywordId) {
    await api.delete(`/moderation/keywords/${keywordId}`);
}

// ---------------------------------------------------------------- Chế độ kiểm duyệt toàn trang

export async function getModerationSettings(options = {}) {
    const { data } = await api.get('/moderation/settings', options);
    return data; // { preModeration }
}

/** Chỉ ADMIN được gọi */
export async function updateModerationSettings(preModeration) {
    const { data } = await api.put('/moderation/settings', { preModeration });
    return data;
}