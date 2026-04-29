import request from '@/utils/request'

export function createSession(data) {
  return request.post('/practice/sessions', data)
}

export function getSessions(params) {
  return request.get('/practice/sessions', { params })
}

export function getSession(id) {
  return request.get(`/practice/sessions/${id}`)
}

export function submitSession(id, data) {
  return request.post(`/practice/sessions/${id}/submit`, data)
}
