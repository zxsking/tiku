<template>
  <div class="my-banks-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">我的题库</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ total }} 个</el-tag>
          </div>
          <button class="create-btn" @click="$router.push('/user/banks/create')">
            <el-icon><Plus /></el-icon>创建题库
          </button>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="私有" name="private" />
        <el-tab-pane label="审核中" name="pending" />
        <el-tab-pane label="已公开" name="public_published" />
      </el-tabs>

      <!-- 批量操作栏 -->
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="batch-info">已选 <strong>{{ selectedIds.length }}</strong> 个题库</span>
        <el-popconfirm
          :title="`确定删除选中的 ${selectedIds.length} 个题库吗？`"
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

      <el-table
        ref="bankTableRef"
        :data="banks"
        class="bank-table"
        :class="{ 'is-dragging': isDragging }"
        :row-class-name="() => 'bank-row'"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="题库名称" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/banks/${row.id}`" class="bank-link">{{ row.name }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.categoryName" size="small" class="category-tag">{{ row.categoryName }}</el-tag>
            <span v-else class="no-category">未分类</span>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="100">
          <template #default="{ row }">
            <span class="count-cell">
              <el-icon><Document /></el-icon>{{ row.questionCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="favoriteCount" label="收藏数" width="100">
          <template #default="{ row }">
            <span class="count-cell">
              <el-icon><Star /></el-icon>{{ row.favoriteCount }}
            </span>
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
        <el-table-column prop="createdAt" label="创建时间" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <div class="action-group">
              <button class="op-btn op-btn--edit" @click="$router.push(`/user/banks/${row.id}/edit`)">编辑</button>
              <button class="op-btn op-btn--view" @click="$router.push(`/banks/${row.id}`)">查看</button>
              <el-popconfirm title="确定删除该题库吗？" @confirm="deleteBank(row.id)">
                <template #reference>
                  <button class="op-btn op-btn--delete">删除</button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!banks.length" description="暂无题库" class="empty-state" />

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
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useBankStore } from '@/store/bank'
import { useDragSelect } from '@/composables/useDragSelect'

const bankStore = useBankStore()

const activeTab = ref('all')
const page = ref(1)
const total = ref(0)
const selectedIds = ref([])

const banks = ref([])

const { tableRef: bankTableRef, isDragging } = useDragSelect(banks, selectedIds)

function handleSelectionChange(rows) {
  // 拖拽过程中由 composable 直接维护 selectedIds，避免重复触发
  if (!isDragging.value) {
    selectedIds.value = rows.map(r => r.id)
  }
}

function getStatusKey(row) {
  if (row.visibility === 'private' && row.status === 'published') return 'private'
  if (row.visibility === 'public' && row.status === 'pending') return 'pending'
  if (row.visibility === 'public' && row.status === 'published') return 'public_published'
  return row.status
}

function getStatusName(row) {
  const key = getStatusKey(row)
  const map = { private: '私有', pending: '审核中', public_published: '已公开' }
  return map[key] || key
}

async function loadMyBanks() {
  const params = { page: page.value, size: 10 }

  // 按 Tab 筛选：通过 visibility + status 组合过滤
  if (activeTab.value === 'private') {
    params.visibility = 'private'
    params.status = 'published'
  } else if (activeTab.value === 'pending') {
    params.visibility = 'public'
    params.status = 'pending'
  } else if (activeTab.value === 'public_published') {
    params.visibility = 'public'
    params.status = 'published'
  }

  const data = await bankStore.fetchMyBanks(params)
  banks.value = data.records || data.list || []
  total.value = data.total || 0
}

function handleTabChange() {
  page.value = 1
  loadMyBanks()
}

async function deleteBank(id) {
  await bankStore.deleteBank(id)
  ElMessage.success('删除成功')
  loadMyBanks()
}

async function batchDelete() {
  const ids = [...selectedIds.value]
  try {
    await Promise.all(ids.map(id => bankStore.deleteBank(id)))
    ElMessage.success(`已删除 ${ids.length} 个题库`)
    selectedIds.value = []
    loadMyBanks()
  } catch {
    ElMessage.error('部分题库删除失败')
    loadMyBanks()
  }
}

onMounted(() => {
  loadMyBanks()
})

watch(page, () => {
  loadMyBanks()
})
</script>

<style lang="scss" scoped>
.my-banks-page {
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

.status-tabs {
  margin-bottom: 16px;

  :deep(.el-tabs__header) {
    margin: 0 0 16px;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    font-weight: 500;
  }

  :deep(.el-tabs__item.is-active) {
    color: #409eff;
    font-weight: 600;
  }
}

.bank-table {
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

  :deep(.bank-row td) {
    padding: 12px 0;
  }

  :deep(.el-table__row:hover > td) {
    background: #f7f9fc !important;
  }
}

.bank-link {
  font-size: 14px;
  color: #303133;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;

  &:hover { color: #409eff; }
}

.category-tag {
  border-radius: 6px;
  font-size: 11px;
}

.no-category {
  font-size: 12px;
  color: #c0c4cc;
}

.count-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;

  .el-icon { color: #b1b8c4; font-size: 14px; }
}

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