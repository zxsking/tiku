import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { createSession, getSession, submitSession } from '@/api/practice'

const STORAGE_KEY = 'practice_session_draft'

export const usePracticeStore = defineStore('practice', () => {
  const sessionId = ref(null)
  const questions = ref([])
  const userAnswers = ref({})
  const markedQuestions = ref(new Set())
  const timeLeft = ref(0)
  const timeLimit = ref(0)
  const isSubmitted = ref(false)
  const result = ref(null)
  const loading = ref(false)

  const answeredCount = computed(() =>
    Object.keys(userAnswers.value).filter(k => {
      const v = userAnswers.value[k]
      return v !== null && v !== undefined && v !== '' && !(Array.isArray(v) && v.length === 0)
    }).length
  )

  const totalCount = computed(() => questions.value.length)

  function saveDraft() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      sessionId: sessionId.value,
      questions: questions.value,
      userAnswers: userAnswers.value,
      markedQuestions: [...markedQuestions.value],
      timeLeft: timeLeft.value,
      timeLimit: timeLimit.value
    }))
  }

  function loadDraft() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return false
      const draft = JSON.parse(raw)
      sessionId.value = draft.sessionId
      questions.value = draft.questions || []
      userAnswers.value = draft.userAnswers || {}
      markedQuestions.value = new Set(draft.markedQuestions || [])
      timeLeft.value = draft.timeLeft || 0
      timeLimit.value = draft.timeLimit || 0
      isSubmitted.value = false
      return true
    } catch {
      return false
    }
  }

  function clearDraft() {
    localStorage.removeItem(STORAGE_KEY)
  }

  async function startSession(data) {
    loading.value = true
    try {
      const res = await createSession(data)
      sessionId.value = res.id
      questions.value = res.questions || []
      userAnswers.value = {}
      markedQuestions.value = new Set()
      timeLimit.value = res.timeLimit || 0
      timeLeft.value = res.timeLimit || 0
      isSubmitted.value = false
      result.value = null
      saveDraft()
      return res
    } finally {
      loading.value = false
    }
  }

  async function loadSession(id) {
    loading.value = true
    try {
      const res = await getSession(id)
      sessionId.value = res.id
      questions.value = res.questions || []
      userAnswers.value = res.userAnswers || {}
      timeLimit.value = res.timeLimit || 0
      timeLeft.value = res.timeLeft || 0
      isSubmitted.value = res.status === 1
      result.value = res.result || null
      return res
    } finally {
      loading.value = false
    }
  }

  function saveAnswer(questionId, answer) {
    userAnswers.value[questionId] = answer
    saveDraft()
  }

  function toggleMark(questionId) {
    if (markedQuestions.value.has(questionId)) {
      markedQuestions.value.delete(questionId)
    } else {
      markedQuestions.value.add(questionId)
    }
  }

  async function submit(timeUsed) {
    loading.value = true
    try {
      const res = await submitSession(sessionId.value, {
        userAnswers: userAnswers.value,
        timeUsed
      })
      isSubmitted.value = true
      result.value = res
      clearDraft()
      return res
    } finally {
      loading.value = false
    }
  }

  function reset() {
    sessionId.value = null
    questions.value = []
    userAnswers.value = {}
    markedQuestions.value = new Set()
    timeLeft.value = 0
    timeLimit.value = 0
    isSubmitted.value = false
    result.value = null
    clearDraft()
  }

  return {
    sessionId,
    questions,
    userAnswers,
    markedQuestions,
    timeLeft,
    timeLimit,
    isSubmitted,
    result,
    loading,
    answeredCount,
    totalCount,
    startSession,
    loadSession,
    saveAnswer,
    toggleMark,
    submit,
    loadDraft,
    saveDraft,
    clearDraft,
    reset
  }
})
