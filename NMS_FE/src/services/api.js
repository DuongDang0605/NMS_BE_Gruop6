import axios from 'axios';
import { clearSession, getSession, saveSession } from './session';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
});

const refreshApi = axios.create({ baseURL: api.defaults.baseURL, timeout: 10000 });
let refreshPromise = null;
const isAuthRequest = (config) => ['/auth/login', '/auth/refresh'].includes(config?.url);

function endSession() {
  clearSession();
  window.location.replace('/login');
}

api.interceptors.request.use((config) => {
  const session = getSession();
  if (session && !isAuthRequest(config)) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    if (error.response?.status === 403) {
      window.alert(error.response.data?.message || 'Bạn không có quyền thực hiện chức năng này!');
      return Promise.reject(error);
    }
    if (error.response?.status !== 401 || !request || isAuthRequest(request)) {
      return Promise.reject(error);
    }
    const session = getSession();
    if (request._retry || !session?.refreshToken) {
      if (!session || request.headers.Authorization === `Bearer ${session.token}`) endSession();
      return Promise.reject(error);
    }
    request._retry = true;
    try {
      // Another request may already have refreshed the access token.
      if (request.headers.Authorization === `Bearer ${session.token}`) {
        if (!refreshPromise) {
          const refreshToken = session.refreshToken;
          refreshPromise = refreshApi.post('/auth/refresh', { refreshToken })
            .then(({ data }) => {
              if (getSession()?.refreshToken !== refreshToken) throw new Error('Session has changed');
              saveSession(data);
              return data.accessToken;
            })
            .finally(() => { refreshPromise = null; });
        }
        await refreshPromise;
      }
      const current = getSession();
      if (!current) return Promise.reject(error);
      request.headers.Authorization = `Bearer ${current.token}`;
      return api(request);
    } catch (refreshError) {
      if ([401, 403].includes(refreshError.response?.status)
        && getSession()?.refreshToken === session.refreshToken) endSession();
      return Promise.reject(refreshError);
    }
  },
);

export default api;
