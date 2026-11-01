import api from '../services/api';

export async function login(credentials) {
  const { data } = await api.post('/auth/login', credentials);
  return data;
}

export async function registerReader(account) {
  const { data } = await api.post('/auth/register-reader', account);
  return data;
}

export async function getCurrentUser(options = {}) {
  const { data } = await api.get('/auth/me', options);
  return data;
}

export async function refreshTokens(refreshToken) {
  const { data } = await api.post('/auth/refresh', { refreshToken });
  return data;
}

export async function requestUnlock(accountEmail) {
  const { data } = await api.post('/auth/unlock/request', { accountEmail });
  return data;
}

export async function verifyUnlock(accountEmail, otp) {
  const { data } = await api.post('/auth/unlock/verify', { accountEmail, otp });
  return data;
}

export async function resetUnlockPassword(accountEmail, resetToken, newPassword) {
  const { data } = await api.post('/auth/unlock/reset-password', { accountEmail, resetToken, newPassword });
  return data;
}
