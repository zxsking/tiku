import request from '@/utils/request'

export function login(data) {
  console.log('Login request data:', data)
  return request.post('/auth/login', data)
}

export function register(data) {
  console.log('Register request data:', data)
  return request.post('/auth/register', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

export function changePassword(data) {
  return request.put('/user/password', data)
}