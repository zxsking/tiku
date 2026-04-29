<template>
  <div class="practice-result-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="resultData">
      <!-- 得分概览卡片 -->
      <el-card shadow="never" class="score-card">
        <div class="score-overview">
          <!-- 左：环形得分 + 评级 -->
          <div class="score-left">
            <div class="score-circle-wrap">
              <el-progress
                type="circle"
                :percentage="Math.round((resultData.accuracy || 0) * 100)"
                :width="140"
                :stroke-width="12"
                :color="scoreColor"
              >
                <template #default>
                  <div class="score-inner">
                    <div class="score-num" :style="{ color: scoreColor }">{{ resultData.score }}</div>
                    <div class="score-total">/ {{ resultData.totalScore }}</div>
                    <div class="score-pct">{{ Math.round((resultData.accuracy || 0) * 100) }}%</div>
                  </div>
                </template>
              </el-progress>
            </div>
            <div class="score-grade" :style="{ color: gradeInfo.color, background: gradeInfo.bg }">
              {{ gradeInfo.label }}
            </div>
          </div>

          <!-- 中：统计数字 -->
          <div class="score-stats">
            <div class="stat-block stat-block--correct">
              <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
              <div class="stat-val">{{ resultData.correctCount }}</div>
              <div class="stat-lbl">答对</div>
            </div>
            <div class="stat-block stat-block--wrong">
              <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
              <div class="stat-val">{{ resultData.wrongCount }}</div>
              <div class="stat-lbl">答错</div>
            </div>
            <div class="stat-block stat-block--skip">
              <div class="stat-icon"><el-icon><QuestionFilled /></el-icon></div>
              <div class="stat-val">{{ resultData.skipCount }}</div>
              <div class="stat-lbl">未答</div>
            </div>
            <div class="stat-block stat-block--time">
              <div class="stat-icon"><el-icon><Timer /></el-icon></div>
              <div class="stat-val">{{ formatTime(resultData.timeUsed) }}</div>
              <div class="stat-lbl">用时</div>
            </div>
          </div>

          <!-- 右：操作按钮 -->
          <div class="score-actions">
            <el-button type="primary" size="large" @click="practiceAgain">
              <el-icon><RefreshRight /></el-icon>再练一次
            </el-button>
            <el-button size="large" @click="$router.push('/user/wrong-book')">
              <el-icon><WarningFilled /></el-icon>查看错题本
            </el-button>
            <el-button size="large" @click="$router.push('/user/practice/history')">
              <el-icon><List /></el-icon>历史记录
            </el-button>
          </div>
        </div>

        <!-- 底部简易分布条 -->
        <div class="score-bar-wrap">
          <div class="score-bar">
            <div
              class="score-bar-seg seg--correct"
              :style="{ width: `${correctPct}%` }"
              :title="`答对 ${resultData.correctCount} 题`"
            ></div>
            <div
              class="score-bar-seg seg--wrong"
              :style="{ width: `${wrongPct}%` }"
              :title="`答错 ${resultData.wrongCount} 题`"
            ></div>
            <div
              class="score-bar-seg seg--skip"
              :style="{ width: `${skipPct}%` }"
              :title="`未答 ${resultData.skipCount} 题`"
            ></div>
          </div>
          <div class="score-bar-legend">
            <span><i class="bar-dot bar-dot--correct"></i>答对 {{ resultData.correctCount }}</span>
            <span><i class="bar-dot bar-dot--wrong"></i>答错 {{ resultData.wrongCount }}</span>
            <span><i class="bar-dot bar-dot--skip"></i>未答 {{ resultData.skipCount }}</span>
          </div>
        </div>
      </el-card>

      <!-- 筛选栏 -->
      <div class="result-filter">
        <el-radio-group v-model="filterMode" size="default">
          <el-radio-button value="all">
            全部 <el-badge :value="resultData.questions?.length" type="info" />
          </el-radio-button>
          <el-radio-button value="wrong">
            答错 <el-badge :value="resultData.wrongCount" type="danger" />
          </el-radio-button>
          <el-radio-button value="correct">
            答对 <el-badge :value="resultData.correctCount" type="success" />
          </el-radio-button>
          <el-radio-button value="skip">
            未答 <el-badge :value="resultData.skipCount" />
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 题目解析列表 -->
      <div class="questions-review">
        <TransitionGroup name="review-list">
          <el-card
            v-for="item in filteredQuestions"
            :key="item.id"
            shadow="never"
            class="review-card"
            :class="reviewCardClass(item)"
          >
            <div class="review-header">
              <div class="review-index">
                <div class="review-status-icon" :class="reviewStatusClass(item)">
                  <el-icon v-if="isEssayType(item)"><EditPen /></el-icon>
                  <el-icon v-else-if="item.isCorrect"><Check /></el-icon>
                  <el-icon v-else-if="!isSkipped(item)"><Close /></el-icon>
                  <el-icon v-else><Minus /></el-icon>
                </div>
                <span>第 {{ getOriginalIndex(item) + 1 }} 题</span>
              </div>
              <div class="review-tags">
                <el-tag size="small" :type="typeTagMap[item.type]?.type">{{ typeTagMap[item.type]?.label }}</el-tag>
                <el-tag size="small" :type="diffTagMap[item.difficulty]?.type">{{ diffTagMap[item.difficulty]?.label }}</el-tag>
              </div>
            </div>

            <div class="review-content">{{ item.content }}</div>

            <!-- 选项高亮展示（单选/多选） -->
            <div v-if="item.options && item.options.length" class="review-options">
              <div
                v-for="opt in normalizeOptions(item.options)"
                :key="opt.key"
                class="review-option"
                :class="reviewOptionClass(item, opt.key)"
              >
                <span class="review-opt-badge" :class="reviewOptBadgeClass(item, opt.key)">
                  {{ opt.key }}
                </span>
                <span class="review-opt-text">{{ opt.value }}</span>
                <span class="review-opt-mark">
                  <el-icon v-if="isCorrectOption(item, opt.key)" color="#67c23a"><Check /></el-icon>
                  <el-icon v-else-if="isUserWrongOption(item, opt.key)" color="#f56c6c"><Close /></el-icon>
                </span>
              </div>
            </div>

            <!-- 判断题答案展示 -->
            <div v-else-if="item.type === 'judge'" class="review-judge">
              <div
                class="review-judge-opt"
                :class="{
                  'review-judge-opt--correct-answer': item.correctAnswer === 'true',
                  'review-judge-opt--user-correct': item.userAnswer === 'true' && item.isCorrect,
                  'review-judge-opt--user-wrong': item.userAnswer === 'true' && !item.isCorrect
                }"
              >
                <el-icon><CircleCheck /></el-icon> 正确
              </div>
              <div
                class="review-judge-opt"
                :class="{
                  'review-judge-opt--correct-answer': item.correctAnswer === 'false',
                  'review-judge-opt--user-correct': item.userAnswer === 'false' && item.isCorrect,
                  'review-judge-opt--user-wrong': item.userAnswer === 'false' && !item.isCorrect
                }"
              >
                <el-icon><CircleClose /></el-icon> 错误
              </div>
            </div>

            <!-- 简答题：展示作答内容，不判对错 -->
            <div v-else-if="item.type === 'essay'" class="essay-review">
              <div class="essay-label">
                <el-icon><EditPen /></el-icon> 你的作答
              </div>
              <div class="essay-content" :class="{ 'essay-empty': !item.userAnswer }">
                {{ item.userAnswer || '（未作答）' }}
              </div>
              <div v-if="item.correctAnswer" class="essay-reference">
                <div class="essay-label essay-label--ref">
                  <el-icon><Promotion /></el-icon> 参考答案
                </div>
                <div class="essay-content">{{ formatAnswer(item.correctAnswer) }}</div>
              </div>
            </div>

            <!-- 填空题：展示答案对比 -->
            <div v-else-if="item.type === 'fill'" class="answer-compare">
              <div class="answer-row">
                <span class="answer-label">你的答案：</span>
                <span :class="item.isCorrect ? 'answer-correct' : 'answer-wrong'">
                  {{ formatAnswer(item.userAnswer) || '未作答' }}
                </span>
              </div>
              <div class="answer-row">
                <span class="answer-label">正确答案：</span>
                <span class="answer-correct">{{ formatAnswer(item.correctAnswer) }}</span>
              </div>
            </div>

            <!-- 其他无选项题型（兜底） -->
            <div v-else class="answer-compare">
              <div class="answer-row">
                <span class="answer-label">你的答案：</span>
                <span :class="item.isCorrect ? 'answer-correct' : 'answer-wrong'">
                  {{ formatAnswer(item.userAnswer) || '未作答' }}
                </span>
              </div>
              <div class="answer-row" v-if="!item.isCorrect">
                <span class="answer-label">正确答案：</span>
                <span class="answer-correct">{{ formatAnswer(item.correctAnswer) }}</span>
              </div>
            </div>

            <!-- 解析 -->
            <div v-if="item.analysis" class="analysis-block">
              <div class="analysis-title">
                <el-icon><Promotion /></el-icon>题目解析
              </div>
              <div class="analysis-content">{{ item.analysis }}</div>
            </div>
          </el-card>
        </TransitionGroup>

        <div v-if="filteredQuestions.length === 0" class="empty-state">
          <el-empty description="没有符合条件的题目" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  RefreshRight, List, CircleCheck, CircleClose,
  QuestionFilled, Timer, Check, Close, Minus,
  WarningFilled, Promotion, EditPen
} from '@element-plus/icons-vue'
import { usePracticeStore } from '@/store/practice'
import { getSession } from '@/api/practice'

const router = useRouter()
const route = useRoute()
const practiceStore = usePracticeStore()

const loading = ref(false)
const resultData = ref(null)
const filterMode = ref('all')

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

const scoreColor = computed(() => {
  const acc = (resultData.value?.accuracy || 0) * 100
  if (acc >= 90) return '#67c23a'
  if (acc >= 75) return '#409eff'
  if (acc >= 60) return '#e6a23c'
  return '#f56c6c'
})

const gradeInfo = computed(() => {
  const acc = (resultData.value?.accuracy || 0) * 100
  if (acc >= 90) return { label: '优秀', color: '#67c23a', bg: '#f0f9eb' }
  if (acc >= 75) return { label: '良好', color: '#409eff', bg: '#ecf5ff' }
  if (acc >= 60) return { label: '及格', color: '#e6a23c', bg: '#fdf6ec' }
  return { label: '需加油', color: '#f56c6c', bg: '#fef0f0' }
})

const total = computed(() => resultData.value?.questions?.length || 1)
const correctPct = computed(() => ((resultData.value?.correctCount || 0) / total.value) * 100)
const wrongPct = computed(() => ((resultData.value?.wrongCount || 0) / total.value) * 100)
const skipPct = computed(() => ((resultData.value?.skipCount || 0) / total.value) * 100)

const filteredQuestions = computed(() => {
  const qs = resultData.value?.questions || []
  if (filterMode.value === 'wrong') return qs.filter(q => q.type !== 'essay' && !q.isCorrect && !isSkipped(q))
  if (filterMode.value === 'correct') return qs.filter(q => q.isCorrect)
  if (filterMode.value === 'skip') return qs.filter(q => isSkipped(q))
  return qs
})

function isSkipped(item) {
  const a = item.userAnswer
  return a === null || a === undefined || a === '' || (Array.isArray(a) && a.length === 0)
}

function getOriginalIndex(item) {
  return (resultData.value?.questions || []).indexOf(item)
}

function formatAnswer(answer) {
  if (answer === null || answer === undefined || answer === '') return ''
  if (Array.isArray(answer)) return answer.join('、')
  if (answer === 'true') return '正确'
  if (answer === 'false') return '错误'
  return String(answer)
}

function formatTime(seconds) {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m === 0) return `${s}秒`
  return `${m}分${s}秒`
}

function practiceAgain() {
  const bankId = resultData.value?.bankId
  router.push(bankId ? `/user/practice/start/${bankId}` : '/user/practice/start')
}

// ---- 选项格式规范化 ----
// 后端存储为纯字符串数组 ["选项A文本", ...]，统一转为 { key, value } 格式
function normalizeOptions(options) {
  if (!options || !Array.isArray(options)) return []
  return options.map((opt, i) => {
    if (typeof opt === 'string') {
      return { key: String.fromCharCode(65 + i), value: opt }
    }
    return {
      key: opt.key || opt.label || String.fromCharCode(65 + i),
      value: opt.value || opt.text || opt.content || String(opt)
    }
  })
}

// ---- 选项高亮逻辑 ----

function toSet(val) {
  if (!val) return new Set()
  if (Array.isArray(val)) return new Set(val.map(String))
  return new Set(String(val).split(',').map(s => s.trim()).filter(Boolean))
}

function isCorrectOption(item, key) {
  return toSet(item.correctAnswer).has(key)
}

function isUserWrongOption(item, key) {
  return toSet(item.userAnswer).has(key) && !toSet(item.correctAnswer).has(key)
}

function reviewOptionClass(item, key) {
  const correct = toSet(item.correctAnswer).has(key)
  const userSelected = toSet(item.userAnswer).has(key)
  if (correct) return 'review-option--correct'
  if (userSelected && !correct) return 'review-option--wrong'
  return ''
}

function reviewOptBadgeClass(item, key) {
  const correct = toSet(item.correctAnswer).has(key)
  const userSelected = toSet(item.userAnswer).has(key)
  if (correct) return 'review-opt-badge--correct'
  if (userSelected && !correct) return 'review-opt-badge--wrong'
  return ''
}

function isEssayType(item) {
  return item.type === 'essay'
}

function reviewCardClass(item) {
  if (isEssayType(item)) return 'review-card--essay'
  if (item.isCorrect) return 'review-card--correct'
  if (isSkipped(item)) return 'review-card--skip'
  return 'review-card--wrong'
}

function reviewStatusClass(item) {
  if (isEssayType(item)) return 'status-icon--essay'
  if (item.isCorrect) return 'status-icon--correct'
  if (isSkipped(item)) return 'status-icon--skip'
  return 'status-icon--wrong'
}

onMounted(async () => {
  const sessionId = route.params.sessionId
  if (practiceStore.sessionId === sessionId && practiceStore.result) {
    resultData.value = practiceStore.result
    return
  }
  loading.value = true
  try {
    const data = await getSession(sessionId)
    resultData.value = data.result || data
  } catch {
    ElMessage.error('加载结果失败')
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.practice-result-page {
  padding: 4px 0;
}

.loading-wrap {
  padding: 40px;
}

// ── 得分卡片 ──────────────────────────────────────────
.score-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;

  :deep(.el-card__body) { padding: 28px 24px 20px; }
}

.score-overview {
  display: flex;
  align-items: center;
  gap: 40px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.score-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.score-inner {
  text-align: center;

  .score-num {
    font-size: 30px;
    font-weight: 800;
    line-height: 1;
  }

  .score-total {
    font-size: 13px;
    color: #909399;
    margin: 2px 0;
  }

  .score-pct {
    font-size: 14px;
    font-weight: 600;
    color: #909399;
  }
}

.score-grade {
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
}

.score-stats {
  flex: 1;
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
  border-radius: 12px;
  min-width: 80px;

  .stat-icon {
    font-size: 22px;
  }

  .stat-val {
    font-size: 28px;
    font-weight: 800;
    line-height: 1;
  }

  .stat-lbl {
    font-size: 12px;
    color: #909399;
  }

  &--correct {
    background: #f0f9eb;
    .stat-icon, .stat-val { color: #67c23a; }
  }

  &--wrong {
    background: #fef0f0;
    .stat-icon, .stat-val { color: #f56c6c; }
  }

  &--skip {
    background: #f7f9fc;
    .stat-icon, .stat-val { color: #b1b8c4; }
  }

  &--time {
    background: #ecf5ff;
    .stat-icon, .stat-val { color: #409eff; font-size: 20px; }
  }
}

.score-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}

// ── 分布条 ──────────────────────────────────────────
.score-bar-wrap {
  border-top: 1px solid #edf0f5;
  padding-top: 16px;
}

.score-bar {
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
  background: #f7f9fc;
  display: flex;
  margin-bottom: 10px;
}

.score-bar-seg {
  height: 100%;
  transition: width 0.6s ease;

  &.seg--correct { background: #67c23a; }
  &.seg--wrong { background: #f56c6c; }
  &.seg--skip { background: #d0d3d9; }
}

.score-bar-legend {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}

.bar-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;

  &--correct { background: #67c23a; }
  &--wrong { background: #f56c6c; }
  &--skip { background: #d0d3d9; }
}

// ── 筛选栏 ──────────────────────────────────────────
.result-filter {
  margin-bottom: 16px;

  :deep(.el-badge__content) {
    transform: translateY(-2px) translateX(4px);
  }
}

// ── 题目解析列表 ──────────────────────────────────────
.questions-review {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-list-enter-active,
.review-list-leave-active {
  transition: all 0.2s ease;
}

.review-list-enter-from,
.review-list-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.review-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  border-left: 4px solid #edf0f5;

  &--correct { border-left-color: #67c23a; }
  &--wrong { border-left-color: #f56c6c; }
  &--skip { border-left-color: #d0d3d9; }
  &--essay { border-left-color: #909399; }

  :deep(.el-card__body) { padding: 20px; }
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.review-index {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.review-status-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;

  &--correct { background: #67c23a; }
  &--wrong { background: #f56c6c; }
  &--skip { background: #d0d3d9; }
  &--essay { background: #909399; }
}

.review-tags {
  display: flex;
  gap: 6px;
}

.review-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  margin-bottom: 16px;
  font-weight: 500;
}

// ── 选项高亮 ──────────────────────────────────────────
.review-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.review-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #edf0f5;
  background: #fff;
  font-size: 14px;

  &--correct {
    border-color: #67c23a;
    background: #f0f9eb;
  }

  &--wrong {
    border-color: #f56c6c;
    background: #fef0f0;
  }
}

.review-opt-badge {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid #edf0f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #909399;

  &--correct {
    background: #67c23a;
    border-color: #67c23a;
    color: #fff;
  }

  &--wrong {
    background: #f56c6c;
    border-color: #f56c6c;
    color: #fff;
  }
}

.review-opt-text {
  flex: 1;
  color: #303133;
}

.review-opt-mark {
  flex-shrink: 0;
  font-size: 16px;
}

// ── 判断题展示 ──────────────────────────────────────────
.review-judge {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.review-judge-opt {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #edf0f5;
  font-size: 15px;
  font-weight: 600;
  color: #909399;

  &--correct-answer {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #67c23a;
  }

  &--user-wrong {
    border-color: #f56c6c;
    background: #fef0f0;
    color: #f56c6c;
    text-decoration: line-through;
  }
}

// ── 答案对比 ──────────────────────────────────────────
.answer-compare {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f7f9fc;
  border-radius: 8px;
}

// ── 简答题展示 ────────────────────────────────────────
.essay-review {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.essay-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 8px 14px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;

  &--ref {
    background: #f0f9eb;
    color: #67c23a;
    border-top: 1px solid #e4e7ed;
  }
}

.essay-content {
  padding: 12px 14px;
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;

  &.essay-empty {
    color: #c0c4cc;
    font-style: italic;
  }
}

.essay-reference {
  border-top: 1px solid #e4e7ed;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.answer-label {
  color: #909399;
  flex-shrink: 0;
}

.answer-correct {
  color: #67c23a;
  font-weight: 600;
}

.answer-wrong {
  color: #f56c6c;
  font-weight: 600;
  text-decoration: line-through;
}

// ── 解析 ──────────────────────────────────────────
.analysis-block {
  padding: 14px 16px;
  background: linear-gradient(135deg, #fffbe6, #fff8d6);
  border-radius: 8px;
  border: 1px solid #ffe58f;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: #d48806;
  margin-bottom: 8px;
  font-size: 14px;
}

.analysis-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.empty-state {
  padding: 60px 0;
}
</style>
