<template>
  <div class="my-questions-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">我的题目</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ total }} 题</el-tag>
          </div>
          <button class="create-btn" @click="$router.push('/user/questions/create')">
            <el-icon><Plus /></el-icon>添加题目
          </button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input v-model="filters.keyword" placeholder="搜索题目内容..." class="filter-search" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="filter-right">
          <el-select v-model="filters.bankId" placeholder="全部题库" clearable style="width: 130px">
            <el-option v-for="bank in myBanks" :key="bank.id" :label="bank.name" :value="bank.id" />
          </el-select>
          <el-select v-model="filters.type" placeholder="全部题型" clearable style="width: 110px">
            <el-option label="单选" value="single" />
            <el-option label="多选" value="multiple" />
            <el-option label="判断" value="judge" />
            <el-option label="填空" value="fill" />
            <el-option label="简答" value="essay" />
          </el-select>
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option label="审核中" value="pending" />
            <el-option label="已发布" value="published" />
          </el-select>
        </div>
      </div>

      <!-- 批量操作栏 -->
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 道题目</span>
        <el-popconfirm
          :title="`确定删除选中的 ${selectedIds.length} 道题目吗？`"
          @confirm="batchDelete"
        >
          <template #reference>
            <button class="batch-delete-btn">
              <el-icon><Delete /></el-icon>批量删除
            </button>
          </template>
        </el-popconfirm>
        <button class="batch-cancel-btn" @click="selectedIds = []">取消选择</button>
      </div>

      <!-- 表格 -->
      <el-table
        ref="questionTableRef"
        :data="questions"
        class="question-table"
        :class="{ 'is-dragging': isDragging }"
        :row-class-name="() => 'question-row'"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small" class="type-tag">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="题目内容" min-width="260">
          <template #default="{ row }">
            <router-link :to="`/questions/${row.id}`" class="question-link">{{ row.content }}</router-link>
          </template>
        </el-table-column>

        <el-table-column prop="bankName" label="所属题库" width="160">
          <template #default="{ row }">
            <span class="bank-name-cell">
              <el-icon><Collection /></el-icon>{{ row.bankName || '-' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTag(row.difficulty)" size="small">{{ getDifficultyName(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-dot" :class="`status-dot--${getStatusKey(row)}`"></span>
              <span class="status-text" :class="`status-text--${getStatusKey(row)}`">{{ getStatusName(row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <div class="action-group">
              <button class="op-btn op-btn--edit" @click="$router.push(`/user/questions/${row.id}/edit`)">编辑</button>
              <button class="op-btn op-btn--view" @click="$router.push(`/questions/${row.id}`)">查看</button>
              <el-popconfirm title="确定删除该题目吗？" @confirm="deleteQuestion(row.id)">
                <template #reference>
                  <button class="op-btn op-btn--delete">删除</button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!questions.length" description="暂无题目" class="empty-state" />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="10"
          :total="total"
          background
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useQuestionStore } from '@/store/question'
import { useBankStore } from '@/store/bank'
import { useDragSelect } from '@/composables/useDragSelect'

const questionStore = useQuestionStore()
const bankStore = useBankStore()

const filters = reactive({ bankId: '', type: '', status: '', keyword: '' })
const page = ref(1)
const total = ref(0)
const selectedIds = ref([])

const myBanks = ref([])
const questions = ref([])

const { tableRef: questionTableRef, isDragging } = useDragSelect(questions, selectedIds)

function handleSelectionChange(rows) {
  if (!isDragging.value) {
    selectedIds.value = rows.map(r => r.id)
  }
}

function getTypeName(type) {
  return { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }[type] || type
}
function getTypeTag(type) {
  return { single: undefined, multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }[type]
}
function getDifficultyName(diff) {
  return { easy: '简单', medium: '中等', hard: '困难' }[diff] || diff
}
function getDifficultyTag(diff) {
  return { easy: 'success', medium: 'warning', hard: 'danger' }[diff]
}
function getStatusKey(row) {
  if (row.bankVisibility === 'private' && row.status === 'published') return 'private'
  if (row.bankVisibility === 'public' && row.status === 'pending') return 'pending'
  if (row.bankVisibility === 'public' && row.status === 'published') return 'public_published'
  // 兜底：仅凭 status 判断
  if (row.status === 'pending') return 'pending'
  return 'private'
}

function getStatusName(row) {
  const key = getStatusKey(row)
  return { private: '私有', pending: '审核中', public_published: '已公开' }[key] || key
}

async function loadMyQuestions() {
  const params = {
    page: page.value,
    size: 10
  }
  Object.keys(filters).forEach(key => {
    if (filters[key] !== '' && filters[key] !== null && filters[key] !== undefined) {
      params[key] = filters[key]
    }
  })
  const data = await questionStore.fetchMyQuestions(params)
  questions.value = data.records || data.list || []
  total.value = data.total || 0
}

async function loadMyBanks() {
  const data = await bankStore.fetchMyBanks({})
  myBanks.value = data.records || data.list || []
}

async function deleteQuestion(id) {
  await questionStore.deleteQuestion(id)
  ElMessage.success('删除成功')
  loadMyQuestions()
}

async function batchDelete() {
  const ids = [...selectedIds.value]
  try {
    await Promise.all(ids.map(id => questionStore.deleteQuestion(id)))
    ElMessage.success(`已删除 ${ids.length} 道题目`)
    selectedIds.value = []
    loadMyQuestions()
  } catch {
    ElMessage.error('部分题目删除失败')
    loadMyQuestions()
  }
}

onMounted(() => {
  loadMyBanks()
  loadMyQuestions()
})

watch(page, () => {
  loadMyQuestions()
})

watch(filters, () => {
  page.value = 1
  loadMyQuestions()
}, { deep: true })
</script>

<style lang="scss" scoped>
// ── 页面 ──────────────────────────────────────────────
.my-questions-page {
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

// ── 头部 ──────────────────────────────────────────────
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

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
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

// ── 批量操作栏 ────────────────────────────────────────
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: #fff8e6;
  border: 1px solid #fde9a0;
  border-radius: 8px;

  .batch-info {
    font-size: 13px;
    color: #606266;
    flex: 1;

    strong { color: #e6a23c; }
  }
}

.batch-delete-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: #f56c6c;
  color: #fff;
  transition: opacity 0.15s;

  &:hover { opacity: 0.88; }
}

.batch-cancel-btn {
  height: 30px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  transition: all 0.15s;

  &:hover { border-color: #409eff; color: #409eff; }
}

// ── 筛选栏 ────────────────────────────────────────────
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 10px;
  border: 1px solid #edf0f5;

  .filter-search {
    flex: 1;
    max-width: 280px;

    :deep(.el-input__wrapper) {
      border-radius: 8px;
      background: #fff;
    }
  }

  .filter-right {
    display: flex;
    gap: 10px;

    :deep(.el-select .el-input__wrapper) {
      border-radius: 8px;
      background: #fff;
    }
  }
}

// ── 表格 ──────────────────────────────────────────────
.question-table {
  border-radius: 8px;
  overflow: hidden;

  &.is-dragging {
    cursor: crosshair;
    user-select: none;
  }

  :deep(.el-table__header-wrapper th) {
    background: #f7f9fc;
    color: #909399;
    font-weight: 500;
    font-size: 13px;
  }

  :deep(.question-row td) {
    padding: 12px 0;
  }

  :deep(.el-table__row:hover > td) {
    background: #f7f9fc !important;
  }
}

.type-tag {
  border-radius: 5px;
}

.question-link {
  font-size: 14px;
  color: #303133;
  text-decoration: none;
  transition: color 0.15s;

  &:hover { color: #409eff; }
}

.bank-name-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #606266;

  .el-icon { color: #b1b8c4; }
}

// ── 状态列 ────────────────────────────────────────────
.status-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;

  &--public_published { background: #67c23a; box-shadow: 0 0 0 2px rgba(103,194,58,0.2); }
  &--pending          { background: #e6a23c; box-shadow: 0 0 0 2px rgba(230,162,60,0.2); }
  &--private          { background: #909399; box-shadow: 0 0 0 2px rgba(144,147,153,0.2); }
}

.status-text {
  font-size: 13px;

  &--public_published { color: #67c23a; }
  &--pending          { color: #e6a23c; }
  &--private          { color: #909399; }
}

// ── 操作按钮组 ────────────────────────────────────────
.action-group {
  display: flex;
  gap: 4px;
  align-items: center;
}

.op-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.15s;

  &--edit {
    background: #f0f6ff;
    color: #409eff;

    &:hover { background: #dbeeff; }
  }

  &--view {
    background: #f0faf0;
    color: #67c23a;

    &:hover { background: #d9f0d9; }
  }

  &--delete {
    background: #fff2f2;
    color: #f56c6c;

    &:hover { background: #fde0e0; }
  }
}

// ── 空状态 / 分页 ─────────────────────────────────────
.empty-state {
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}
</style>