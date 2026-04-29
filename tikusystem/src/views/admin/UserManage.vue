<template>
  <div class="user-manage-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">用户管理</span>
          </div>
          <el-input v-model="searchKeyword" placeholder="搜索用户..." class="search-input" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="users" class="user-table" :row-class-name="() => 'user-row'">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">{{ row.username.charAt(0) }}</div>
              <div class="user-info">
                <div class="username">{{ row.username }}</div>
                <div class="email">{{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small" class="role-tag">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-dot" :class="`status-dot--${row.status}`"></span>
              <span class="status-text" :class="`status-text--${row.status}`">
                {{ row.status === 'active' ? '正常' : '禁用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="统计" width="200">
          <template #default="{ row }">
            <div class="stats-cell">
              <span><el-icon><Collection /></el-icon>{{ row.bankCount ?? '-' }}</span>
              <span><el-icon><Document /></el-icon>{{ row.questionCount ?? '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="action-group">
              <button 
                v-if="row.role !== 'admin'" 
                class="op-btn op-btn--primary"
                @click="changeRole(row, 'admin')"
              >
                设为管理员
              </button>
              <button 
                v-else
                class="op-btn op-btn--warn"
                @click="changeRole(row, 'user')"
              >
                取消管理员
              </button>
              <button 
                class="op-btn"
                :class="row.status === 'active' ? 'op-btn--delete' : 'op-btn--success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserRole, updateUserStatus } from '@/api/admin'

const searchKeyword = ref('')
const page = ref(1)
const total = ref(0)
const users = ref([])
let searchTimer = null

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadUsers() {
  const params = { page: page.value, size: 10 }
  if (searchKeyword.value?.trim()) {
    params.keyword = searchKeyword.value.trim()
  }
  const data = await getUsers(params)
  users.value = data.records || []
  total.value = data.total || 0
}

async function changeRole(user, role) {
  const msg = role === 'admin'
    ? `确定将 ${user.username} 设为管理员吗？`
    : `确定取消 ${user.username} 的管理员权限吗？`
  await ElMessageBox.confirm(msg)
  await updateUserRole(user.id, role)
  user.role = role
  ElMessage.success(role === 'admin' ? '已设为管理员' : '已取消管理员')
}

async function toggleStatus(user) {
  const action = user.status === 'active' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}用户 ${user.username} 吗？`)
  const nextStatus = user.status === 'active' ? 'disabled' : 'active'
  await updateUserStatus(user.id, nextStatus)
  user.status = nextStatus
  ElMessage.success(`已${action}`)
}

watch(page, () => {
  loadUsers()
})

watch(searchKeyword, () => {
  page.value = 1
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadUsers()
  }, 300)
})

onMounted(() => {
  loadUsers()
})
</script>

<style lang="scss" scoped>
.user-manage-page {
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

.search-input {
  width: 250px;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

.user-table {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header-wrapper th) {
    background: #f7f9fc;
    color: #909399;
    font-weight: 500;
    font-size: 13px;
  }

  :deep(.user-row td) {
    padding: 12px 0;
  }

  :deep(.el-table__row:hover > td) {
    background: #f7f9fc !important;
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
  }

  .user-info {
    .username {
      font-weight: 600;
      font-size: 14px;
      color: #303133;
      margin-bottom: 2px;
    }

    .email {
      color: #909399;
      font-size: 12px;
    }
  }
}

.role-tag {
  border-radius: 6px;
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

  &--active { background: #67c23a; box-shadow: 0 0 0 2px rgba(103,194,58,0.2); }
  &--disabled { background: #f56c6c; box-shadow: 0 0 0 2px rgba(245,108,108,0.2); }
}

.status-text {
  font-size: 13px;

  &--active { color: #67c23a; }
  &--disabled { color: #f56c6c; }
}

.stats-cell {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .el-icon {
    color: #b1b8c4;
  }
}

.action-group {
  display: flex;
  gap: 6px;
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

  &--primary {
    background: #f0f6ff;
    color: #409eff;
    &:hover { background: #dbeeff; }
  }

  &--success {
    background: #f0faf0;
    color: #67c23a;
    &:hover { background: #d9f0d9; }
  }

  &--delete {
    background: #fff2f2;
    color: #f56c6c;
    &:hover { background: #fde0e0; }
  }

  &--warn {
    background: #fdf6ec;
    color: #e6a23c;
    &:hover { background: #faecd8; }
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