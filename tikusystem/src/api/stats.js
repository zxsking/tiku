import request from '@/utils/request'

// 获取系统统计数据
export function getSystemStats() {
  return request.get('/stats/system')
}

// 获取热门题库列表
export function getHotBanks(params) {
  return request.get('/stats/hot-banks', { params })
}

// 获取最新题目列表
export function getLatestQuestions(params) {
  return request.get('/stats/latest-questions', { params })
}

// 获取用户统计数据
export function getUserStats() {
  return request.get('/stats/user')
}

// 获取题库统计数据
export function getBankStats(bankId) {
  return request.get(`/stats/banks/${bankId}`)
}

// 获取题目统计数据
export function getQuestionStats(questionId) {
  return request.get(`/stats/questions/${questionId}`)
}

// 获取分类统计数据
export function getCategoryStats() {
  return request.get('/stats/categories')
}