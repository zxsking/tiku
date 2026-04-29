import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useQuestionStore = defineStore('question', () => {
  const questions = ref([])
  const currentQuestion = ref(null)
  const total = ref(0)
  const latestQuestions = ref([])

  async function fetchQuestions(params) {
    const data = await request.get('/questions', { params })
    questions.value = data.records || data.list || []
    total.value = data.total
    return data
  }

  async function fetchQuestion(id) {
    const data = await request.get(`/questions/${id}`)
    currentQuestion.value = data
    return data
  }

  async function createQuestion(questionData) {
    const data = await request.post('/questions', questionData)
    return data
  }

  async function updateQuestion(id, questionData) {
    const data = await request.put(`/questions/${id}`, questionData)
    return data
  }

  async function deleteQuestion(id) {
    await request.delete(`/questions/${id}`)
  }

  async function fetchMyQuestions(params) {
    const data = await request.get('/user/questions', { params })
    return data
  }

  // 修复：改用现有的 /questions?bankId=xxx 接口，不再请求不存在的 /banks/{id}/questions
  async function fetchBankQuestions(bankId, params = {}) {
    const data = await request.get('/questions', {
      params: {
        ...params,
        bankId,
        // 过滤掉空字符串参数，避免后端收到 type=&difficulty= 这类无效值
        ...(params.type ? { type: params.type } : {}),
        ...(params.difficulty ? { difficulty: params.difficulty } : {})
      }
    })
    return data
  }

  async function toggleLike(id) {
    const data = await request.post(`/questions/${id}/like`)
    return data
  }

  async function toggleFavorite(id) {
    const data = await request.post(`/questions/${id}/favorite`)
    return data
  }

  async function fetchComments(questionId, params) {
    const data = await request.get(`/questions/${questionId}/comments`, { params })
    return data
  }

  async function addComment(questionId, content, parentId = null) {
    const body = { content }
    if (parentId !== null && parentId !== undefined) body.parentId = parentId
    const data = await request.post(`/questions/${questionId}/comments`, body)
    return data
  }

  async function deleteComment(questionId, commentId) {
    const data = await request.delete(`/questions/${questionId}/comments/${commentId}`)
    return data
  }

  async function fetchLatestQuestions(limit = 5) {
    const data = await request.get('/stats/latest-questions', { params: { limit } })
    latestQuestions.value = data
    return data
  }

  return {
    questions,
    currentQuestion,
    total,
    latestQuestions,
    fetchQuestions,
    fetchQuestion,
    createQuestion,
    updateQuestion,
    deleteQuestion,
    fetchMyQuestions,
    fetchBankQuestions,
    toggleLike,
    toggleFavorite,
    fetchComments,
    addComment,
    deleteComment,
    fetchLatestQuestions
  }
})