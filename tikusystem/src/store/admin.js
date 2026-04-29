import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useAdminStore = defineStore('admin', () => {
  const reviews = ref([])
  const pendingCount = ref(0)
  const users = ref([])

  async function fetchReviews(params) {
    const data = await request.get('/admin/reviews', { params })
    reviews.value = data.list
    pendingCount.value = data.pendingCount
    return data
  }

  async function approveReview(id) {
    await request.post(`/admin/reviews/${id}/approve`)
  }

  async function rejectReview(id, reason) {
    await request.post(`/admin/reviews/${id}/reject`, { reason })
  }

  async function fetchUsers(params) {
    const data = await request.get('/admin/users', { params })
    users.value = data.list
    return data
  }

  async function updateUserStatus(id, status) {
    await request.put(`/admin/users/${id}/status`, { status })
  }

  async function updateUserRole(id, role) {
    await request.put(`/admin/users/${id}/role`, { role })
  }

  return {
    reviews,
    pendingCount,
    users,
    fetchReviews,
    approveReview,
    rejectReview,
    fetchUsers,
    updateUserStatus,
    updateUserRole
  }
})