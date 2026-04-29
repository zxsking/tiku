import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getWrongQuestions,
  getWrongQuestionDetail,
  removeWrongQuestion,
  getWrongQuestionStats,
  toggleMastered,
  toggleStarred,
  setErrorReason,
  getTodayReview,
  submitReview,
  getHeatmap
} from '@/api/wrongBook'

export const useWrongBookStore = defineStore('wrongBook', () => {
  const list = ref([])
  const total = ref(0)
  const stats = ref(null)
  const loading = ref(false)

  // 今日复习
  const todayReview = ref([])
  const reviewLoading = ref(false)

  // ---- 列表 ----
  async function fetchList(params) {
    loading.value = true
    try {
      const data = await getWrongQuestions(params)
      list.value = data.records || data.list || []
      total.value = data.total || 0
      return data
    } finally {
      loading.value = false
    }
  }

  // ---- 统计 ----
  async function fetchStats() {
    const data = await getWrongQuestionStats()
    stats.value = data
    return data
  }

  // ---- 移除 ----
  async function remove(questionId) {
    await removeWrongQuestion(questionId)
    list.value = list.value.filter(item => item.questionId !== questionId)
    total.value = Math.max(0, total.value - 1)
  }

  // ---- 掌握 ----
  async function toggleMasteredById(id) {
    await toggleMastered(id)
    const item = list.value.find(i => i.id === id)
    if (item) item.mastered = !item.mastered
  }

  // ---- 收藏 ----
  async function toggleStarredById(id) {
    await toggleStarred(id)
    const item = list.value.find(i => i.id === id)
    if (item) item.starred = !item.starred
  }

  // ---- 错因 ----
  async function setReason(id, reason) {
    await setErrorReason(id, reason)
    const item = list.value.find(i => i.id === id)
    if (item) item.errorReason = reason
  }

  // ---- 今日复习 ----
  async function fetchTodayReview() {
    reviewLoading.value = true
    try {
      const data = await getTodayReview()
      todayReview.value = Array.isArray(data) ? data : []
      return todayReview.value
    } finally {
      reviewLoading.value = false
    }
  }

  async function doSubmitReview(id, result) {
    await submitReview(id, result)
    todayReview.value = todayReview.value.filter(item => item.id !== id)
  }

  // ---- 热力图 ----
  async function fetchHeatmap(days = 90) {
    return await getHeatmap(days)
  }

  // ---- 详情 ----
  async function fetchDetail(id) {
    return await getWrongQuestionDetail(id)
  }

  return {
    list,
    total,
    stats,
    loading,
    todayReview,
    reviewLoading,
    fetchList,
    fetchStats,
    remove,
    toggleMasteredById,
    toggleStarredById,
    setReason,
    fetchTodayReview,
    doSubmitReview,
    fetchHeatmap,
    fetchDetail
  }
})
