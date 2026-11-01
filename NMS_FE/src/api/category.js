import api from '../services/api';

/**
 * Lấy cây danh mục phân cấp (root -> children)
 */
export async function getCategoryTree() {
  const { data } = await api.get('/categories/tree');
  return data;
}

/**
 * Lấy danh sách phẳng tất cả danh mục (dùng cho dropdown chọn parent hoặc bảng)
 */
export async function getCategories() {
  const { data } = await api.get('/categories');
  return data;
}

export async function createCategory(payload) {
  const { data } = await api.post('/categories', payload);
  return data;
}

export async function updateCategory(id, payload) {
  const { data } = await api.put(`/categories/${id}`, payload);
  return data;
}

export async function deleteCategory(id) {
  await api.delete(`/categories/${id}`);
}
