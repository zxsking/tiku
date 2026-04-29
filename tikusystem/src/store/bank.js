import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useBankStore = defineStore('bank', () => {
  const banks = ref([])
  const currentBank = ref(null)
  const total = ref(0)
  const hotBanks = ref([])

  async function fetchBanks(params) {
    const data = await request.get('/banks', { params })
    banks.value = data.records || data.list || []
    total.value = data.total || 0
    return data
  }

  async function fetchBank(id) {
    try {
      const data = await request.get(`/banks/${id}`)
      currentBank.value = data
      return data
    } catch (e) {
      // 不可见或不存在的题库，统一清空当前题库并向上抛出，让页面自行处理提示
      currentBank.value = null
      throw e
    }
  }

  async function createBank(bankData) {
    const data = await request.post('/banks', bankData)
    return data
  }

  async function updateBank(id, bankData) {
    const data = await request.put(`/banks/${id}`, bankData)
    return data
  }

  async function deleteBank(id) {
    await request.delete(`/banks/${id}`)
  }

  async function fetchMyBanks(params) {
    const data = await request.get('/user/banks', { params })
    return data
  }

  async function toggleFavorite(id) {
    const data = await request.post(`/banks/${id}/favorite`)
    return data
  }

  async function fetchHotBanks(limit = 6) {
    const data = await request.get('/stats/hot-banks', { params: { limit } })
    hotBanks.value = data
    return data
  }

  // 查询收藏列表：type 为 'banks' 或 'questions'
  async function fetchFavorites({ type = 'banks', page = 1, size = 12 } = {}) {
    const data = await request.get(`/user/favorites/${type}`, { params: { page, size } })
    return data
  }

  // 取消收藏：type 为 'bank' 或 'question'
  async function removeFavorite(type, id) {
    await request.delete(`/user/favorites/${type}/${id}`)
  }

  return {
    banks,
    currentBank,
    total,
    hotBanks,
    fetchBanks,
    fetchBank,
    createBank,
    updateBank,
    deleteBank,
    fetchMyBanks,
    toggleFavorite,
    fetchHotBanks,
    fetchFavorites,
    removeFavorite
  }
})