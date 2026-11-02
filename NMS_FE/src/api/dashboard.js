import api from '../services/api';

/**
 * Lấy số liệu tổng quan Dashboard (Total News, Categories, Accounts)
 */
export async function getDashboardOverview(options = {}) {
  const { data } = await api.get('/dashboard/overview', options);
  return data;
}

/**
 * Lấy số liệu tương tác (Total Views, Total Likes, Top 5 Trending)
 */
export async function getDashboardEngagement(options = {}) {
  const { data } = await api.get('/dashboard/engagement', options);
  return data; // { totalViews, totalLikes, top5Trending: [...] }
}
