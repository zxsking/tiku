<template>
  <div class="practice-exam-page" tabindex="0" @keydown="handleKeydown">
    <div v-if="practiceStore.loading && !practiceStore.questions.length" class="loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="practiceStore.questions.length">
      <!-- 顶部固定进度条 -->
      <div class="exam-progress-bar">
        <div
          class="exam-progress-fill"
          :style="{ width: `${(practiceStore.answeredCount / practiceStore.totalCount) * 100}%` }"
        ></div>
      </div>

      <el-row :gutter="16">
        <!-- 主答题区 -->
        <el-col :span="17">
          <!-- 顶部信息栏 -->
          <el-card shadow="never" class="top-bar-card">
            <div class="top-bar">
              <div class="top-bar-left">
                <span class="q-index">
                  <span class="q-current">{{ currentIndex + 1 }}</span>
                  <span class="q-sep"> / </span>
                  <span class="q-total">{{ practiceStore.totalCount }}</span>
                </span>
                <el-tag v-if="currentQ" size="small" :type="typeTagMap[currentQ.type]?.type">
                  {{ typeTagMap[currentQ.type]?.label }}
                </el-tag>
                <el-tag v-if="currentQ" size="small" :type="diffTagMap[currentQ.difficulty]?.type">
                  {{ diffTagMap[currentQ.difficulty]?.label }}
                </el-tag>
              </div>
              <div class="top-bar-right">
                <div class="progress-text">
                  已答 <b>{{ practiceStore.answeredCount }}</b>/{{ practiceStore.totalCount }}
                </div>
                <div class="timer" :class="timerClass">
                  <el-icon><Timer /></el-icon>
                  <span>{{ timerDisplay }}</span>
                </div>
                <el-button
                  size="small"
                  :type="practiceStore.markedQuestions.has(currentQ?.id) ? 'warning' : 'default'"
                  @click="toggleMark"
                >
                  <el-icon><Flag /></el-icon>
                  {{ practiceStore.markedQuestions.has(currentQ?.id) ? '已标记' : '标记' }}
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- 题目内容 -->
          <Transition name="q-slide" mode="out-in">
            <el-card shadow="never" class="question-card" :key="currentIndex" v-if="currentQ">
              <div class="question-content">
                <div class="question-text">
                  <span class="q-num">{{ currentIndex + 1 }}.</span>
                  {{ currentQ.content }}
                </div>

                <div v-if="currentQ.type === 'multiple'" class="multi-hint">
                  <el-icon><InfoFilled /></el-icon> 多选题，请选择所有正确答案
                </div>

                <!-- 单选题 -->
                <div v-if="currentQ.type === 'single'" class="options-list">
                  <div
                    v-for="opt in currentOptions"
                    :key="opt.key"
                    class="option-item"
                    :class="{ 'option-item--selected': currentAnswer === opt.key }"
                    @click="selectSingle(opt.key)"
                  >
                    <span class="option-badge" :class="{ 'option-badge--selected': currentAnswer === opt.key }">
                      {{ opt.key }}
                    </span>
                    <span class="option-text">{{ opt.value }}</span>
                  </div>
                </div>

                <!-- 多选题 -->
                <div v-else-if="currentQ.type === 'multiple'" class="options-list">
                  <div
                    v-for="opt in currentOptions"
                    :key="opt.key"
                    class="option-item option-item--checkbox"
                    :class="{ 'option-item--selected': Array.isArray(currentAnswer) && currentAnswer.includes(opt.key) }"
                    @click="toggleMulti(opt.key)"
                  >
                    <span
                      class="option-badge option-badge--checkbox"
                      :class="{ 'option-badge--selected': Array.isArray(currentAnswer) && currentAnswer.includes(opt.key) }"
                    >
                      <el-icon v-if="Array.isArray(currentAnswer) && currentAnswer.includes(opt.key)"><Check /></el-icon>
                      <span v-else>{{ opt.key }}</span>
                    </span>
                    <span class="option-text">{{ opt.value }}</span>
                  </div>
                </div>

                <!-- 判断题 -->
                <div v-else-if="currentQ.type === 'judge'" class="judge-options">
                  <div
                    class="judge-btn"
                    :class="{ 'judge-btn--correct': currentAnswer === 'true', 'judge-btn--selected': currentAnswer === 'true' }"
                    @click="selectSingle('true')"
                  >
                    <el-icon><CircleCheck /></el-icon>
                    <span>正确</span>
                  </div>
                  <div
                    class="judge-btn"
                    :class="{ 'judge-btn--wrong': currentAnswer === 'false', 'judge-btn--selected': currentAnswer === 'false' }"
                    @click="selectSingle('false')"
                  >
                    <el-icon><CircleClose /></el-icon>
                    <span>错误</span>
                  </div>
                </div>

                <!-- 填空题 -->
                <div v-else-if="currentQ.type === 'fill'" class="fill-area">
                  <div class="fill-hint">
                    <el-icon><EditPen /></el-icon> 请在下方输入答案
                  </div>
                  <el-input
                    v-model="fillAnswer"
                    placeholder="请输入填空答案..."
                    clearable
                    @input="onFillInput"
                    @keyup.enter.stop
                  />
                </div>

                <!-- 简答题 -->
                <div v-else-if="currentQ.type === 'essay'" class="essay-area">
                  <div class="fill-hint">
                    <el-icon><EditPen /></el-icon> 请在下方作答（简答题不自动判分）
                  </div>
                  <el-input
                    v-model="fillAnswer"
                    type="textarea"
                    :rows="5"
                    placeholder="请输入你的答案..."
                    @input="onFillInput"
                    @keyup.enter.stop
                  />
                </div>
              </div>

              <!-- 底部翻页 -->
              <div class="nav-buttons">
                <el-button :disabled="currentIndex === 0" @click="prevQuestion">
                  <el-icon><ArrowLeft /></el-icon>上一题
                </el-button>
                <div class="keyboard-hint">
                  <span>← → 切换题目</span>
                </div>
                <el-button
                  v-if="currentIndex < practiceStore.totalCount - 1"
                  type="primary"
                  @click="nextQuestion"
                >
                  下一题<el-icon><ArrowRight /></el-icon>
                </el-button>
                <el-button v-else type="success" @click="handleSubmitConfirm">
                  <el-icon><Check /></el-icon>提交答卷
                </el-button>
              </div>
            </el-card>
          </Transition>
        </el-col>

        <!-- 右侧导航面板 -->
        <el-col :span="7">
          <el-card shadow="never" class="nav-card">
            <template #header>
              <div class="nav-header">
                <span class="header-title">答题卡</span>
                <el-tag size="small" :type="progressTagType">
                  {{ practiceStore.answeredCount }}/{{ practiceStore.totalCount }}
                </el-tag>
              </div>
            </template>

            <!-- 进度环 -->
            <div class="nav-progress-wrap">
              <el-progress
                type="circle"
                :percentage="Math.round((practiceStore.answeredCount / practiceStore.totalCount) * 100)"
                :width="72"
                :stroke-width="6"
                :color="progressColor"
              />
              <div class="nav-progress-labels">
                <div class="nav-legend">
                  <span class="legend-item"><i class="dot dot--answered"></i>已答</span>
                  <span class="legend-item"><i class="dot dot--marked"></i>标记</span>
                  <span class="legend-item"><i class="dot dot--current"></i>当前</span>
                  <span class="legend-item"><i class="dot dot--unanswered"></i>未答</span>
                </div>
              </div>
            </div>

            <div class="nav-grid">
              <div
                v-for="(q, idx) in practiceStore.questions"
                :key="q.id"
                class="nav-cell"
                :class="navCellClass(q, idx)"
                :title="`第${idx + 1}题`"
                @click="currentIndex = idx"
              >{{ idx + 1 }}</div>
            </div>

            <el-button
              type="danger"
              class="submit-all-btn"
              @click="handleSubmitConfirm"
              :loading="practiceStore.loading"
            >
              <el-icon><Check /></el-icon>
              提交答卷
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 提交确认对话框 -->
    <el-dialog v-model="submitDialogVisible" title="确认提交" width="420px" align-center>
      <div class="submit-confirm">
        <div class="submit-stats">
          <div class="submit-stat-item">
            <div class="submit-stat-num answered">{{ practiceStore.answeredCount }}</div>
            <div class="submit-stat-lbl">已作答</div>
          </div>
          <div class="submit-stat-item">
            <div class="submit-stat-num unanswered">{{ practiceStore.totalCount - practiceStore.answeredCount }}</div>
            <div class="submit-stat-lbl">未作答</div>
          </div>
          <div class="submit-stat-item">
            <div class="submit-stat-num marked">{{ practiceStore.markedQuestions.size }}</div>
            <div class="submit-stat-lbl">已标记</div>
          </div>
        </div>
        <el-alert
          v-if="practiceStore.totalCount - practiceStore.answeredCount > 0"
          type="warning"
          :closable="false"
          show-icon
          title="还有未作答的题目，提交后将计为未答"
          style="margin-top: 16px"
        />
      </div>
      <template #footer>
        <el-button @click="submitDialogVisible = false">继续作答</el-button>
        <el-button type="primary" :loading="practiceStore.loading" @click="handleSubmit">
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Timer, Flag, ArrowLeft, ArrowRight, Check, InfoFilled, CircleCheck, CircleClose, EditPen } from '@element-plus/icons-vue'
import { usePracticeStore } from '@/store/practice'

const router = useRouter()
const route = useRoute()
const practiceStore = usePracticeStore()

const currentIndex = ref(0)
const submitDialogVisible = ref(false)
const startTime = ref(Date.now())
let timerInterval = null

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

const currentQ = computed(() => practiceStore.questions[currentIndex.value] || null)

// 将后端返回的选项统一转为 { key, value } 格式
// 后端存储为纯字符串数组 ["选项A文本", "选项B文本", ...]
const currentOptions = computed(() => {
  const opts = currentQ.value?.options
  if (!opts || !Array.isArray(opts)) return []
  return opts.map((opt, i) => {
    if (typeof opt === 'string') {
      return { key: String.fromCharCode(65 + i), value: opt }
    }
    // 兼容对象格式 { key, value } 或 { label, text }
    return {
      key: opt.key || opt.label || String.fromCharCode(65 + i),
      value: opt.value || opt.text || opt.content || String(opt)
    }
  })
})

const currentAnswer = computed({
  get() {
    if (!currentQ.value) return null
    const ans = practiceStore.userAnswers[currentQ.value.id]
    if (currentQ.value.type === 'multiple') return ans || []
    return ans ?? null
  },
  set(val) {
    if (currentQ.value) practiceStore.saveAnswer(currentQ.value.id, val)
  }
})

// 填空/简答题的输入框双向绑定
const fillAnswer = computed({
  get() {
    if (!currentQ.value) return ''
    return practiceStore.userAnswers[currentQ.value.id] ?? ''
  },
  set(val) {
    if (currentQ.value) practiceStore.saveAnswer(currentQ.value.id, val)
  }
})

function onFillInput(val) {
  if (currentQ.value) practiceStore.saveAnswer(currentQ.value.id, val)
}


const timeLeft = computed(() => practiceStore.timeLeft)

const timerDisplay = computed(() => {
  if (practiceStore.timeLimit === 0) return '不限时'
  const t = Math.max(0, timeLeft.value)
  const m = Math.floor(t / 60).toString().padStart(2, '0')
  const s = (t % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const timerClass = computed(() => {
  if (practiceStore.timeLimit === 0) return 'timer--normal'
  if (timeLeft.value <= 60) return 'timer--danger'
  if (timeLeft.value <= 300) return 'timer--warning'
  return 'timer--normal'
})

const progressColor = computed(() => {
  const pct = practiceStore.answeredCount / practiceStore.totalCount
  if (pct >= 0.8) return '#67c23a'
  if (pct >= 0.5) return '#409eff'
  return '#e6a23c'
})

const progressTagType = computed(() => {
  const pct = practiceStore.answeredCount / practiceStore.totalCount
  if (pct >= 1) return 'success'
  if (pct >= 0.5) return ''
  return 'warning'
})

function selectSingle(key) {
  if (currentQ.value) practiceStore.saveAnswer(currentQ.value.id, key)
}

function toggleMulti(key) {
  if (!currentQ.value) return
  const current = Array.isArray(currentAnswer.value) ? [...currentAnswer.value] : []
  const idx = current.indexOf(key)
  if (idx === -1) current.push(key)
  else current.splice(idx, 1)
  practiceStore.saveAnswer(currentQ.value.id, current)
}

function toggleMark() {
  if (currentQ.value) practiceStore.toggleMark(currentQ.value.id)
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < practiceStore.totalCount - 1) currentIndex.value++
}

function handleKeydown(e) {
  if (submitDialogVisible.value) return
  switch (e.key) {
    case 'ArrowLeft':
      e.preventDefault()
      prevQuestion()
      break
    case 'ArrowRight':
      e.preventDefault()
      nextQuestion()
      break
    case 'Enter':
      if (currentIndex.value === practiceStore.totalCount - 1) handleSubmitConfirm()
      else nextQuestion()
      break
  }
  // 数字键 1-9 快速选择选项
  if (/^[1-9]$/.test(e.key) && currentQ.value) {
    const opts = currentOptions.value
    if (!opts || opts.length === 0) return
    const idx = parseInt(e.key) - 1
    if (idx < opts.length) {
      const key = opts[idx].key
      if (currentQ.value.type === 'single' || currentQ.value.type === 'judge') {
        selectSingle(key)
      } else if (currentQ.value.type === 'multiple') {
        toggleMulti(key)
      }
    }
  }
}

function navCellClass(q, idx) {
  const ans = practiceStore.userAnswers[q.id]
  const answered = ans !== null && ans !== undefined && ans !== '' && !(Array.isArray(ans) && ans.length === 0)
  return {
    'nav-cell--current': idx === currentIndex.value,
    'nav-cell--answered': answered && idx !== currentIndex.value,
    'nav-cell--marked': practiceStore.markedQuestions.has(q.id)
  }
}

function handleSubmitConfirm() {
  submitDialogVisible.value = true
}

async function handleSubmit() {
  submitDialogVisible.value = false
  const timeUsed = Math.floor((Date.now() - startTime.value) / 1000)
  try {
    await practiceStore.submit(timeUsed)
    router.replace(`/user/practice/result/${practiceStore.sessionId}`)
  } catch (e) {
    ElMessage.error(e?.message || '提交失败，请重试')
  }
}

function startTimer() {
  if (practiceStore.timeLimit <= 0) return
  timerInterval = setInterval(() => {
    if (practiceStore.timeLeft <= 1) {
      clearInterval(timerInterval)
      ElMessage.warning('时间到，自动提交！')
      handleSubmit()
    } else {
      practiceStore.timeLeft--
      practiceStore.saveDraft()
    }
  }, 1000)
}

onMounted(async () => {
  const sessionId = route.params.sessionId
  if (practiceStore.sessionId !== sessionId || !practiceStore.questions.length) {
    const restored = practiceStore.loadDraft()
    if (!restored || practiceStore.sessionId !== sessionId) {
      try {
        await practiceStore.loadSession(sessionId)
      } catch {
        ElMessage.error('加载答题数据失败')
        router.replace('/user/practice/history')
        return
      }
    }
  }
  if (practiceStore.isSubmitted) {
    router.replace(`/user/practice/result/${sessionId}`)
    return
  }
  startTime.value = Date.now()
  startTimer()
})

onUnmounted(() => {
  clearInterval(timerInterval)
})
</script>

<style lang="scss" scoped>
.practice-exam-page {
  padding: 4px 0;
  outline: none;
}

.loading-wrap {
  padding: 40px;
}

// ── 顶部进度条 ──────────────────────────────────────────
.exam-progress-bar {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  height: 3px;
  background: #edf0f5;
  z-index: 100;

  .exam-progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #409eff, #67c23a);
    transition: width 0.4s ease;
    border-radius: 0 2px 2px 0;
  }
}

// ── 顶部信息栏 ──────────────────────────────────────────
.top-bar-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;

  :deep(.el-card__body) { padding: 12px 20px; }
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.q-index {
  font-size: 15px;
  font-weight: 600;

  .q-current {
    font-size: 20px;
    color: #409eff;
  }

  .q-sep, .q-total {
    color: #909399;
    font-size: 14px;
  }
}

.progress-text {
  font-size: 13px;
  color: #909399;

  b { color: #409eff; }
}

.timer {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  padding: 4px 10px;
  border-radius: 20px;
  transition: all 0.3s;

  &--normal {
    color: #303133;
    background: #f7f9fc;
  }

  &--warning {
    color: #e6a23c;
    background: #fdf6ec;
  }

  &--danger {
    color: #f56c6c;
    background: #fef0f0;
    animation: pulse 1s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

// ── 题目卡片 ──────────────────────────────────────────
.question-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__body) { padding: 28px 24px; }
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 24px;
  font-weight: 500;

  .q-num {
    color: #409eff;
    font-weight: 700;
    margin-right: 6px;
  }
}

.multi-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #e6a23c;
  margin-bottom: 14px;
  padding: 6px 12px;
  background: #fdf6ec;
  border-radius: 8px;
  border: 1px solid #faecd8;
}

// ── 选项列表 ──────────────────────────────────────────
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border: 1.5px solid #edf0f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    background: #f5f9ff;
    transform: translateX(2px);
  }

  &--selected {
    border-color: #409eff;
    background: #ecf5ff;
  }
}

.option-badge {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  transition: all 0.15s;
  background: #fff;

  &--selected {
    background: #409eff;
    border-color: #409eff;
    color: #fff !important;
  }

  &--checkbox {
    border-radius: 6px;
  }
}

.option-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  padding-top: 3px;
}

// ── 判断题 ──────────────────────────────────────────
.judge-options {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.judge-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  border: 2px solid #edf0f5;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  color: #909399;
  transition: all 0.15s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    color: #409eff;
    background: #f5f9ff;
  }

  &--selected.judge-btn--correct {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #67c23a;
  }

  &--selected.judge-btn--wrong {
    border-color: #f56c6c;
    background: #fef0f0;
    color: #f56c6c;
  }
}

// ── 填空 / 简答 ───────────────────────────────────────
.fill-area,
.essay-area {
  margin-top: 16px;
}

.fill-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

// ── 翻页按钮 ──────────────────────────────────────────
.nav-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}

.keyboard-hint {
  font-size: 12px;
  color: #b1b8c4;
}

// ── 题目切换动画 ──────────────────────────────────────
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

// ── 右侧答题卡 ──────────────────────────────────────────
.nav-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  position: sticky;
  top: 80px;

  :deep(.el-card__header) {
    padding: 12px 16px;
    background: #fafbfd;
    border-bottom: 1px solid #edf0f5;
  }
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-progress-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  padding: 8px 0;
}

.nav-progress-labels {
  flex: 1;
  min-width: 0;
}

.nav-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 12px;
  color: #909399;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;

  &--answered { background: #409eff; }
  &--unanswered { background: #f7f9fc; border: 1px solid #ccc; }
  &--marked { background: #e6a23c; }
  &--current { background: #67c23a; }
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  margin-bottom: 16px;
}

.nav-cell {
  height: 32px;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: #f7f9fc;
  border: 1.5px solid #edf0f5;
  color: #303133;
  transition: all 0.15s;
  overflow: visible;

  &:hover {
    border-color: #409eff;
    color: #409eff;
    transform: scale(1.05);
  }

  &--current {
    background: #67c23a;
    border-color: #67c23a;
    color: #fff !important;
    box-shadow: 0 2px 8px rgba(103, 194, 58, 0.4);
  }

  &--answered {
    background: #409eff;
    border-color: #409eff;
    color: #fff !important;
  }

  &--marked {
    background: #e6a23c;
    border-color: #e6a23c;
    color: #fff !important;
  }
}

.submit-all-btn {
  width: 100%;
  border-radius: 8px;
  font-weight: 600;
}

// ── 提交确认弹窗 ──────────────────────────────────────
.submit-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
}

.submit-stat-item {
  text-align: center;
}

.submit-stat-num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 6px;

  &.answered { color: #409eff; }
  &.unanswered { color: #b1b8c4; }
  &.marked { color: #e6a23c; }
}

.submit-stat-lbl {
  font-size: 13px;
  color: #909399;
}
</style>
