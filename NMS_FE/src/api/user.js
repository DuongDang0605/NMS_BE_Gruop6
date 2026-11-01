import api from '../services/api';

export async function getMyProfile(options = {}) {
  const { data } = await api.get('/users/me', options);
  return data;
}

export async function changeMyPassword(passwords) {
  await api.put('/users/me/password', passwords);
}

export async function getUsers(params, options = {}) {
  const { data } = await api.get('/users', { ...options, params });
  return data;
}

export async function createUser(payload) {
  const { data } = await api.post('/users', payload);
  return data;
}

export async function updateUser(id, payload) {
  const { data } = await api.put(`/users/${id}`, payload);
  return data;
}

export async function deleteUser(id) {
  await api.delete(`/users/${id}`);
}

export async function toggleUserVerification(id) {
  const { data } = await api.patch(`/users/${id}/verify`);
  return data;
}
