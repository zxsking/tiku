import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useCategoryStore = defineStore('category', () => {
  const categories = ref([])
  const categoryStats = ref([])

  async function fetchCategories() {
    const data = await request.get('/categories')
    categories.value = data || []
    return data
  }

  async function createCategory(data) {
    const res = await request.post('/categories', data)
    return res
  }

  async function updateCategory(id, data) {
    const res = await request.put(`/categories/${id}`, data)
    return res
  }

  async function deleteCategory(id) {
    await request.delete(`/categories/${id}`)
  }

  async function fetchCategoryStats() {
    const data = await request.get('/stats/categories')
    categoryStats.value = data || []
    return data
  }

  return {
    categories,
    categoryStats,
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
    fetchCategoryStats
  }
})