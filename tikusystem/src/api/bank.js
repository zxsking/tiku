import request from '@/utils/request'

export function getBanks(params) {
  return request.get('/banks', { params })
}

export function getBank(id) {
  return request.get(`/banks/${id}`)
}

export function createBank(data) {
  return request.post('/banks', data)
}

export function updateBank(id, data) {
  return request.put(`/banks/${id}`, data)
}

export function deleteBank(id) {
  return request.delete(`/banks/${id}`)
}

export function getMyBanks(params) {
  return request.get('/user/banks', { params })
}

export function toggleBankFavorite(id) {
  return request.post(`/banks/${id}/favorite`)
}

export function getBankQuestions(id, params) {
  return request.get(`/banks/${id}/questions`, { params })
}

// Additional bank functions
export function getBankFavorites(params) {
  return request.get('/user/favorite-banks', { params })
}

export function getBankLikes(id) {
  return request.get(`/banks/${id}/likes`)
}

export function toggleBankLike(id) {
  return request.post(`/banks/${id}/like`)
}

export function getBankStats(id) {
  return request.get(`/banks/${id}/stats`)
}

// Admin functions for bank management
export function getPendingBanks(params) {
  return request.get('/admin/banks/pending', { params })
}

export function approveBank(id, data) {
  return request.post(`/admin/banks/${id}/approve`, data)
}

export function rejectBank(id, data) {
  return request.post(`/admin/banks/${id}/reject`, data)
}

export function getBankReport(id) {
  return request.get(`/admin/banks/${id}/report`)
}