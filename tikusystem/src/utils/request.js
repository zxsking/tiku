import axios from 'axios'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import router from '@/router'
import { useUserStore } from '@/store/user'


//创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})


//请求拦截器(发送前处理)
request.interceptors.request.use(
  config => {
    NProgress.start()   //顶部进度条
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    NProgress.done()
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    NProgress.done()
    const res = response.data
    
    if (res === null || res === undefined || res.code === undefined) {
      return res
    }
    
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    NProgress.done()
    console.error('Request error:', error.response?.data, error.response?.status)
    
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push({ name: 'Login' })
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 403) {
      ElMessage.error(error.response?.data?.message || '没有权限访问该资源')
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.status) {
      ElMessage.error(`请求失败：${error.response.status}`)
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request