import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useInteractionStore = defineStore('interaction', () => {
  const followingUsers = ref([])
  const favoriteQuestions = ref([])
  const favoriteTotal = ref(0)

  async function toggleFollow(userId) {
    const data = await request.post(`/users/${userId}/follow`)
    return data
  }

  async function fetchFollowing() {
    const data = await request.get('/user/following')
    followingUsers.value = Array.isArray(data) ? data : (data?.list ?? [])
    return data
  }

  async function fetchFavoriteQuestions(params) {
    const data = await request.get('/user/favorites/questions', { params })
    favoriteQuestions.value = data.list ?? []
    favoriteTotal.value = data.total ?? data.list?.length ?? 0
    return data
  }

  return {
    followingUsers,
    favoriteQuestions,
    favoriteTotal,
    toggleFollow,
    fetchFollowing,
    fetchFavoriteQuestions
  }
})