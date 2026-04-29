import request from '@/utils/request'

export function search(params) {
  return request.get('/search', { params })
}

export function searchBanks(params) {
  return request.get('/search/banks', { params })
}

export function searchQuestions(params) {
  return request.get('/search/questions', { params })
}