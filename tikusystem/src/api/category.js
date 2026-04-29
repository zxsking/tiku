import request from '@/utils/request'

export function getCategories() {
  return request.get('/categories')
}

export function getCategory(id) {
  return request.get(`/categories/${id}`)
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}

// Admin-specific functions for category management
export function getAllCategories(params) {
  return request.get('/admin/categories', { params })
}

export function updateCategoryStatus(id, status) {
  return request.put(`/admin/categories/${id}/status`, { status })
}