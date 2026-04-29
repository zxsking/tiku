<template>
  <div class="question-detail-page" v-if="questionStore.currentQuestion">
    <el-row :gutter="20">
      <el-col :span="18">
        <el-card shadow="never" class="section-card question-card">
          <div class="question-header">
            <el-tag :type="getTypeTag(questionStore.currentQuestion.type)" size="large" class="type-tag">
              {{ getTypeName(questionStore.currentQuestion.type) }}
            </el-tag>
            <el-tag :type="getDifficultyTag(questionStore.currentQuestion.difficulty)" size="large">
              {{ getDifficultyName(questionStore.currentQuestion.difficulty) }}
            </el-tag>
            <span class="question-meta">
              <el-icon><Collection /></el-icon>
              <router-link :to="`/banks/${questionStore.currentQuestion.bankId}`">{{ questionStore.currentQuestion.bankName }}</router-link>
            </span>
          </div>

          <div class="question-content">
            <h3>{{ questionStore.currentQuestion.content }}</h3>
          </div>

          <div v-if="['single', 'multiple'].includes(questionStore.currentQuestion.type)" class="options-list">
            <div 
              v-for="(option, index) in questionStore.currentQuestion.options" 
              :key="index" 
              class="option-item"
              :class="{
                selected: isOptionSelected(index),
                correct: (showAnswer || submitted) && isOptionCorrect(index),
                wrong: submitted && isOptionSelected(index) && !isOptionCorrect(index),
              }"
              @click="selectOption(index)"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-text">{{ getOptionText(option) }}</span>
            </div>
          </div>

          <div v-if="questionStore.currentQuestion.type === 'judge'" class="judge-answer">
            <div class="judge-options">
              <button
                class="judge-option"
                :class="{
                  selected: userAnswer === true,
                  correct: (showAnswer || submitted) && normalizedJudgeAnswer === true,
                  wrong: submitted && userAnswer === true && normalizedJudgeAnswer !== true
                }"
                @click="selectJudge(true)"
              >
                正确
              </button>
              <button
                class="judge-option"
                :class="{
                  selected: userAnswer === false,
                  correct: (showAnswer || submitted) && normalizedJudgeAnswer === false,
                  wrong: submitted && userAnswer === false && normalizedJudgeAnswer !== false
                }"
                @click="selectJudge(false)"
              >
                错误
              </button>
            </div>
          </div>

          <div v-if="questionStore.currentQuestion.type === 'fill'" class="fill-answer">
            <el-input
              v-model="userAnswer"
              :disabled="submitted"
              placeholder="请输入你的填空答案..."
              class="answer-input"
              @keyup.enter.stop="submitLocalAnswer"
            />
            <div v-if="showAnswer || submitted" class="fill-answer-content">
              <div><strong>你的答案：</strong><span>{{ userAnswer || '（未作答）' }}</span></div>
              <strong>答案：</strong>
              <span v-for="(ans, idx) in normalizedFillAnswers" :key="idx">
                {{ ans }}<span v-if="idx < normalizedFillAnswers.length - 1">、</span>
              </span>
            </div>
          </div>
          
          <div v-if="questionStore.currentQuestion.type === 'essay'" class="fill-answer">
            <el-input
              v-model="userAnswer"
              type="textarea"
              :rows="4"
              :disabled="submitted"
              placeholder="请输入你的简答内容..."
              class="answer-input"
            />
            <div v-if="submitted" class="fill-answer-content">
              <div><strong>你的答案：</strong><span>{{ userAnswer || '（未作答）' }}</span></div>
              <div v-if="showAnswer"><strong>参考答案：</strong><span>{{ questionStore.currentQuestion.answer || '（暂无）' }}</span></div>
            </div>
          </div>

          <div class="action-bar">
            <button class="action-btn action-btn--active" :disabled="!canSubmit || submitted" @click="submitLocalAnswer">
              提交答案
            </button>
            <button class="action-btn action-btn--default" @click="resetLocalAnswer">
              重新作答
            </button>
            <button class="action-btn action-btn--default" @click="showAnswer = !showAnswer">
              <el-icon><View /></el-icon>
              {{ showAnswer ? '隐藏答案' : '查看答案' }}
            </button>
            <button 
              class="action-btn"
              :class="questionStore.currentQuestion.isLiked ? 'action-btn--active' : 'action-btn--default'"
              @click="toggleLike"
            >
              <el-icon><Pointer /></el-icon>
              {{ questionStore.currentQuestion.isLiked ? '已点赞' : '点赞' }} ({{ questionStore.currentQuestion.likeCount }})
            </button>
            <button 
              class="action-btn"
              :class="questionStore.currentQuestion.isFavorited ? 'action-btn--favorite' : 'action-btn--default'"
              @click="toggleFavorite"
            >
              <el-icon><Star /></el-icon>
              {{ questionStore.currentQuestion.isFavorited ? '已收藏' : '收藏' }}
            </button>
          </div>
          
          <div v-if="submitted" class="result-tip" :class="isCorrect === true ? 'is-correct' : isCorrect === false ? 'is-wrong' : 'is-neutral'">
            {{ resultText }}
          </div>

          <el-collapse-transition>
            <div v-show="showAnswer && questionStore.currentQuestion.analysis" class="analysis-section">
              <el-divider />
              <h4>答案解析</h4>
              <p>{{ questionStore.currentQuestion.analysis }}</p>
            </div>
          </el-collapse-transition>
        </el-card>

        <el-card shadow="never" class="section-card comments-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #e6a23c"></span>
                <span class="header-title">评论讨论</span>
                <el-tag type="info" size="small" class="total-tag">{{ comments.length }}</el-tag>
              </div>
            </div>
          </template>

          <div class="comment-form" v-if="userStore.isLoggedIn">
            <el-input 
              v-model="newComment" 
              type="textarea" 
              :rows="3" 
              maxlength="200"
              show-word-limit
              placeholder="写下你的评论..."
              class="comment-input"
            />
            <button class="submit-btn" @click="submitComment">
              <el-icon><ChatDotRound /></el-icon>发表评论
            </button>
          </div>
          <el-empty v-else description="登录后参与讨论" class="empty-state" />

          <div class="comments-list">
            <div v-for="comment in commentThreads" :key="comment.id" class="comment-item">
              <div class="comment-avatar user-link" @click="comment.userId && router.push(`/users/${comment.userId}`)">{{ comment.author?.charAt(0) || '用' }}</div>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="author user-link" @click="comment.userId && router.push(`/users/${comment.userId}`)">{{ comment.author }}</span>
                  <span class="time">{{ comment.createdAt }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <button class="text-op" @click="toggleReply(comment.id)">回复</button>
                  <button v-if="canDeleteComment(comment)" class="text-op text-op--danger" @click="handleDeleteComment(comment)">删除</button>
                </div>
                <div class="reply-box" v-if="replyingToId === comment.id && userStore.isLoggedIn">
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    :rows="2"
                    maxlength="200"
                    show-word-limit
                    :placeholder="replyPlaceholder"
                  />
                  <div class="reply-actions">
                    <button class="mini-btn mini-btn--primary" @click="submitReply(comment.id)">回复</button>
                    <button class="mini-btn" @click="cancelReply">取消</button>
                  </div>
                </div>
                <div class="reply-list" v-if="comment.children?.length">
                  <div v-for="reply in comment.children" :key="reply.id" class="reply-item">
                    <div class="reply-meta">
                      <span class="author user-link" @click="reply.userId && router.push(`/users/${reply.userId}`)">{{ reply.author }}</span>
                      <span class="time">{{ reply.createdAt }}</span>
                    </div>
                    <div class="comment-text">
                      <template v-if="reply.replyToAuthor">
                        <span>回复 </span>
                        <span
                          class="reply-to user-link"
                          @click="reply.replyToUserId && router.push(`/users/${reply.replyToUserId}`)"
                        >@{{ reply.replyToAuthor }}</span>
                        <span>：</span>
                      </template>
                      {{ reply.content }}
                    </div>
                    <div class="comment-actions">
                      <button class="text-op" @click="toggleReply(reply.id)">回复</button>
                      <button v-if="canDeleteComment(reply)" class="text-op text-op--danger" @click="handleDeleteComment(reply)">删除</button>
                    </div>
                    <div class="reply-box" v-if="replyingToId === reply.id && userStore.isLoggedIn">
                      <el-input
                        v-model="replyContent"
                        type="textarea"
                        :rows="2"
                        maxlength="200"
                        show-word-limit
                        :placeholder="replyPlaceholder"
                      />
                      <div class="reply-actions">
                        <button class="mini-btn mini-btn--primary" @click="submitReply(reply.id)">回复</button>
                        <button class="mini-btn" @click="cancelReply">取消</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="section-card info-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #67c23a"></span>
              <span class="header-title">题目信息</span>
            </div>
          </template>
          <el-descriptions :column="1" class="info-desc">
            <el-descriptions-item label="所属题库">{{ questionStore.currentQuestion.bankName }}</el-descriptions-item>
            <el-descriptions-item label="创建者">
              <span
                class="user-link"
                @click="questionStore.currentQuestion.authorId && router.push(`/users/${questionStore.currentQuestion.authorId}`)"
              >{{ questionStore.currentQuestion.author }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ questionStore.currentQuestion.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="浏览量">{{ questionStore.currentQuestion.viewCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useQuestionStore } from '@/store/question'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const questionStore = useQuestionStore()

const showAnswer = ref(false)
const newComment = ref('')
const comments = ref([])
const replyingToId = ref(null)
const replyContent = ref('')
const commentThreads = computed(() => {
  const byId = new Map(comments.value.map(c => [c.id, { ...c, children: [] }]))
  const roots = []

  for (const comment of byId.values()) {
    if (!comment.parentId) {
      roots.push(comment)
      continue
    }
    const parent = byId.get(comment.parentId)
    if (!parent) {
      roots.push(comment)
      continue
    }
    comment.replyToAuthor = parent.author
    comment.replyToUserId = parent.userId
    const rootId = findRootId(parent.id, byId)
    const root = byId.get(rootId)
    if (root && root.id !== comment.id) {
      root.children.push(comment)
    } else {
      roots.push(comment)
    }
  }

  roots.forEach(root => {
    root.children.sort((a, b) => Number(a.id) - Number(b.id))
  })
  return roots.sort((a, b) => Number(b.id) - Number(a.id))
})
const replyPlaceholder = computed(() => {
  if (!replyingToId.value) return '写下你的回复...'
  const target = comments.value.find(c => Number(c.id) === Number(replyingToId.value))
  const name = target?.author || 'Ta'
  return `回复 @${name}...`
})
const userAnswer = ref(null)
const submitted = ref(false)
const isCorrect = ref(null)

const normalizedObjectiveAnswers = computed(() => normalizeObjectiveAnswers(questionStore.currentQuestion?.answer))
const normalizedJudgeAnswer = computed(() => normalizeJudgeAnswer(questionStore.currentQuestion?.answer))
const normalizedFillAnswers = computed(() => normalizeFillAnswers(questionStore.currentQuestion?.answer))
const canSubmit = computed(() => {
  const type = questionStore.currentQuestion?.type
  if (type === 'single') return typeof userAnswer.value === 'number'
  if (type === 'multiple') return Array.isArray(userAnswer.value) && userAnswer.value.length > 0
  if (type === 'judge') return typeof userAnswer.value === 'boolean'
  if (type === 'fill' || type === 'essay') return String(userAnswer.value || '').trim().length > 0
  return false
})
const resultText = computed(() => {
  if (!submitted.value) return ''
  if (isCorrect.value === true) return '回答正确'
  if (isCorrect.value === false) return '回答错误'
  return '已提交，本题不自动判分'
})

function getTypeName(type) {
  const map = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }
  return map[type] || type
}

function getTypeTag(type) {
  const map = { single: undefined, multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }
  return map[type]
}

function getDifficultyName(diff) {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[diff] || diff
}

function getDifficultyTag(diff) {
  const map = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[diff]
}

/**
 * 兼容多种 options 存储格式，统一提取选项文本
 * - 字符串: "static"  → "static"
 * - {key,value}: {"key":"A","value":"static"} → "static"
 * - {label,text}: {"label":"A","text":"static"} → "static"
 */
function getOptionText(opt) {
  if (typeof opt === 'string') return opt
  if (opt && typeof opt === 'object') {
    return opt.value ?? opt.text ?? opt.content ?? JSON.stringify(opt)
  }
  return String(opt)
}

function normalizeObjectiveAnswers(raw) {
  const source = Array.isArray(raw) ? raw : [raw]
  const result = source
    .flatMap(item => (typeof item === 'string' ? item.split(/[,\s|]+/) : [item]))
    .map(item => {
      if (typeof item === 'number') return item
      if (typeof item === 'string') {
        const s = item.trim().toUpperCase()
        if (!s) return NaN
        if (/^\d+$/.test(s)) return Number(s)
        if (/^[A-Z]$/.test(s)) return s.charCodeAt(0) - 65
      }
      return NaN
    })
    .filter(num => Number.isInteger(num) && num >= 0)
  return [...new Set(result)]
}

function normalizeJudgeAnswer(raw) {
  if (typeof raw === 'boolean') return raw
  if (typeof raw === 'number') return raw === 1
  if (typeof raw === 'string') {
    const v = raw.trim().toLowerCase()
    if (['true', '1', 'yes', 'y', '正确', '对'].includes(v)) return true
    if (['false', '0', 'no', 'n', '错误', '错'].includes(v)) return false
  }
  return null
}

function normalizeFillAnswers(raw) {
  if (Array.isArray(raw)) return raw.map(v => String(v).trim()).filter(Boolean)
  if (typeof raw === 'string') return raw.split('|').map(v => v.trim()).filter(Boolean)
  return []
}

function normalizeText(value) {
  return String(value || '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function findRootId(commentId, byId) {
  let current = byId.get(commentId)
  const visited = new Set()
  while (current && current.parentId && !visited.has(current.id)) {
    visited.add(current.id)
    const parent = byId.get(current.parentId)
    if (!parent) break
    current = parent
  }
  return current?.id ?? commentId
}

function resetLocalAnswer() {
  const type = questionStore.currentQuestion?.type
  if (type === 'multiple') userAnswer.value = []
  else if (type === 'judge') userAnswer.value = null
  else userAnswer.value = ''

  if (type === 'single') userAnswer.value = null
  submitted.value = false
  isCorrect.value = null
}

function isOptionSelected(index) {
  const type = questionStore.currentQuestion?.type
  if (type === 'single') return userAnswer.value === index
  if (type === 'multiple') return Array.isArray(userAnswer.value) && userAnswer.value.includes(index)
  return false
}

function isOptionCorrect(index) {
  return normalizedObjectiveAnswers.value.includes(index)
}

function selectOption(index) {
  if (submitted.value) return
  const type = questionStore.currentQuestion?.type
  if (type === 'single') {
    userAnswer.value = index
    return
  }
  if (type === 'multiple') {
    const selected = Array.isArray(userAnswer.value) ? [...userAnswer.value] : []
    const target = selected.indexOf(index)
    if (target >= 0) selected.splice(target, 1)
    else selected.push(index)
    userAnswer.value = selected
  }
}

function selectJudge(value) {
  if (submitted.value) return
  userAnswer.value = value
}

function submitLocalAnswer() {
  const type = questionStore.currentQuestion?.type
  if (!canSubmit.value) {
    ElMessage.warning('请先作答后再提交')
    return
  }

  submitted.value = true
  showAnswer.value = true

  if (type === 'single') {
    isCorrect.value = userAnswer.value === normalizedObjectiveAnswers.value[0]
  } else if (type === 'multiple') {
    const userSet = new Set(Array.isArray(userAnswer.value) ? userAnswer.value : [])
    const ansSet = new Set(normalizedObjectiveAnswers.value)
    isCorrect.value = userSet.size === ansSet.size && [...userSet].every(v => ansSet.has(v))
  } else if (type === 'judge') {
    isCorrect.value = userAnswer.value === normalizedJudgeAnswer.value
  } else if (type === 'fill') {
    const user = normalizeText(userAnswer.value)
    isCorrect.value = normalizedFillAnswers.value.some(ans => normalizeText(ans) === user)
  } else {
    isCorrect.value = null
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  await questionStore.toggleLike(route.params.id)
  const q = questionStore.currentQuestion
  if (q.isLiked) {
    q.isLiked = false
    q.likeCount = (q.likeCount || 1) - 1
  } else {
    q.isLiked = true
    q.likeCount = (q.likeCount || 0) + 1
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  await questionStore.toggleFavorite(route.params.id)
  const q = questionStore.currentQuestion
  q.isFavorited = !q.isFavorited
  ElMessage.success(q.isFavorited ? '收藏成功' : '已取消收藏')
}

async function submitComment() {
  const content = newComment.value.trim()
  if (!content) return
  if (content.length > 200) {
    ElMessage.warning('评论不能超过 200 字')
    return
  }
  const comment = await questionStore.addComment(route.params.id, content)
  comments.value.unshift(comment)
  newComment.value = ''
  ElMessage.success('评论成功')
}

function canDeleteComment(comment) {
  if (!userStore.isLoggedIn) return false
  if (userStore.isAdmin) return true
  return Number(comment.userId) === Number(userStore.userInfo?.id)
}

function toggleReply(commentId) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (replyingToId.value === commentId) {
    cancelReply()
    return
  }
  replyingToId.value = commentId
  replyContent.value = ''
}

function cancelReply() {
  replyingToId.value = null
  replyContent.value = ''
}

async function submitReply(parentId) {
  const content = replyContent.value.trim()
  if (!content) return
  if (content.length > 200) {
    ElMessage.warning('回复不能超过 200 字')
    return
  }
  const reply = await questionStore.addComment(route.params.id, content, parentId)
  comments.value.unshift(reply)
  cancelReply()
  ElMessage.success('回复成功')
}

async function handleDeleteComment(comment) {
  await questionStore.deleteComment(route.params.id, comment.id)
  const data = await questionStore.fetchComments(route.params.id, { page: 1, size: 200 })
  comments.value = data.records || []
  if (replyingToId.value === comment.id) cancelReply()
  ElMessage.success('删除成功')
}

onMounted(async () => {
  await questionStore.fetchQuestion(route.params.id)
  resetLocalAnswer()
  const data = await questionStore.fetchComments(route.params.id, { page: 1, size: 50 })
  comments.value = data.records || []
})
</script>

<style lang="scss" scoped>
.question-detail-page {
  padding: 4px 0;
}

.section-card {
  margin-bottom: 20px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f5;
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-dot {
  width: 4px;
  height: 18px;
  border-radius: 4px;
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.total-tag {
  border-radius: 20px;
  :deep(.el-tag__content) { color: #909399; }
}

.question-card {
  :deep(.el-card__body) { padding: 28px; }
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;

  .type-tag {
    font-weight: 600;
  }

  .question-meta {
    margin-left: auto;
    color: #909399;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 4px;

    a {
      color: #409eff;
      text-decoration: none;
      transition: color 0.15s;

      &:hover {
        color: #2d87f0;
      }
    }
  }
}

.question-content {
  margin-bottom: 24px;

  h3 {
    margin: 0;
    line-height: 1.7;
    font-size: 16px;
    color: #303133;
    font-weight: 500;
  }
}

.options-list {
  .option-item {
    padding: 14px 16px;
    margin-bottom: 10px;
    background: #f7f9fc;
    border-radius: 8px;
    border: 1px solid #edf0f5;
    transition: all 0.2s;

    &:hover {
      background: #f0f6ff;
      border-color: #c6e0ff;
      cursor: pointer;
    }

    &.selected {
      background: #ecf5ff;
      border-color: #409eff;
    }

    &.correct {
      background: #f0f9eb;
      border-color: #67c23a;
    }
    
    &.wrong {
      background: #fff2f2;
      border-color: #f56c6c;
    }

    .option-label {
      font-weight: 600;
      margin-right: 8px;
      color: #303133;
    }

    .option-text {
      color: #606266;
      font-size: 14px;
    }
  }
}

.judge-answer {
  margin: 20px 0;
}

.judge-options {
  display: flex;
  gap: 10px;
}

.judge-option {
  height: 38px;
  min-width: 100px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  &.selected {
    border-color: #409eff;
    color: #409eff;
    background: #ecf5ff;
  }

  &.correct {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #67c23a;
  }

  &.wrong {
    border-color: #f56c6c;
    background: #fff2f2;
    color: #f56c6c;
  }
}

.answer-input {
  margin-bottom: 10px;
}

.fill-answer-content {
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 8px;
  border: 1px solid #edf0f5;
  font-size: 14px;

  strong {
    color: #303133;
  }

  span {
    color: #606266;
  }
}

.action-bar {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s, transform 0.15s;
  white-space: nowrap;
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &:active { transform: scale(0.97); }

  &--default {
    background: #f0f6ff;
    color: #409eff;
    border: 1px solid #c6e0ff;

    &:hover { background: #e0efff; }
  }

  &--active {
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);

    &:hover { opacity: 0.88; }
  }

  &--favorite {
    background: linear-gradient(135deg, #f56c6c, #e85555);
    color: #fff;
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);

    &:hover { opacity: 0.88; }
  }
}

.result-tip {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;

  &.is-correct {
    background: #f0f9eb;
    color: #67c23a;
    border: 1px solid #d9f2c4;
  }

  &.is-wrong {
    background: #fff2f2;
    color: #f56c6c;
    border: 1px solid #ffd5d5;
  }

  &.is-neutral {
    background: #f4f4f5;
    color: #606266;
    border: 1px solid #e5e7eb;
  }
}

.analysis-section {
  margin-top: 20px;

  h4 {
    margin-bottom: 12px;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  p {
    color: #606266;
    line-height: 1.7;
    font-size: 14px;
    padding: 14px 16px;
    background: #f7f9fc;
    border-radius: 8px;
    border: 1px solid #edf0f5;
  }
}

.comments-card {
  :deep(.el-card__body) { padding: 20px; }
}

.comment-form {
  margin-bottom: 20px;

  .comment-input {
    :deep(.el-textarea__inner) {
      border-radius: 8px;
      border-color: #edf0f5;

      &:focus {
        border-color: #409eff;
      }
    }
  }

  .submit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 36px;
    padding: 0 16px;
    margin-top: 10px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    border: none;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    transition: opacity 0.2s, transform 0.15s;

    &:hover { opacity: 0.88; }
    &:active { transform: scale(0.97); }
  }
}

.comments-list {
  .comment-item {
    display: flex;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid #f2f4f8;

    &:last-child {
      border-bottom: none;
    }

    .comment-avatar {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, #e6a23c, #d08f20);
      color: #fff;
      font-size: 14px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.user-link {
        cursor: pointer;
        &:hover { opacity: 0.82; transform: scale(1.06); }
        transition: opacity 0.15s, transform 0.15s;
      }
    }

    .comment-content {
      flex: 1;
      min-width: 0;

      .comment-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;

        .author {
          font-weight: 600;
          font-size: 14px;
          color: #303133;

          &.user-link {
            cursor: pointer;
            &:hover { color: #409eff; text-decoration: underline; }
          }
        }

        .time {
          color: #909399;
          font-size: 12px;
        }
      }

      .comment-text {
        color: #606266;
        font-size: 13px;
        line-height: 1.6;
      }

      .comment-actions {
        margin-top: 6px;
        display: flex;
        gap: 10px;
      }
    }
  }
}

.text-op {
  border: none;
  background: transparent;
  color: #909399;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  transition: color 0.15s;

  &:hover {
    color: #409eff;
  }

  &--danger:hover {
    color: #f56c6c;
  }
}

.reply-box {
  margin-top: 10px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.mini-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 12px;

  &--primary {
    border-color: #409eff;
    background: #409eff;
    color: #fff;
  }
}

.reply-list {
  margin-top: 10px;
  background: #fafbfd;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 8px 12px;
}

.reply-item {
  padding: 10px 0;
  border-bottom: 1px solid #f2f4f8;

  &:last-child {
    border-bottom: none;
  }
}

.reply-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;

  .author {
    font-weight: 600;
    font-size: 13px;
    color: #303133;
  }

  .time {
    color: #909399;
    font-size: 12px;
  }
}

.reply-to {
  color: #409eff;
  font-weight: 500;
}

.info-card {
  :deep(.el-card__body) { padding: 20px; }
}

.info-desc {
  :deep(.el-descriptions__label) {
    color: #909399;
    font-size: 13px;
  }

  :deep(.el-descriptions__content) {
    color: #303133;
    font-size: 13px;
  }
}

.empty-state {
  padding: 40px 0;
}

.user-link {
  cursor: pointer;
  user-select: none;
  &:hover {
    color: #409eff;
    text-decoration: underline !important;
  }
}
</style>