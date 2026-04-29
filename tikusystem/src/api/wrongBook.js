import request from '@/utils/request'

// ---- 列表（扩展：keyword / sort / mastered / starred）----
export function getWrongQuestions(params) {
  return request.get('/user/wrong-questions', { params })
}

// ---- 单条详情 ----
export function getWrongQuestionDetail(id) {
  return request.get(`/user/wrong-questions/${id}`)
}

// ---- 从错题本移除 ----
export function removeWrongQuestion(questionId) {
  return request.delete(`/user/wrong-questions/remove/${questionId}`)
}

// ---- 掌握 / 收藏 / 错因 ----
export function toggleMastered(id) {
  return request.patch(`/user/wrong-questions/${id}/mastered`)
}

export function toggleStarred(id) {
  return request.patch(`/user/wrong-questions/${id}/starred`)
}

export function setErrorReason(id, reason) {
  return request.patch(`/user/wrong-questions/${id}/reason`, null, { params: { reason } })
}

// ---- 今日复习 ----
export function getTodayReview() {
  return request.get('/user/wrong-questions/today-review')
}

export function submitReview(id, result) {
  return request.post(`/user/wrong-questions/${id}/review`, null, {
    params: result !== undefined ? { result } : {}
  })
}

// ---- 热力图 ----
export function getHeatmap(days = 90) {
  return request.get('/user/wrong-questions/heatmap', { params: { days } })
}

// ---- 统计 ----
export function getWrongQuestionStats() {
  return request.get('/user/wrong-questions/stats')
}
