<template>
  <div class="detail-page">
    <div class="page-back">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回错题本
      </el-button>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="detail">
      <el-row :gutter="20">
        <!-- 左栏：题目内容 + 答案对比 -->
        <el-col :span="16">
          <!-- 题目卡片 -->
          <el-card shadow="never" class="question-card">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <span class="header-dot" style="background: #f56c6c"></span>
                  <span class="header-title">题目详情</span>
                </div>
                <div class="header-tags">
                  <el-tag size="small" :type="typeTagMap[detail.type]?.type">{{ typeTagMap[detail.type]?.label }}</el-tag>
                  <el-tag size="small" :type="diffTagMap[detail.difficulty]?.type">{{ diffTagMap[detail.difficulty]?.label }}</el-tag>
                  <el-tag v-if="detail.mastered" size="small" type="success">已掌握</el-tag>
                </div>
              </div>
            </template>

            <div class="question-content">{{ detail.content }}</div>

            <!-- 单选/多选选项 -->
            <div v-if="detail.options && detail.options.length" class="options-list">
              <div
                v-for="opt in normalizeOptions(detail.options)"
                :key="opt.key"
                class="option-item"
                :class="optionClass(opt.key)"
              >
                <span class="opt-badge" :class="optBadgeClass(opt.key)">{{ opt.key }}</span>
                <span class="opt-text">{{ opt.value }}</span>
                <span class="opt-mark">
                  <el-icon v-if="isCorrectOption(opt.key)" color="#67c23a"><Check /></el-icon>
                </span>
              </div>
            </div>

            <!-- 判断题 -->
            <div v-else-if="detail.type === 'judge'" class="judge-options">
              <div
                class="judge-opt"
                :class="{
                  'judge-opt--correct': String(detail.correctAnswer) === 'true',
                  'judge-opt--user': detail.type === 'judge'
                }"
              >
                <el-icon><CircleCheck /></el-icon> 正确
                <el-icon v-if="String(detail.correctAnswer) === 'true'" color="#67c23a" style="margin-left: 4px"><Check /></el-icon>
              </div>
              <div
                class="judge-opt"
                :class="{ 'judge-opt--correct': String(detail.correctAnswer) === 'false' }"
              >
                <el-icon><CircleClose /></el-icon> 错误
                <el-icon v-if="String(detail.correctAnswer) === 'false'" color="#67c23a" style="margin-left: 4px"><Check /></el-icon>
              </div>
            </div>

            <!-- 填空/简答 -->
            <div v-else-if="detail.type === 'fill' || detail.type === 'essay'" class="text-answer">
              <div class="answer-label">正确答案：</div>
              <div class="answer-value">{{ formatAnswer(detail.correctAnswer) }}</div>
            </div>

            <!-- 解析 -->
            <div v-if="detail.analysis" class="analysis-block">
              <div class="analysis-title">
                <el-icon><Promotion /></el-icon> 题目解析
              </div>
              <div class="analysis-content">{{ detail.analysis }}</div>
            </div>
          </el-card>

          <!-- 错误统计卡片 -->
          <el-card shadow="never" class="wrong-stats-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #e6a23c"></span>
                <span class="header-title">答题记录</span>
              </div>
            </template>
            <div class="wrong-stats-row">
              <div class="wrong-stat-item">
                <div class="wrong-stat-num" style="color: #f56c6c">{{ detail.wrongCount }}</div>
                <div class="wrong-stat-label">累计答错</div>
              </div>
              <div class="wrong-stat-item">
                <div class="wrong-stat-num" style="color: #409eff">{{ detail.reviewCount || 0 }}</div>
                <div class="wrong-stat-label">已复习</div>
              </div>
              <div class="wrong-stat-item">
                <div class="wrong-stat-num" style="color: #909399">
                  {{ detail.lastWrongAt ? formatDate(detail.lastWrongAt) : '—' }}
                </div>
                <div class="wrong-stat-label">最近答错</div>
              </div>
              <div class="wrong-stat-item">
                <div class="wrong-stat-num" style="color: #67c23a">
                  {{ detail.nextReviewAt ? formatDate(detail.nextReviewAt) : '待安排' }}
                </div>
                <div class="wrong-stat-label">下次复习</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右栏：操作区 -->
        <el-col :span="8">
          <!-- 操作卡片 -->
          <el-card shadow="never" class="action-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <span class="header-title">操作</span>
              </div>
            </template>

            <div class="action-list">
              <!-- 收藏 -->
              <div class="action-item" @click="handleToggleStarred">
                <el-icon :class="detail.starred ? 'icon-starred' : 'icon-normal'"><Star /></el-icon>
                <span>{{ detail.starred ? '取消收藏' : '收藏题目' }}</span>
                <el-tag v-if="detail.starred" size="small" type="warning">已收藏</el-tag>
              </div>

              <!-- 掌握 -->
              <div class="action-item" @click="handleToggleMastered">
                <el-icon :class="detail.mastered ? 'icon-mastered' : 'icon-normal'"><CircleCheck /></el-icon>
                <span>{{ detail.mastered ? '取消掌握' : '标记已掌握' }}</span>
                <el-tag v-if="detail.mastered" size="small" type="success">已掌握</el-tag>
              </div>

              <!-- 单独练习 -->
              <div class="action-item" @click="practiceThis">
                <el-icon class="icon-normal"><VideoPlay /></el-icon>
                <span>单独练习</span>
              </div>
            </div>

            <!-- 错因标注 -->
            <el-divider style="margin: 16px 0" />
            <div class="reason-section">
              <div class="reason-title">错因标注</div>
              <el-radio-group v-model="selectedReason" @change="handleSetReason" class="reason-group">
                <el-radio value="">未标注</el-radio>
                <el-radio value="careless">粗心大意</el-radio>
                <el-radio value="unknown">不会做</el-radio>
                <el-radio value="concept">概念模糊</el-radio>
              </el-radio-group>
            </div>
          </el-card>

          <!-- 题库信息 -->
          <el-card v-if="detail.bankId" shadow="never" class="bank-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #909399"></span>
                <span class="header-title">所属题库</span>
              </div>
            </template>
            <div class="bank-info">
              <el-icon><Collection /></el-icon>
              <router-link :to="`/banks/${detail.bankId}`" class="bank-link">
                {{ detail.bankName || `题库 #${detail.bankId}` }}
              </router-link>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <div v-else class="not-found">
      <el-empty description="错题记录不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Check, CircleCheck, CircleClose, Star,
  VideoPlay, Promotion, Collection
} from '@element-plus/icons-vue'
import { useWrongBookStore } from '@/store/wrongBook'
import { usePracticeStore } from '@/store/practice'
import { setErrorReason } from '@/api/wrongBook'

const route = useRoute()
const router = useRouter()
const wrongBookStore = useWrongBookStore()
const practiceStore = usePracticeStore()

const loading = ref(false)
const detail = ref(null)
const selectedReason = ref('')

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

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

function isCorrectOption(key) {
  const ans = detail.value?.correctAnswer
  if (!ans) return false
  if (Array.isArray(ans)) return ans.map(String).includes(String(key))
  return String(ans) === String(key)
}

function optionClass(key) {
  return { 'option-item--correct': isCorrectOption(key) }
}

function optBadgeClass(key) {
  return { 'opt-badge--correct': isCorrectOption(key) }
}

async function handleToggleStarred() {
  if (!detail.value) return
  try {
    await wrongBookStore.toggleStarredById(detail.value.id)
    detail.value.starred = !detail.value.starred
    ElMessage.success(detail.value.starred ? '已收藏' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleToggleMastered() {
  if (!detail.value) return
  try {
    await wrongBookStore.toggleMasteredById(detail.value.id)
    detail.value.mastered = !detail.value.mastered
    ElMessage.success(detail.value.mastered ? '已标记为掌握' : '已取消掌握')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleSetReason(reason) {
  if (!detail.value) return
  try {
    await setErrorReason(detail.value.id, reason)
    detail.value.errorReason = reason
    ElMessage.success('错因已更新')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function practiceThis() {
  if (!detail.value) return
  try {
    const session = await practiceStore.startSession({
      questionIds: [detail.value.questionId],
      timeLimit: 0,
      shuffle: false
    })
    router.push(`/user/practice/exam/${session.id}`)
  } catch (e) {
    ElMessage.error(e?.message || '创建练习失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await wrongBookStore.fetchDetail(route.params.id)
    detail.value = data
    selectedReason.value = data.errorReason || ''
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.detail-page {
  padding: 4px 0;
}

.page-back {
  margin-bottom: 16px;
}

.loading-wrap {
  padding: 20px;
}

.not-found {
  padding: 60px 0;
  text-align: center;
}

// ── 卡片通用 ──────────────────────────────────────────
.question-card,
.wrong-stats-card,
.action-card,
.bank-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;

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

.header-tags {
  display: flex;
  gap: 6px;
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

// ── 题目内容 ──────────────────────────────────────────
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

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1.5px solid #edf0f5;
  background: #fafbfd;
  font-size: 14px;
  color: #606266;
  transition: border-color 0.15s;

  &--correct {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #303133;
  }
}

.opt-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  background: #edf0f5;
  color: #606266;

  &--correct {
    background: #67c23a;
    color: #fff;
  }
}

.opt-text {
  flex: 1;
}

.opt-mark {
  flex-shrink: 0;
}

// ── 判断题 ────────────────────────────────────────────
.judge-options {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.judge-opt {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  border: 1.5px solid #edf0f5;
  font-size: 14px;
  color: #606266;
  cursor: default;

  &--correct {
    border-color: #67c23a;
    background: #f0f9eb;
    color: #67c23a;
    font-weight: 600;
  }
}

// ── 填空/简答答案 ──────────────────────────────────────
.text-answer {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f0f9eb;
  border-radius: 8px;
  border: 1px solid #b3e19d;
}

.answer-label {
  font-size: 13px;
  color: #67c23a;
  font-weight: 600;
  margin-bottom: 6px;
}

.answer-value {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
}

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

// ── 答题记录统计 ──────────────────────────────────────
.wrong-stats-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.wrong-stat-item {
  padding: 8px 0;
}

.wrong-stat-num {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.wrong-stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

// ── 操作卡片 ──────────────────────────────────────────
.action-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  transition: background 0.15s;

  &:hover { background: #f5f7fa; }

  .el-icon { font-size: 18px; }
}

.icon-normal { color: #c0c4cc; }
.icon-starred { color: #e6a23c; }
.icon-mastered { color: #67c23a; }

// ── 错因 ──────────────────────────────────────────────
.reason-section {
  padding: 0 4px;
}

.reason-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.reason-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

// ── 题库信息 ──────────────────────────────────────────
.bank-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.bank-link {
  color: #409eff;
  text-decoration: none;

  &:hover { text-decoration: underline; }
}
</style>
