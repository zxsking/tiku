<template>
  <div class="review-page">
    <div class="page-back">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回错题本
      </el-button>
    </div>

    <div class="page-title">
      <el-icon><Refresh /></el-icon>
      今日复习
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 全部完成 -->
    <div v-else-if="allDone" class="done-state">
      <el-card shadow="never" class="done-card">
        <div class="done-icon">🎉</div>
        <div class="done-title">今日复习完成！</div>
        <div class="done-sub">
          共复习 {{ totalCount }} 道题，答对 {{ correctCount }} 道，
          正确率 {{ totalCount > 0 ? Math.round((correctCount / totalCount) * 100) : 0 }}%
        </div>
        <div class="done-actions">
          <el-button type="primary" @click="$router.push('/user/wrong-book')">返回错题本</el-button>
          <el-button @click="$router.push('/user/wrong-book/stats')">查看统计</el-button>
        </div>
      </el-card>
    </div>

    <!-- 无任务 -->
    <div v-else-if="list.length === 0" class="empty-state">
      <el-card shadow="never" class="empty-card">
        <el-empty description="今日暂无待复习题目">
          <div class="empty-hint">基于艾宾浩斯遗忘曲线，系统会在合适时间推送复习任务</div>
          <el-button type="primary" @click="$router.push('/user/wrong-book')">查看错题本</el-button>
        </el-empty>
      </el-card>
    </div>

    <!-- 复习中 -->
    <template v-else>
      <!-- 进度条 -->
      <el-card shadow="never" class="progress-card">
        <div class="progress-info">
          <div class="progress-text">
            <span class="progress-current">{{ doneCount }}</span>
            <span class="progress-sep">/</span>
            <span class="progress-total">{{ totalCount }}</span>
            <span class="progress-label">已完成</span>
          </div>
          <div class="progress-rate" v-if="doneCount > 0">
            正确率 {{ Math.round((correctCount / doneCount) * 100) }}%
          </div>
        </div>
        <el-progress
          :percentage="Math.round((doneCount / totalCount) * 100)"
          :color="progressColor"
          :stroke-width="8"
          :show-text="false"
        />
      </el-card>

      <!-- 题目卡片 -->
      <Transition name="q-slide" mode="out-in">
        <el-card
          shadow="never"
          class="question-card"
          :key="currentItem?.id"
          v-if="currentItem && !showResult"
        >
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <el-tag size="small" :type="typeTagMap[currentItem.type]?.type">
                  {{ typeTagMap[currentItem.type]?.label }}
                </el-tag>
                <el-tag size="small" :type="diffTagMap[currentItem.difficulty]?.type">
                  {{ diffTagMap[currentItem.difficulty]?.label }}
                </el-tag>
              </div>
              <span class="wrong-badge">
                <el-icon><WarningFilled /></el-icon>
                错 {{ currentItem.wrongCount }} 次
              </span>
            </div>
          </template>

          <div class="question-content">{{ currentItem.content }}</div>

          <!-- 单选 -->
          <div v-if="currentItem.type === 'single'" class="options-list">
            <div
              v-for="opt in normalizeOptions(currentItem.options)"
              :key="opt.key"
              class="option-item"
              :class="{ 'option-item--selected': userAnswer === opt.key }"
              @click="selectAnswer(opt.key)"
            >
              <span class="opt-badge" :class="{ 'opt-badge--selected': userAnswer === opt.key }">
                {{ opt.key }}
              </span>
              <span>{{ opt.value }}</span>
            </div>
          </div>

          <!-- 多选 -->
          <div v-else-if="currentItem.type === 'multiple'" class="options-list">
            <div class="multi-hint"><el-icon><InfoFilled /></el-icon> 多选题，请选择所有正确答案</div>
            <div
              v-for="opt in normalizeOptions(currentItem.options)"
              :key="opt.key"
              class="option-item"
              :class="{ 'option-item--selected': Array.isArray(userAnswer) && userAnswer.includes(opt.key) }"
              @click="toggleMulti(opt.key)"
            >
              <span class="opt-badge" :class="{ 'opt-badge--selected': Array.isArray(userAnswer) && userAnswer.includes(opt.key) }">
                <el-icon v-if="Array.isArray(userAnswer) && userAnswer.includes(opt.key)"><Check /></el-icon>
                <span v-else>{{ opt.key }}</span>
              </span>
              <span>{{ opt.value }}</span>
            </div>
          </div>

          <!-- 判断 -->
          <div v-else-if="currentItem.type === 'judge'" class="judge-options">
            <div
              class="judge-btn"
              :class="{ 'judge-btn--selected': userAnswer === 'true' }"
              @click="selectAnswer('true')"
            >
              <el-icon><CircleCheck /></el-icon> 正确
            </div>
            <div
              class="judge-btn"
              :class="{ 'judge-btn--selected': userAnswer === 'false' }"
              @click="selectAnswer('false')"
            >
              <el-icon><CircleClose /></el-icon> 错误
            </div>
          </div>

          <!-- 填空 -->
          <div v-else-if="currentItem.type === 'fill'" class="fill-area">
            <el-input v-model="fillInput" placeholder="请输入答案..." clearable @keyup.enter.stop />
          </div>

          <!-- 简答 -->
          <div v-else-if="currentItem.type === 'essay'" class="essay-area">
            <div class="fill-hint"><el-icon><EditPen /></el-icon> 简答题，请在下方作答</div>
            <el-input v-model="fillInput" type="textarea" :rows="4" placeholder="请输入你的答案..." @keyup.enter.stop />
          </div>

          <!-- 提交按钮 -->
          <div class="submit-bar">
            <el-button
              type="primary"
              :disabled="!hasAnswer"
              @click="handleSubmit"
              :loading="submitting"
            >
              提交答案
            </el-button>
            <el-button @click="skipCurrent" :loading="submitting">跳过</el-button>
          </div>
        </el-card>
      </Transition>

      <!-- 答案解析（提交后显示） -->
      <Transition name="q-slide" mode="out-in">
        <el-card
          shadow="never"
          class="result-card"
          :key="`result-${currentItem?.id}`"
          v-if="currentItem && showResult"
        >
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <div class="result-icon" :class="lastCorrect ? 'result-icon--correct' : 'result-icon--wrong'">
                  <el-icon v-if="lastCorrect"><Check /></el-icon>
                  <el-icon v-else><Close /></el-icon>
                </div>
                <span class="result-text" :class="lastCorrect ? 'text-correct' : 'text-wrong'">
                  {{ lastCorrect ? '回答正确！' : '回答错误' }}
                </span>
              </div>
            </div>
          </template>

          <div class="question-content">{{ currentItem.content }}</div>

          <!-- 选项高亮 -->
          <div v-if="currentItem.options && currentItem.options.length" class="options-list">
            <div
              v-for="opt in normalizeOptions(currentItem.options)"
              :key="opt.key"
              class="option-item"
              :class="resultOptionClass(opt.key)"
            >
              <span class="opt-badge" :class="resultBadgeClass(opt.key)">{{ opt.key }}</span>
              <span>{{ opt.value }}</span>
              <span class="opt-mark">
                <el-icon v-if="isCorrectOption(opt.key)" color="#67c23a"><Check /></el-icon>
                <el-icon v-else-if="isUserWrong(opt.key)" color="#f56c6c"><Close /></el-icon>
              </span>
            </div>
          </div>

          <!-- 判断题结果 -->
          <div v-else-if="currentItem.type === 'judge'" class="judge-result">
            <span>正确答案：</span>
            <el-tag :type="String(currentItem.correctAnswer) === 'true' ? 'success' : 'danger'">
              {{ String(currentItem.correctAnswer) === 'true' ? '正确' : '错误' }}
            </el-tag>
          </div>

          <!-- 填空/简答结果 -->
          <div v-else-if="currentItem.type === 'fill' || currentItem.type === 'essay'" class="text-result">
            <div class="text-result-row">
              <span class="tr-label">你的答案：</span>
              <span :class="lastCorrect ? 'tr-correct' : 'tr-wrong'">{{ fillInput || '（未作答）' }}</span>
            </div>
            <div class="text-result-row" v-if="currentItem.correctAnswer">
              <span class="tr-label">正确答案：</span>
              <span class="tr-correct">{{ formatAnswer(currentItem.correctAnswer) }}</span>
            </div>
          </div>

          <!-- 解析 -->
          <div v-if="currentItem.analysis" class="analysis-block">
            <div class="analysis-title"><el-icon><Promotion /></el-icon> 题目解析</div>
            <div class="analysis-content">{{ currentItem.analysis }}</div>
          </div>

          <div class="next-bar">
            <el-button type="primary" @click="nextQuestion">
              {{ isLast ? '完成复习' : '下一题' }}
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-card>
      </Transition>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Check, Close, CircleCheck, CircleClose,
  WarningFilled, InfoFilled, EditPen, Refresh, Promotion
} from '@element-plus/icons-vue'
import { useWrongBookStore } from '@/store/wrongBook'

const router = useRouter()
const wrongBookStore = useWrongBookStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const currentIndex = ref(0)
const showResult = ref(false)
const userAnswer = ref(null)
const fillInput = ref('')
const lastCorrect = ref(false)
const doneCount = ref(0)
const correctCount = ref(0)
const allDone = ref(false)

const typeTagMap = {
  single: { label: '单选题', type: '' },
  multiple: { label: '多选题', type: 'warning' },
  judge: { label: '判断题', type: 'success' },
  fill: { label: '填空题', type: 'info' },
  essay: { label: '简答题', type: 'info' }
}

const diffTagMap = {
  easy: { label: '简单', type: 'success' },
  medium: { label: '中等', type: 'warning' },
  hard: { label: '困难', type: 'danger' }
}

const totalCount = computed(() => list.value.length)
const currentItem = computed(() => list.value[currentIndex.value] || null)
const isLast = computed(() => currentIndex.value >= list.value.length - 1)

const progressColor = computed(() => {
  const pct = doneCount.value / totalCount.value
  if (pct >= 0.8) return '#67c23a'
  if (pct >= 0.5) return '#409eff'
  return '#e6a23c'
})

const hasAnswer = computed(() => {
  const item = currentItem.value
  if (!item) return false
  if (item.type === 'multiple') return Array.isArray(userAnswer.value) && userAnswer.value.length > 0
  if (item.type === 'fill' || item.type === 'essay') return fillInput.value.trim().length > 0
  return userAnswer.value !== null && userAnswer.value !== undefined
})

function normalizeOptions(options) {
  if (!options || !Array.isArray(options)) return []
  return options.map((opt, i) => {
    if (typeof opt === 'string') return { key: String.fromCharCode(65 + i), value: opt }
    return {
      key: opt.key || opt.label || String.fromCharCode(65 + i),
      value: opt.value || opt.text || opt.content || String(opt)
    }
  })
}

function formatAnswer(answer) {
  if (answer === null || answer === undefined || answer === '') return '—'
  if (Array.isArray(answer)) return answer.join('、')
  return String(answer)
}

function selectAnswer(key) {
  userAnswer.value = key
}

function toggleMulti(key) {
  const current = Array.isArray(userAnswer.value) ? [...userAnswer.value] : []
  const idx = current.indexOf(key)
  if (idx === -1) current.push(key)
  else current.splice(idx, 1)
  userAnswer.value = current
}

function isCorrectOption(key) {
  const ans = currentItem.value?.correctAnswer
  if (!ans) return false
  if (Array.isArray(ans)) return ans.map(String).includes(String(key))
  return String(ans) === String(key)
}

function isUserWrong(key) {
  const ua = userAnswer.value
  if (!ua) return false
  if (Array.isArray(ua)) return ua.includes(key) && !isCorrectOption(key)
  return String(ua) === String(key) && !isCorrectOption(key)
}

function resultOptionClass(key) {
  return {
    'option-item--correct': isCorrectOption(key),
    'option-item--wrong': isUserWrong(key)
  }
}

function resultBadgeClass(key) {
  return {
    'opt-badge--correct': isCorrectOption(key),
    'opt-badge--wrong': isUserWrong(key)
  }
}

function judgeCorrect() {
  const item = currentItem.value
  if (!item) return false
  const correct = item.correctAnswer
  if (item.type === 'multiple') {
    const ua = new Set((Array.isArray(userAnswer.value) ? userAnswer.value : []).map(String))
    const ca = new Set((Array.isArray(correct) ? correct : String(correct).split(',')).map(s => s.trim()))
    return ua.size === ca.size && [...ua].every(k => ca.has(k))
  }
  if (item.type === 'fill') {
    return fillInput.value.trim().toLowerCase() === String(correct).trim().toLowerCase()
  }
  if (item.type === 'essay') return true // 简答题默认已作答
  return String(userAnswer.value) === String(correct)
}

async function handleSubmit() {
  if (!currentItem.value || !hasAnswer.value) return
  submitting.value = true
  try {
    const isCorrect = judgeCorrect()
    lastCorrect.value = isCorrect
    await wrongBookStore.doSubmitReview(currentItem.value.id, isCorrect)
    doneCount.value++
    if (isCorrect) correctCount.value++
    showResult.value = true
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function skipCurrent() {
  submitting.value = true
  try {
    await wrongBookStore.doSubmitReview(currentItem.value.id, null)
    doneCount.value++
    showResult.value = true
    lastCorrect.value = false
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (isLast.value) {
    allDone.value = true
    return
  }
  currentIndex.value++
  showResult.value = false
  userAnswer.value = null
  fillInput.value = ''
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await wrongBookStore.fetchTodayReview()
    list.value = data
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.review-page {
  padding: 4px 0;
  max-width: 720px;
  margin: 0 auto;
}

.page-back {
  margin-bottom: 12px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

.loading-wrap {
  padding: 20px;
}

// ── 进度卡片 ──────────────────────────────────────────
.progress-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  margin-bottom: 16px;

  :deep(.el-card__body) { padding: 16px 20px; }
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.progress-text {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.progress-current {
  font-size: 24px;
  font-weight: 800;
  color: #409eff;
}

.progress-sep {
  font-size: 18px;
  color: #c0c4cc;
}

.progress-total {
  font-size: 18px;
  font-weight: 600;
  color: #909399;
}

.progress-label {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.progress-rate {
  font-size: 14px;
  color: #67c23a;
  font-weight: 600;
}

// ── 题目卡片 ──────────────────────────────────────────
.question-card,
.result-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 14px 20px;
    background: #fafbfd;
    border-bottom: 1px solid #edf0f5;
  }

  :deep(.el-card__body) { padding: 20px; }
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

.wrong-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #fff;
  background: #f56c6c;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;
}

.question-content {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  line-height: 1.8;
  margin-bottom: 20px;
}

// ── 选项 ──────────────────────────────────────────────
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.multi-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #e6a23c;
  margin-bottom: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1.5px solid #edf0f5;
  background: #fafbfd;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s;

  &:hover { border-color: #409eff; background: #ecf5ff; }

  &--selected {
    border-color: #409eff;
    background: #ecf5ff;
    color: #303133;
  }

  &--correct {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #303133;
    cursor: default;
  }

  &--wrong {
    border-color: #f56c6c;
    background: #fef0f0;
    cursor: default;
  }
}

.opt-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  background: #edf0f5;
  color: #606266;
  transition: all 0.15s;

  &--selected { background: #409eff; color: #fff; }
  &--correct { background: #67c23a; color: #fff; }
  &--wrong { background: #f56c6c; color: #fff; }
}

.opt-mark {
  margin-left: auto;
  flex-shrink: 0;
}

// ── 判断题 ────────────────────────────────────────────
.judge-options {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.judge-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  border-radius: 10px;
  border: 1.5px solid #edf0f5;
  font-size: 15px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s;

  &:hover { border-color: #409eff; background: #ecf5ff; }

  &--selected {
    border-color: #409eff;
    background: #409eff;
    color: #fff;
  }
}

.judge-result {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

// ── 填空/简答 ─────────────────────────────────────────
.fill-area,
.essay-area {
  margin-bottom: 20px;
}

.fill-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

// ── 文字答案结果 ──────────────────────────────────────
.text-result {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f7f9fc;
  border-radius: 8px;
}

.text-result-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 6px;

  &:last-child { margin-bottom: 0; }
}

.tr-label { color: #909399; flex-shrink: 0; }
.tr-correct { color: #67c23a; font-weight: 600; }
.tr-wrong { color: #f56c6c; font-weight: 600; }

// ── 解析 ──────────────────────────────────────────────
.analysis-block {
  margin-top: 16px;
  padding: 14px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  border: 1px solid #f5dab1;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 8px;
}

.analysis-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}

// ── 提交/下一题 ───────────────────────────────────────
.submit-bar {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #edf0f5;
}

.next-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #edf0f5;
}

// ── 结果图标 ──────────────────────────────────────────
.result-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;

  &--correct { background: #67c23a; }
  &--wrong { background: #f56c6c; }
}

.result-text {
  font-size: 16px;
  font-weight: 700;
}

.text-correct { color: #67c23a; }
.text-wrong { color: #f56c6c; }

// ── 完成/空状态 ───────────────────────────────────────
.done-card,
.empty-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  text-align: center;

  :deep(.el-card__body) { padding: 48px 20px; }
}

.done-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.done-title {
  font-size: 22px;
  font-weight: 800;
  color: #303133;
  margin-bottom: 8px;
}

.done-sub {
  font-size: 15px;
  color: #606266;
  margin-bottom: 24px;
}

.done-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.empty-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

// ── 切题动画 ──────────────────────────────────────────
.q-slide-enter-active,
.q-slide-leave-active {
  transition: all 0.2s ease;
}

.q-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.q-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
