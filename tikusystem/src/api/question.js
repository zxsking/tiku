import request from '@/utils/request'

export function getQuestions(params) {
  return request.get('/questions', { params })
}

export function getQuestion(id) {
  return request.get(`/questions/${id}`)
}

export function createQuestion(data) {
  return request.post('/questions', data)
}

export function updateQuestion(id, data) {
  return request.put(`/questions/${id}`, data)
}

export function deleteQuestion(id) {
  return request.delete(`/questions/${id}`)
}

export function getMyQuestions(params) {
  return request.get('/user/questions', { params })
}

export function toggleQuestionLike(id) {
  return request.post(`/questions/${id}/like`)
}

export function toggleQuestionFavorite(id) {
  return request.post(`/questions/${id}/favorite`)
}

export function getQuestionComments(id, params) {
  return request.get(`/questions/${id}/comments`, { params })
}

export function addComment(questionId, data) {
  return request.post(`/questions/${questionId}/comments`, data)
}

// Additional question functions
export function deleteComment(questionId, commentId) {
  return request.delete(`/questions/${questionId}/comments/${commentId}`)
}

export function updateComment(questionId, commentId, data) {
  return request.put(`/questions/${questionId}/comments/${commentId}`, data)
}

export function getQuestionLikes(id) {
  return request.get(`/questions/${id}/likes`)
}

export function getQuestionFavorites(id) {
  return request.get(`/questions/${id}/favorites`)
}

export function getRandomQuestion(params) {
  return request.get('/questions/random', { params })
}

export function getQuestionStats(id) {
  return request.get(`/questions/${id}/stats`)
}

// Admin functions for question management
export function getPendingQuestions(params) {
  return request.get('/admin/questions/pending', { params })
}

export function approveQuestion(id, data) {
  return request.post(`/admin/questions/${id}/approve`, data)
}

export function rejectQuestion(id, data) {
  return request.post(`/admin/questions/${id}/reject`, data)
}

export function getQuestionReport(id) {
  return request.get(`/admin/questions/${id}/report`)
}

export function batchUpdateQuestions(data) {
  return request.put('/admin/questions/batch', data)
}

export function batchDeleteQuestions(ids) {
  return request.post('/admin/questions/batch-delete', { ids })
}