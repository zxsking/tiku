import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useStatsStore = defineStore('stats', () => {
  const stats = ref({
    totalBanks: 0,
    totalQuestions: 0,
    totalUsers: 0,
    totalFavorites: 0
  })

  async function fetchStats() {
    const data = await request.get('/stats')
    stats.value = data
    return data
  }

  return {
    stats,
    fetchStats
  }
})