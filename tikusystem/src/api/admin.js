import request from '@/utils/request'

// User management
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, {
    params: { status }
  })
}

export function updateUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, null, {
    params: { role }
  })
}

// Review management
export function getReviewQuestions(params) {
  return request.get('/admin/questions', { params })
}

export function reviewQuestion(id, status) {
  return request.put(`/admin/questions/${id}/status`, null, {
    params: { status }
  })
}

export function getReviewBanks(params) {
  return request.get('/admin/banks', { params })
}

export function reviewBank(id, status) {
  return request.put(`/admin/banks/${id}/status`, null, {
    params: { status }
  })
}

// Compatibility aliases
export const getPendingQuestions = getReviewQuestions
export const getPendingBanks = getReviewBanks
export const approveQuestion = id => reviewQuestion(id, 'published')
export const rejectQuestion = id => reviewQuestion(id, 'rejected')
export const approveBank = id => reviewBank(id, 'published')
export const rejectBank = id => reviewBank(id, 'rejected')