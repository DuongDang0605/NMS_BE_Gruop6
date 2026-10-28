import api from '../services/api';

/**
 * Lấy danh sách thông báo của tài khoản
 * @param {number|string} recipientId
 * @param {string} status 'all' | 'unread' | 'read'
 * @param {number} page
 * @param {number} size
 */
export async function getNotifications(recipientId, status = 'all', page = 0, size = 10) {
  const { data } = await api.get('/notifications', {
    params: { recipientId, status, page, size },
  });
  return data; // Page<NotificationResponseDto>
}

/**
 * Lấy số lượng thông báo chưa đọc
 * @param {number|string} recipientId
 */
export async function getUnreadCount(recipientId) {
  const { data } = await api.get('/notifications/unread-count', {
    params: { recipientId },
  });
  return data?.unreadCount || 0;
}

/**
 * Đánh dấu 1 thông báo là đã đọc
 * @param {number|string} id
 * @param {number|string} recipientId
 */
export async function markAsRead(id, recipientId) {
  const { data } = await api.patch(`/notifications/${id}/read`, null, {
    params: { recipientId },
  });
  return data;
}

/**
 * Đánh dấu tất cả thông báo là đã đọc
 * @param {number|string} recipientId
 */
export async function markAllAsRead(recipientId) {
  const { data } = await api.patch('/notifications/read-all', null, {
    params: { recipientId },
  });
  return data;
}
