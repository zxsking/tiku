import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useSearchStore = defineStore('search', () => {
  const results = ref({ banks: [], questions: [] })
  const bankResults = ref([])
  const questionResults = ref([])
  const total = ref(0)

  async function search(params) {
    const data = await request.get('/search', { params })
    results.value = data
    return data
  }

  async function searchBanks(params) {
    const data = await request.get('/search/banks', { params })
    bankResults.value = data.records || data.list || []
    total.value = data.total || 0
    return data
  }

  async function searchQuestions(params) {
    const data = await request.get('/search/questions', { params })
    questionResults.value = data.records || data.list || []
    total.value = data.total || 0
    return data
  }

  return {
    results,
    bankResults,
    questionResults,
    total,
    search,
    searchBanks,
    searchQuestions
  }
})