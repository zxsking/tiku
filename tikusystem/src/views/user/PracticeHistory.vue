<template>
  <div class="practice-history-page">
    <div class="page-title">历史答题记录</div>

    <!-- 统计卡片行 -->
    <el-row :gutter="16" class="summary-row" v-if="summary.total > 0">
      <el-col :span="6">
        <div class="summary-card summary-card--total">
          <div class="summary-icon"><el-icon><Document /></el-icon></div>
          <div class="summary-val">{{ summary.total }}</div>
          <div class="summary-lbl">累计练习</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-card--accuracy">
          <div class="summary-icon"><el-icon><TrendCharts /></el-icon></div>
          <div class="summary-val">{{ summary.avgAccuracy }}%</div>
          <div class="summary-lbl">平均正确率</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-card--questions">
          <div class="summary-icon"><el-icon><EditPen /></el-icon></div>
          <div class="summary-val">{{ summary.totalQuestions }}</div>
          <div class="summary-lbl">累计答题</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="summary-card summary-card--time">
          <div class="summary-icon"><el-icon><Timer /></el-icon></div>
          <div class="summary-val">{{ summary.totalTimeStr }}</div>
          <div class="summary-lbl">累计用时</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">答题记录</span>
            <el-tag size="small" class="total-tag" type="info">共 {{ total }} 次</el-tag>
          </div>
          <el-button
            type="primary"
            size="small"
            @click="$router.push('/user/practice/start')"
          >
            <el-icon><VideoPlay /></el-icon>
            开始练习
          </el-button>
        </div>
      </template>

      <div v-if="loading && list.length === 0" class="loading-wrap">
        <el-skeleton :rows="6" animated />
      </div>

      <template v-else-if="list.length > 0">
        <div class="record-list">
          <div
            v-for="item in list"
            :key="item.id"
            class="record-item"
            @click="viewResult(item)"
          >
            <div class="record-left">
              <!-- 正确率环 -->
              <el-progress
                type="circle"
                :percentage="item.totalScore > 0 ? Math.round((item.score / item.totalScore) * 100) : 0"
                :width="56"
                :stroke-width="5"
                :color="scoreColor(item)"
              >
                <template #default>
                  <span class="record-pct" :style="{ color: scoreColor(item) }">
                    {{ item.totalScore > 0 ? Math.round((item.score / item.totalScore) * 100) : 0 }}%
                  </span>
                </template>
              </el-progress>
            </div>

            <div class="record-body">
              <div class="record-title">
                {{ item.bankName || '自定义练习' }}
                <el-tag
                  size="small"
                  :type="item.status === 1 ? 'success' : 'warning'"
                  style="margin-left: 8px"
                >
                  {{ item.status === 1 ? '已完成' : '进行中' }}
                </el-tag>
              </div>
              <div class="record-meta">
                <span>
                  <el-icon><EditPen /></el-icon>
                  {{ item.score }} / {{ item.totalScore }} 分
                </span>
                <span>
                  <el-icon><Timer /></el-icon>
                  {{ formatTime(item.timeUsed) }}
                </span>
                <span>
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(item.createdAt) }}
                </span>
              </div>
            </div>

            <div class="record-right">
              <el-button
                link
                :type="item.status === 1 ? 'primary' : 'warning'"
                size="small"
              >
                {{ item.status === 1 ? '查看详情' : '继续作答' }}
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-empty description="暂无答题记录">
          <el-button type="primary" @click="$router.push('/user/practice/start')">
            <el-icon><VideoPlay /></el-icon>
            开始第一次练习
          </el-button>
        </el-empty>
      </div>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, TrendCharts, EditPen, Timer, VideoPlay, Calendar, ArrowRight } from '@element-plus/icons-vue'
import { getSessions } from '@/api/practice'

const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 15

const summary = computed(() => {
  if (list.value.length === 0) return { total: 0 }
  const completed = list.value.filter(r => r.status === 1)
  const avgAcc = completed.length > 0
    ? Math.round(completed.reduce((s, r) => s + (r.totalScore > 0 ? r.score / r.totalScore : 0), 0) / completed.length * 100)
    : 0
  const totalQ = list.value.reduce((s, r) => s + (r.totalScore || 0), 0)
  const totalSec = list.value.reduce((s, r) => s + (r.timeUsed || 0), 0)
  return {
    total: total.value,
    avgAccuracy: avgAcc,
    totalQuestions: totalQ,
    totalTimeStr: formatTime(totalSec)
  }
})

function scoreColor(item) {
  if (item.totalScore === 0) return '#909399'
  const acc = item.score / item.totalScore
  if (acc >= 0.8) return '#67c23a'
  if (acc >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

function formatTime(seconds) {
  if (!seconds) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m === 0) return `${s}秒`
  if (m < 60) return `${m}分${s > 0 ? s + '秒' : ''}`
  const h = Math.floor(m / 60)
  return `${h}小时${m % 60}分`
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
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function viewResult(row) {
  if (row.status === 1) {
    router.push(`/user/practice/result/${row.id}`)
  } else {
    router.push(`/user/practice/exam/${row.id}`)
  }
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getSessions({ page: currentPage.value, size: pageSize })
    list.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.practice-history-page {
  padding: 4px 0;
}

.loading-wrap {
  padding: 20px;
}

// ── 统计卡片行 ──────────────────────────────────────────
.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 16px;
  border-radius: 12px;
  border: 1px solid #edf0f5;
  background: #fff;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }

  .summary-icon {
    font-size: 22px;
    margin-bottom: 2px;
  }

  .summary-val {
    font-size: 24px;
    font-weight: 800;
    line-height: 1;
  }

  .summary-lbl {
    font-size: 12px;
    color: #909399;
  }

  &--total {
    .summary-icon, .summary-val { color: #409eff; }
    background: linear-gradient(135deg, #ecf5ff, #fff);
  }

  &--accuracy {
    .summary-icon, .summary-val { color: #67c23a; }
    background: linear-gradient(135deg, #f0f9eb, #fff);
  }

  &--questions {
    .summary-icon, .summary-val { color: #e6a23c; }
    background: linear-gradient(135deg, #fdf6ec, #fff);
  }

  &--time {
    .summary-icon, .summary-val { color: #909399; }
    background: linear-gradient(135deg, #f7f9fc, #fff);
  }
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

// ── 记录列表 ──────────────────────────────────────────
.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1.5px solid #edf0f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    background: #f5f9ff;
    transform: translateX(3px);
  }
}

.record-left {
  flex-shrink: 0;
}

.record-pct {
  font-size: 12px;
  font-weight: 700;
}

.record-body {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #909399;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.record-right {
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}

.empty-state {
  padding: 60px 0;
}
</style>
