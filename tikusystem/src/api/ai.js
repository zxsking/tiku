import request from '@/utils/request'
import { useUserStore } from '@/store/user'

/**
 * AI 批量识别题目（一次性返回，用于图片模式）
 * @param {Object} data - { text?, fileBase64?, fileType?, imageBase64List? }
 * @returns {Promise<Array>} QuestionDraft 列表
 */
export function parseQuestions(data) {
  return request.post('/ai/parse', data, { timeout: 120000 })
}

/**
 * AI 流式识别题目（SSE），每识别出一道题立即回调（用于文本/文件模式）
 * @param {Object} payload - { text?, fileBase64?, fileType? }
 * @param {Function} onQuestion - 每道题回调，参数为 QuestionDraft 对象
 * @param {Function} onDone    - 全部识别完成回调
 * @param {Function} onError   - 错误回调，参数为错误信息字符串
 * @returns {EventSource} 可调用 .close() 手动中止
 */
export function parseQuestionsStream(payload, onQuestion, onDone, onError) {
  const userStore = useUserStore()
  let es = null
  let cancelled = false

  // 先初始化拿 sessionKey，避免把大文本/文件 base64 放到 URL 导致 431
  request.post('/ai/parse/stream/init', payload, { timeout: 120000 })
    .then((res) => {
      if (cancelled) return
      const sessionKey = res?.sessionKey
      if (!sessionKey) throw new Error('初始化失败：缺少 sessionKey')

      const params = new URLSearchParams()
      params.set('sessionKey', sessionKey)
      if (userStore.token) params.set('token', userStore.token)

      es = new EventSource(`/api/ai/parse/stream?${params.toString()}`)

      es.addEventListener('question', (e) => {
        try {
          onQuestion(JSON.parse(e.data))
        } catch (err) {
          console.warn('Failed to parse SSE question event', err)
        }
      })

      // 可选：进度事件（不影响原有逻辑）
      es.addEventListener('progress', () => {})

      es.addEventListener('done', () => {
        es.close()
        onDone()
      })

      es.addEventListener('server_error', (e) => {
        es.close()
        if (typeof e?.data === 'string' && e.data.trim()) return onError(e.data)
        onError('AI 识别失败，请稍后重试')
      })

      es.onerror = () => {
        if (es.readyState === EventSource.CLOSED) return
        es.close()
        onError('连接中断，请稍后重试')
      }
    })
    .catch((err) => {
      if (cancelled) return
      onError(err?.message || '初始化失败，请稍后重试')
    })

  return {
    close() {
      cancelled = true
      if (es) es.close()
    }
  }
}
