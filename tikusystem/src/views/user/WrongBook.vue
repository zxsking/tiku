<template>
  <div class="wrong-book-page">
    <div class="page-title">
      <el-icon><WarningFilled /></el-icon>
      错题本
    </div>

    <el-row :gutter="16">
      <!-- 左侧统计 -->
      <el-col :span="7">
        <el-card shadow="never" class="stats-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #f56c6c"></span>
              <span class="header-title">错题统计</span>
            </div>
          </template>

          <div v-if="statsLoading" class="loading-wrap">
            <el-skeleton :rows="5" animated />
          </div>

          <template v-else-if="wrongBookStore.stats && wrongBookStore.stats.total > 0">
            <!-- 总数大环 -->
            <div class="stats-total-ring">
              <el-progress
                type="circle"
                :percentage="100"
                :width="80"
                :stroke-width="8"
                color="#f56c6c"
              >
                <template #default>
                  <div class="ring-inner">
                    <div class="ring-num">{{ wrongBookStore.stats.total }}</div>
                    <div class="ring-lbl">道错题</div>
                  </div>
                </template>
              </el-progress>
            </div>

            <div class="stats-section">
              <div class="stats-section-title">按题型</div>
              <div v-for="item in wrongBookStore.stats.byType" :key="item.type" class="stats-item">
                <div class="stats-item-label">
                  <span class="stats-item-name">{{ typeLabel(item.type) }}</span>
                  <span class="stats-count" :style="{ color: typeColor(item.type) }">{{ item.count }}</span>
                </div>
                <el-progress
                  :percentage="Math.round((item.count / wrongBookStore.stats.total) * 100)"
                  :color="typeColor(item.type)"
                  :show-text="false"
                  :stroke-width="7"
                />
              </div>
            </div>

            <el-divider style="margin: 12px 0" />

            <div class="stats-section">
              <div class="stats-section-title">按难度</div>
              <div v-for="item in wrongBookStore.stats.byDifficulty" :key="item.difficulty" class="stats-item">
                <div class="stats-item-label">
                  <span class="stats-item-name">{{ diffLabel(item.difficulty) }}</span>
                  <span class="stats-count" :style="{ color: diffColor(item.difficulty) }">{{ item.count }}</span>
                </div>
                <el-progress
                  :percentage="Math.round((item.count / wrongBookStore.stats.total) * 100)"
                  :color="diffColor(item.difficulty)"
                  :show-text="false"
                  :stroke-width="7"
                />
              </div>
            </div>

            <!-- 高频错题题库 -->
            <template v-if="wrongBookStore.stats.byBank?.length">
              <el-divider style="margin: 12px 0" />
              <div class="stats-section">
                <div class="stats-section-title">高频错题来源</div>
                <div v-for="item in wrongBookStore.stats.byBank.slice(0, 3)" :key="item.bankId" class="bank-stat-item">
                  <span class="bank-stat-name">{{ item.bankName }}</span>
                  <el-badge :value="item.count" type="danger" />
                </div>
              </div>
            </template>

            <!-- 快捷入口 -->
            <el-divider style="margin: 12px 0" />
            <div class="quick-links">
              <el-button
                type="primary"
                plain
                size="small"
                style="width: 100%"
                @click="$router.push('/user/wrong-book/review')"
              >
                <el-icon><Refresh /></el-icon> 今日复习
                <el-badge
                  v-if="todayCount > 0"
                  :value="todayCount"
                  type="danger"
                  style="margin-left: 4px"
                />
              </el-button>
              <el-button
                plain
                size="small"
                style="width: 100%; margin-top: 8px; margin-left: 0"
                @click="$router.push('/user/wrong-book/stats')"
              >
                <el-icon><DataAnalysis /></el-icon> 统计面板
              </el-button>
            </div>
          </template>

          <div v-else class="stats-empty">
            <div class="stats-empty-icon">🎉</div>
            <div class="stats-empty-text">暂无错题记录</div>
            <div class="stats-empty-sub">继续保持，加油！</div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧错题列表 -->
      <el-col :span="17">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #f56c6c"></span>
                <span class="header-title">错题列表</span>
                <el-tag size="small" class="total-tag" type="info">共 {{ wrongBookStore.total }} 题</el-tag>
              </div>
              <div class="header-right">
                <el-button
                  v-if="selected.length > 0"
                  type="primary"
                  size="small"
                  @click="practiceSelected"
                  :loading="practiceStore.loading"
                >
                  <el-icon><VideoPlay /></el-icon>
                  练习选中 ({{ selected.length }})
                </el-button>
                <el-button
                  v-if="selected.length > 0"
                  type="danger"
                  size="small"
                  plain
                  @click="batchRemove"
                >
                  <el-icon><Delete /></el-icon>
                  批量移除
                </el-button>
              </div>
            </div>
          </template>

          <!-- 搜索 + 筛选栏 -->
          <div class="search-bar">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索题目内容..."
              clearable
              style="width: 220px"
              @keyup.enter="handleFilterChange"
              @clear="handleFilterChange"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="filters.type" placeholder="题目类型" clearable style="width: 120px" @change="handleFilterChange">
              <el-option label="单选题" value="single" />
              <el-option label="多选题" value="multiple" />
              <el-option label="判断题" value="judge" />
              <el-option label="填空题" value="fill" />
              <el-option label="简答题" value="essay" />
            </el-select>
            <el-select v-model="filters.difficulty" placeholder="难度" clearable style="width: 100px" @change="handleFilterChange">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
            <el-select v-model="filters.mastered" placeholder="掌握状态" clearable style="width: 120px" @change="handleFilterChange">
              <el-option label="未掌握" :value="false" />
              <el-option label="已掌握" :value="true" />
            </el-select>
            <el-select v-model="filters.starred" placeholder="收藏" clearable style="width: 100px" @change="handleFilterChange">
              <el-option label="已收藏" :value="true" />
            </el-select>
            <el-select v-model="filters.sort" placeholder="排序" clearable style="width: 120px" @change="handleFilterChange">
              <el-option label="最近答错" value="last_wrong" />
              <el-option label="错误次数" value="wrong_count" />
              <el-option label="待复习" value="next_review" />
            </el-select>
          </div>

          <!-- 全选栏 -->
          <div class="select-bar" v-if="wrongBookStore.list.length > 0">
            <el-checkbox
              :model-value="isAllSelected"
              :indeterminate="isIndeterminate"
              @change="toggleSelectAll"
            >全选</el-checkbox>
          </div>

          <div v-if="wrongBookStore.loading && wrongBookStore.list.length === 0" class="loading-wrap">
            <el-skeleton :rows="6" animated />
          </div>

          <template v-else-if="wrongBookStore.list.length > 0">
            <TransitionGroup name="wrong-list">
              <div
                v-for="item in wrongBookStore.list"
                :key="item.questionId"
                class="wrong-item"
                :class="{
                  'wrong-item--selected': selected.includes(item.questionId),
                  'wrong-item--expanded': expanded.has(item.questionId),
                  'wrong-item--mastered': item.mastered
                }"
              >
                <div class="wrong-item-main">
                  <div class="wrong-item-check" @click.stop>
                    <el-checkbox
                      :model-value="selected.includes(item.questionId)"
                      @change="toggleSelect(item.questionId)"
                    />
                  </div>
                  <div class="wrong-item-body" @click="goDetail(item)">
                    <div class="wrong-item-meta">
                      <el-tag size="small" :type="typeTagMap[item.type]?.type">{{ typeTagMap[item.type]?.label }}</el-tag>
                      <el-tag size="small" :type="diffTagMap[item.difficulty]?.type">{{ diffTagMap[item.difficulty]?.label }}</el-tag>
                      <span class="wrong-count-badge">
                        <el-icon><WarningFilled /></el-icon>
                        错 {{ item.wrongCount }} 次
                      </span>
                      <el-tag v-if="item.mastered" size="small" type="success">已掌握</el-tag>
                      <el-tag v-if="item.errorReason" size="small" type="info">{{ reasonLabel(item.errorReason) }}</el-tag>
                    </div>
                    <div class="wrong-item-content">{{ item.content }}</div>
                  </div>
                  <div class="wrong-item-right" @click.stop>
                    <div class="wrong-item-icons">
                      <!-- 收藏 -->
                      <el-icon
                        class="action-icon"
                        :class="{ 'action-icon--active': item.starred }"
                        :title="item.starred ? '取消收藏' : '收藏'"
                        @click="handleToggleStarred(item)"
                      >
                        <Star />
                      </el-icon>
                      <!-- 掌握 -->
                      <el-icon
                        class="action-icon"
                        :class="{ 'action-icon--mastered': item.mastered }"
                        :title="item.mastered ? '取消掌握' : '标记已掌握'"
                        @click="handleToggleMastered(item)"
                      >
                        <CircleCheck />
                      </el-icon>
                    </div>
                    <span class="wrong-time">{{ formatDate(item.lastWrongAt) }}</span>
                    <div class="wrong-item-actions">
                      <el-button link type="primary" size="small" @click="goDetail(item)">详情</el-button>
                      <el-button link size="small" @click="toggleExpand(item.questionId)">
                        {{ expanded.has(item.questionId) ? '收起' : '展开' }}
                      </el-button>
                      <el-button link type="danger" size="small" @click="removeItem(item)">移除</el-button>
                    </div>
                  </div>
                </div>

                <!-- 折叠展开的选项区 -->
                <Transition name="expand">
                  <div v-if="expanded.has(item.questionId)" class="wrong-item-expand">
                    <div v-if="item.options && item.options.length" class="expand-options">
                      <div v-for="opt in normalizeOptions(item.options)" :key="opt.key" class="expand-option">
                        <span class="expand-opt-key">{{ opt.key }}.</span>
                        <span>{{ opt.value }}</span>
                      </div>
                    </div>
                    <div v-else class="expand-no-options">（判断题或无选项）</div>
                    <el-button
                      type="primary"
                      size="small"
                      style="margin-top: 10px"
                      @click.stop="practiceOne(item)"
                    >
                      <el-icon><VideoPlay /></el-icon> 单独练习
                    </el-button>
                  </div>
                </Transition>
              </div>
            </TransitionGroup>
          </template>

          <div v-else class="empty-state">
            <el-empty description="错题本是空的，继续加油！">
              <el-button type="primary" @click="$router.push('/user/practice/start')">
                去练习
              </el-button>
            </el-empty>
          </div>

          <div class="pagination-wrapper" v-if="wrongBookStore.total > pageSize">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="wrongBookStore.total"
              layout="prev, pager, next"
              @current-change="fetchList"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  VideoPlay, WarningFilled, Delete, Search, Star, CircleCheck,
  Refresh, DataAnalysis
} from '@element-plus/icons-vue'
import { useWrongBookStore } from '@/store/wrongBook'
import { usePracticeStore } from '@/store/practice'

const router = useRouter()
const wrongBookStore = useWrongBookStore()
const practiceStore = usePracticeStore()

const selected = ref([])
const expanded = ref(new Set())
const currentPage = ref(1)
const pageSize = 15
const statsLoading = ref(false)
const todayCount = ref(0)

const filters = reactive({
  keyword: '',
  type: '',
  difficulty: '',
  mastered: null,
  starred: null,
  sort: ''
})

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

const reasonMap = {
  careless: '粗心',
  unknown: '不会',
  concept: '概念模糊'
}

const isAllSelected = computed(() =>
  wrongBookStore.list.length > 0 &&
  wrongBookStore.list.every(item => selected.value.includes(item.questionId))
)

const isIndeterminate = computed(() =>
  selected.value.length > 0 && !isAllSelected.value
)

function typeLabel(type) { return typeTagMap[type]?.label || type }
function typeColor(type) {
  return { single: '#409eff', multiple: '#e6a23c', judge: '#67c23a', fill: '#909399', essay: '#909399' }[type] || '#909399'
}
function diffLabel(diff) { return diffTagMap[diff]?.label || diff }
function diffColor(diff) {
  return { easy: '#67c23a', medium: '#e6a23c', hard: '#f56c6c' }[diff] || '#909399'
}
function reasonLabel(r) { return reasonMap[r] || r }

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

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function toggleSelect(questionId) {
  const idx = selected.value.indexOf(questionId)
  if (idx === -1) selected.value.push(questionId)
  else selected.value.splice(idx, 1)
}

function toggleSelectAll(val) {
  if (val) selected.value = wrongBookStore.list.map(item => item.questionId)
  else selected.value = []
}

function toggleExpand(questionId) {
  const s = new Set(expanded.value)
  if (s.has(questionId)) s.delete(questionId)
  else s.add(questionId)
  expanded.value = s
}

function goDetail(item) {
  router.push(`/user/wrong-book/detail/${item.id}`)
}

async function handleToggleMastered(item) {
  try {
    await wrongBookStore.toggleMasteredById(item.id)
    ElMessage.success(item.mastered ? '已取消掌握' : '已标记为掌握')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleToggleStarred(item) {
  try {
    await wrongBookStore.toggleStarredById(item.id)
    ElMessage.success(item.starred ? '已取消收藏' : '已收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm('确定从错题本中移除该题目？', '提示', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await wrongBookStore.remove(item.questionId)
    selected.value = selected.value.filter(id => id !== item.questionId)
    ElMessage.success('已移除')
    wrongBookStore.fetchStats()
  } catch {
    // 取消
  }
}

async function batchRemove() {
  if (selected.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定从错题本中移除选中的 ${selected.value.length} 道题目？`,
      '批量移除',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }
    )
    for (const qId of [...selected.value]) {
      await wrongBookStore.remove(qId)
    }
    selected.value = []
    ElMessage.success('批量移除成功')
    wrongBookStore.fetchStats()
  } catch {
    // 取消
  }
}

async function practiceSelected() {
  if (selected.value.length === 0) return
  try {
    const session = await practiceStore.startSession({
      questionIds: selected.value,
      timeLimit: 0,
      shuffle: true
    })
    router.push(`/user/practice/exam/${session.id}`)
  } catch (e) {
    ElMessage.error(e?.message || '创建练习失败')
  }
}

async function practiceOne(item) {
  try {
    const session = await practiceStore.startSession({
      questionIds: [item.questionId],
      timeLimit: 0,
      shuffle: false
    })
    router.push(`/user/practice/exam/${session.id}`)
  } catch (e) {
    ElMessage.error(e?.message || '创建练习失败')
  }
}

function handleFilterChange() {
  currentPage.value = 1
  fetchList()
}

async function fetchList() {
  const params = {
    page: currentPage.value,
    size: pageSize,
    type: filters.type || undefined,
    difficulty: filters.difficulty || undefined,
    keyword: filters.keyword || undefined,
    sort: filters.sort || undefined,
    mastered: filters.mastered !== null && filters.mastered !== undefined ? filters.mastered : undefined,
    starred: filters.starred !== null && filters.starred !== undefined ? filters.starred : undefined
  }
  await wrongBookStore.fetchList(params)
  selected.value = []
}

onMounted(async () => {
  fetchList()
  statsLoading.value = true
  try {
    await wrongBookStore.fetchStats()
    // 加载今日复习数量
    const todayList = await wrongBookStore.fetchTodayReview()
    todayCount.value = todayList.length
  } finally {
    statsLoading.value = false
  }
})
</script>

<style lang="scss" scoped>
.wrong-book-page {
  padding: 4px 0;
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

// ── 统计卡片 ──────────────────────────────────────────
.stats-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  position: sticky;
  top: 80px;

  :deep(.el-card__header) {
    padding: 16px 16px;
    background: #fafbfd;
    border-bottom: 1px solid #edf0f5;
  }

  :deep(.el-card__body) { padding: 16px; }
}

.stats-total-ring {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.ring-inner {
  text-align: center;

  .ring-num {
    font-size: 22px;
    font-weight: 800;
    color: #f56c6c;
    line-height: 1;
  }

  .ring-lbl {
    font-size: 11px;
    color: #909399;
    margin-top: 2px;
  }
}

.stats-section {
  margin-bottom: 4px;
}

.stats-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.stats-item {
  margin-bottom: 10px;
}

.stats-item-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}

.stats-item-name {
  font-weight: 500;
}

.stats-count {
  font-weight: 700;
  font-size: 14px;
}

.bank-stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #edf0f5;
  font-size: 13px;
  color: #606266;

  &:last-child { border-bottom: none; }
}

.bank-stat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}

.quick-links {
  margin-top: 4px;
}

.stats-empty {
  text-align: center;
  padding: 24px 0;

  .stats-empty-icon {
    font-size: 36px;
    margin-bottom: 8px;
  }

  .stats-empty-text {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 4px;
  }

  .stats-empty-sub {
    font-size: 13px;
    color: #909399;
  }
}

.loading-wrap {
  padding: 20px;
}

// ── 主卡片 ──────────────────────────────────────────
.main-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f5;
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 20px;
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

.header-right {
  display: flex;
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

// ── 搜索筛选栏 ──────────────────────────────────────
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px 14px;
  background: #f7f9fc;
  border-radius: 8px;
  border: 1px solid #edf0f5;
}

.select-bar {
  margin-bottom: 12px;
  padding: 0 4px;
}

// ── 错题列表 ──────────────────────────────────────────
.wrong-list-enter-active,
.wrong-list-leave-active {
  transition: all 0.2s ease;
}

.wrong-list-enter-from,
.wrong-list-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.wrong-item {
  border: 1.5px solid #edf0f5;
  border-radius: 8px;
  margin-bottom: 10px;
  transition: border-color 0.15s, box-shadow 0.15s;
  overflow: hidden;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  &--selected {
    border-color: #409eff;
    background: #f5f9ff;
  }

  &--mastered {
    border-color: #67c23a;
    opacity: 0.85;
  }
}

.wrong-item-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
}

.wrong-item-check {
  flex-shrink: 0;
  padding-top: 2px;
}

.wrong-item-body {
  flex: 1;
  min-width: 0;
  cursor: pointer;

  &:hover .wrong-item-content {
    color: #409eff;
  }
}

.wrong-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.wrong-count-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #fff;
  background: #f56c6c;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.wrong-item-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.15s;
}

.wrong-item-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.wrong-item-icons {
  display: flex;
  gap: 8px;
}

.action-icon {
  font-size: 16px;
  color: #c0c4cc;
  cursor: pointer;
  transition: color 0.15s, transform 0.15s;

  &:hover { color: #409eff; transform: scale(1.15); }
  &--active { color: #e6a23c; }
  &--mastered { color: #67c23a; }
}

.wrong-time {
  font-size: 12px;
  color: #b1b8c4;
  white-space: nowrap;
}

.wrong-item-actions {
  display: flex;
  gap: 2px;
}

// ── 折叠展开 ──────────────────────────────────────────
.expand-enter-active,
.expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 400px;
}

.wrong-item-expand {
  padding: 12px 16px 14px 52px;
  background: #f7f9fc;
  border-top: 1px solid #edf0f5;
}

.expand-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.expand-option {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.expand-opt-key {
  font-weight: 600;
  color: #409eff;
  margin-right: 4px;
}

.expand-no-options {
  font-size: 13px;
  color: #b1b8c4;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}

.empty-state {
  padding: 40px 0;
}
</style>
