import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import Cookies from 'js-cookie'

export const useUserStore = defineStore('user', () => {
  const token = ref(Cookies.get('token') || '')
  const userInfo = ref(null)
  
  // 从 localStorage 恢复用户信息
  try {
    const savedUserInfo = localStorage.getItem('userInfo')
    if (savedUserInfo) {
      userInfo.value = JSON.parse(savedUserInfo)
    }
  } catch (e) {
    console.error('Failed to parse userInfo from localStorage:', e)
    localStorage.removeItem('userInfo')
  }

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  async function login(credentials) {
    const loginToken = await request.post('/auth/login', credentials)
    token.value = loginToken
    Cookies.set('token', loginToken, { expires: 7 })
    
    // 登录后获取用户信息
    await fetchUserInfo()
    return loginToken
  }

  async function register(userData) {
    const data = await request.post('/auth/register', userData)
    // Return the response data directly
    return data
  }

  async function fetchUserInfo() {
    const data = await request.get('/user/info')
    userInfo.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  async function updateInfo(data) {
    const res = await request.put('/user/info', data)
    userInfo.value = { ...userInfo.value, ...res }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  async function changePassword(data) {
    await request.put('/user/password', data)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    Cookies.remove('token')
    localStorage.removeItem('userInfo')
  }

  // 有 token 时静默拉取最新 userInfo；路由守卫通过 ready() 等待完成后再判断权限
  // 使用固定 Promise 而非 ref，避免路由守卫读取时机不确定导致的竞态
  const _readyPromise = token.value
    ? fetchUserInfo().catch(err => {
        console.error('Failed to fetch user info on init:', err)
        logout()
      })
    : Promise.resolve()

  function ready() {
    return _readyPromise
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    ready,
    login,
    register,
    fetchUserInfo,
    updateInfo,
    changePassword,
    logout
  }
})