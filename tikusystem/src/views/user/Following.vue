<template>
  <div class="following-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #e6a23c"></span>
            <span class="header-title">我的关注</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ followingUsers.length }} 人</el-tag>
          </div>
        </div>
      </template>

      <div class="following-list">
        <div v-for="user in followingUsers" :key="user.id" class="following-item">
          <div class="user-avatar clickable" @click="router.push(`/users/${user.id}`)">{{ user.username.charAt(0) }}</div>
          <div class="user-info">
            <div class="username clickable" @click="router.push(`/users/${user.id}`)">{{ user.username }}</div>
            <div class="user-stats">
              <el-icon><Collection /></el-icon>{{ user.bankCount }} 个题库 · {{ user.questionCount }} 道题目
            </div>
          </div>
          <button class="unfollow-btn" @click="unfollow(user.id)">取消关注</button>
        </div>
      </div>

      <el-empty v-if="followingUsers.length === 0" description="暂无关注的用户" class="empty-state" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useInteractionStore } from '@/store/interaction'
import { useRouter } from 'vue-router'

const interactionStore = useInteractionStore()
const router = useRouter()
const followingUsers = ref([])

async function loadFollowing() {
  const data = await interactionStore.fetchFollowing()
  followingUsers.value = data || []
}

async function unfollow(id) {
  await interactionStore.toggleFollow(id)
  ElMessage.success('已取消关注')
  loadFollowing()
}

onMounted(loadFollowing)
</script>

<style lang="scss" scoped>
.following-page {
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

.following-list {
  .following-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    border-radius: 10px;
    border: 1px solid #edf0f5;
    margin-bottom: 12px;
    transition: all 0.2s;

    &:hover {
      background: #f7f9fc;
      border-color: #c6e0ff;

      .user-avatar {
        transform: scale(1.05);
      }
    }

    .user-avatar {
      width: 52px;
      height: 52px;
      border-radius: 14px;
      background: linear-gradient(135deg, #e6a23c, #d08f20);
      color: #fff;
      font-size: 22px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
      transition: transform 0.2s;

      &.clickable {
        cursor: pointer;
        &:hover { transform: scale(1.08); }
      }
    }

    .user-info {
      flex: 1;
      min-width: 0;

      .username {
        font-weight: 600;
        font-size: 15px;
        color: #303133;
        margin-bottom: 6px;
      }

      .username.clickable {
        cursor: pointer;
        user-select: none;
        &:hover {
          color: #409eff;
          text-decoration: underline;
        }
      }

      .user-stats {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #909399;
        font-size: 13px;

        .el-icon {
          color: #b1b8c4;
          margin-right: 2px;
        }
      }
    }

    .unfollow-btn {
      height: 36px;
      padding: 0 18px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      border: 1px solid #dcdfe6;
      background: #fff;
      color: #606266;
      transition: all 0.15s;
      flex-shrink: 0;

      &:hover {
        border-color: #f56c6c;
        color: #f56c6c;
        background: #fff2f2;
      }
    }
  }
}

.empty-state {
  padding: 60px 0;
}
</style>