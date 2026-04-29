<template>
  <div class="review-center-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #e6a23c"></span>
            <span class="header-title">审核中心</span>
          </div>
          <div class="header-right">
            <el-badge :value="pendingCount" :max="99" class="pending-badge">
              <button class="pending-btn">待审核</button>
            </el-badge>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="review-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="已通过" name="approved" />
      </el-tabs>

      <el-table v-loading="loading" :data="reviews" class="review-table" :row-class-name="() => 'review-row'">
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'bank' ? 'primary' : 'success'" size="small" class="type-tag">
              {{ row.type === 'bank' ? '题库' : '题目' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="220">
          <template #default="{ row }">
            <div class="review-title">{{ row.name || row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提交者" width="120">
          <template #default="{ row }">
            <span class="author-text">{{ row.author }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ row.submittedAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <template v-if="row.status === 'pending' || row.status === 'draft'">
              <button class="op-btn op-btn--success" @click="handleReview(row, 'approve')">通过</button>
              <el-popconfirm title="确认拒绝该内容？" @confirm="handleReview(row, 'reject')">
                <template #reference>
                  <button class="op-btn op-btn--danger">拒绝</button>
                </template>
              </el-popconfirm>
            </template>
            <template v-else-if="row.status === 'published'">
              <span class="review-result review-result--pass">已通过</span>
            </template>
            <template v-else>
              <span class="review-result">{{ getStatusName(row.status) }}</span>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          background
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReviewQuestions, getReviewBanks, reviewQuestion, reviewBank } from '@/api/admin'

const activeTab = ref('pending')
const pendingCount = ref(0)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const reviews = ref([])

function formatDate(value) {
  if (value === null || value === undefined || value === '') return '-'
  // 数组格式：[year, month, day, hour, minute, second, nano]
  if (Array.isArray(value)) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value
    return `${y}-${String(mo).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}:${String(s).padStart(2,'0')}`
  }
  // 对象格式（极少数情况）
  if (typeof value === 'object') return '-'
  const str = String(value)
  // ISO 格式：2024-01-01T12:00:00 或 2024-01-01T12:00:00.000
  if (str.includes('T')) return str.replace('T', ' ').slice(0, 19)
  // 已经是 yyyy-MM-dd HH:mm:ss 格式
  return str.slice(0, 19)
}

function mapTabToStatus(tab) {
  return tab === 'approved' ? 'published' : tab
}

function getStatusName(status) {
  return { pending: '待审核', draft: '待审核', published: '已通过', rejected: '已拒绝' }[status] || status
}

function getStatusType(status) {
  return { pending: 'warning', draft: 'warning', published: 'success', rejected: 'danger' }[status]
}

function normalizeReview(item, type) {
  return {
    id: `${type}-${item.id}`,
    entityId: item.id,
    type,
    name: type === 'bank' ? item.name : '',
    content: type === 'question' ? item.content : '',
    author: item.author || `用户#${item.authorId ?? '-'}`,
    submittedAt: formatDate(item.createdAt),
    createdAt: item.createdAt,
    status: item.status,
  }
}

async function loadPendingCount() {
  const [q1, q2, b1, b2] = await Promise.all([
    getReviewQuestions({ page: 1, size: 1, status: 'pending' }),
    getReviewQuestions({ page: 1, size: 1, status: 'draft' }),
    getReviewBanks({ page: 1, size: 1, status: 'pending' }),
    getReviewBanks({ page: 1, size: 1, status: 'draft' }),
  ])
  pendingCount.value = (q1.total || 0) + (q2.total || 0) + (b1.total || 0) + (b2.total || 0)
}

async function loadReviews() {
  loading.value = true
  try {
    const status = mapTabToStatus(activeTab.value)
    let questionList = []
    let bankList = []
    let questionTotal = 0
    let bankTotal = 0

    if (status === 'pending') {
      // pending 需合并 pending + draft 两种状态，各取半页
      const half = Math.ceil(pageSize / 2)
      const [qPending, qDraft, bPending, bDraft] = await Promise.all([
        getReviewQuestions({ page: page.value, size: half, status: 'pending' }),
        getReviewQuestions({ page: page.value, size: half, status: 'draft' }),
        getReviewBanks({ page: page.value, size: half, status: 'pending' }),
        getReviewBanks({ page: page.value, size: half, status: 'draft' }),
      ])
      questionList = [...(qPending.records || []), ...(qDraft.records || [])]
      bankList = [...(bPending.records || []), ...(bDraft.records || [])]
      questionTotal = (qPending.total || 0) + (qDraft.total || 0)
      bankTotal = (bPending.total || 0) + (bDraft.total || 0)
    } else {
      const [qData, bData] = await Promise.all([
        getReviewQuestions({ page: page.value, size: pageSize, status }),
        getReviewBanks({ page: page.value, size: pageSize, status }),
      ])
      questionList = qData.records || []
      bankList = bData.records || []
      questionTotal = qData.total || 0
      bankTotal = bData.total || 0
    }

    const merged = [
      ...questionList.map(item => normalizeReview(item, 'question')),
      ...bankList.map(item => normalizeReview(item, 'bank')),
    ].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))

    reviews.value = merged
    total.value = questionTotal + bankTotal
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  loadReviews()
}

function handlePageChange(p) {
  page.value = p
  loadReviews()
}

async function handleReview(row, action) {
  const status = action === 'approve' ? 'published' : 'rejected'
  if (row.type === 'question') {
    await reviewQuestion(row.entityId, status)
  } else {
    await reviewBank(row.entityId, status)
  }
  ElMessage.success(action === 'approve' ? '审核通过' : '已拒绝')
  await loadReviews()
  await loadPendingCount()
}

onMounted(async () => {
  await loadReviews()
  await loadPendingCount()
})
</script>

<style lang="scss" scoped>
.review-center-page {
  padding: 4px 0;
}

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

.header-right {
  .pending-btn {
    height: 32px;
    padding: 0 14px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    border: 1px solid #e6a23c;
    background: #fff9e6;
    color: #e6a23c;
    transition: all 0.15s;

    &:hover { background: #fff3cd; }
  }
}

.review-tabs {
  margin-bottom: 16px;

  :deep(.el-tabs__header) { margin: 0 0 16px; }
  :deep(.el-tabs__item) { font-size: 14px; font-weight: 500; }
  :deep(.el-tabs__item.is-active) { color: #409eff; font-weight: 600; }
}

.review-table {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header-wrapper th) {
    background: #f7f9fc;
    color: #909399;
    font-weight: 500;
    font-size: 13px;
  }

  :deep(.review-row td) { padding: 12px 0; }
  :deep(.el-table__row:hover > td) { background: #f7f9fc !important; }
}

.type-tag { border-radius: 5px; }

.review-title {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-text {
  font-size: 13px;
  color: #606266;
}

.time-text {
  font-size: 13px;
  color: #606266;
}

.review-result {
  font-size: 12px;
  color: #909399;
  &--pass   { color: #67c23a; }
  &--reject { color: #f56c6c; }
}

.op-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.15s;
  margin-right: 6px;

  &--success {
    background: #f0faf0;
    color: #67c23a;
    &:hover { background: #d9f0d9; }
  }
  &--danger {
    background: #fff2f2;
    color: #f56c6c;
    &:hover { background: #fde0e0; }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}

</style>
