<template>
  <div class="home-page">

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="(stat, i) in statCards" :key="i">
        <el-card
          shadow="never"
          class="stat-card stat-card--clickable"
          :style="`--accent: ${stat.color}`"
          @click="router.push(stat.to)"
        >
          <div class="stat-content">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
          <div class="stat-bg-icon">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧主内容 -->
      <el-col :span="16">

        <!-- 热门题库 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <span class="header-title">热门题库</span>
              </div>
              <el-button text type="primary" class="more-btn" @click="$router.push('/banks')">
                查看更多 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="8" v-for="bank in bankStore.hotBanks" :key="bank.id">
              <div class="bank-card" @click="$router.push(`/banks/${bank.id}`)">
                <div class="bank-cover">
                  <el-icon class="bank-cover-icon"><Collection /></el-icon>
                </div>
                <div class="bank-body">
                  <div class="bank-name">{{ bank.name }}</div>
                  <div class="bank-desc">{{ bank.description }}</div>
                  <div class="bank-meta">
                    <span><el-icon><Document /></el-icon>{{ bank.questionCount }} 题</span>
                    <span
                      class="bank-fav"
                      :class="{ 'bank-fav--active': bank.isFavorited }"
                      @click.stop="handleToggleFavorite(bank)"
                    >
                      <el-icon><Star /></el-icon>{{ bank.favoriteCount || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 最新题目 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #67c23a"></span>
                <span class="header-title">最新题目</span>
              </div>
            </div>
          </template>
          <div class="question-list">
            <div
              v-for="question in questionStore.latestQuestions"
              :key="question.id"
              class="question-item"
              @click="$router.push(`/questions/${question.id}`)"
            >
              <div class="question-content">
                <el-tag :type="getQuestionTypeTag(question.type)" size="small" class="type-tag">
                  {{ getQuestionTypeName(question.type) }}
                </el-tag>
                <span class="question-text">{{ question.content }}</span>
              </div>
              <div class="question-meta">
                <span class="meta-bank"><el-icon><Collection /></el-icon>{{ question.bankName }}</span>
                <span class="meta-date"><el-icon><Clock /></el-icon>{{ question.createdAt }}</span>
              </div>
            </div>
          </div>
        </el-card>

      </el-col>

      <!-- 右侧边栏 -->
      <el-col :span="8">

        <!-- 分类导航 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #e6a23c"></span>
                <span class="header-title">分类导航</span>
              </div>
            </div>
          </template>
          <div class="category-list">
            <div
              v-for="category in categoryStore.categories"
              :key="category.id"
              class="category-item"
              @click="$router.push(`/categories?id=${category.id}`)"
            >
              <div class="category-icon-wrap">
                <el-icon><Folder /></el-icon>
              </div>
              <span class="category-name">{{ category.name }}</span>
              <el-badge :value="category.count || 0" class="category-badge" type="info" />
            </div>
          </div>
        </el-card>

      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useBankStore } from '@/store/bank'
import { useQuestionStore } from '@/store/question'
import { useCategoryStore } from '@/store/category'
import { useStatsStore } from '@/store/stats'
import { useInteractionStore } from '@/store/interaction'
import { Collection, Document, Star, User, Clock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const bankStore = useBankStore()
const questionStore = useQuestionStore()
const categoryStore = useCategoryStore()
const statsStore = useStatsStore()
const interactionStore = useInteractionStore()

const favoriteCount = computed(() => {
  const fromProfile = Number(userStore.userInfo?.favoriteCount)
  if (Number.isFinite(fromProfile)) return fromProfile
  return Number(interactionStore.favoriteTotal || 0)
})

const followingCount = computed(() => {
  const fromProfile = Number(userStore.userInfo?.followingCount)
  if (Number.isFinite(fromProfile)) return fromProfile
  return Number(interactionStore.followingUsers.length || 0)
})

const statCards = computed(() => {
  const card3 = userStore.isLoggedIn
    ? { icon: 'Star',       color: '#f56c6c', value: favoriteCount.value,                      label: '我的收藏',       to: '/user/favorites' }
    : { icon: 'Star',       color: '#c0c4cc', value: '—',                                     label: '登录后查看收藏', to: '/login' }

  const card4 = userStore.isLoggedIn
    ? { icon: 'User',       color: '#e6a23c', value: followingCount.value,                     label: '我的关注',       to: '/user/following' }
    : { icon: 'User',       color: '#c0c4cc', value: '—',                                     label: '登录后查看关注', to: '/login' }

  return [
    { icon: 'Collection', color: '#409eff', value: statsStore.stats.totalBanks,     label: '题库总数', to: '/banks' },
    { icon: 'Document',   color: '#67c23a', value: statsStore.stats.totalQuestions, label: '题目总数', to: '/questions' },
    card3,
    card4,
  ]
})

function getQuestionTypeName(type) {
  const types = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }
  return types[type] || type
}

function getQuestionTypeTag(type) {
  const tags = { single: undefined, multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }
  return tags[type]
}

async function handleToggleFavorite(bank) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏题库')
    router.push('/login')
    return
  }
  if (!bank || !bank.id) return
  if (bank._favLoading) return
  bank._favLoading = true
  try {
    const res = await bankStore.toggleFavorite(bank.id)
    // 后端一般只返回“操作成功”，这里本地维护状态
    const next = !bank.isFavorited
    bank.isFavorited = next
    const delta = next ? 1 : -1
    const current = Number(bank.favoriteCount || 0)
    bank.favoriteCount = Math.max(0, current + delta)
  } catch (e) {
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  } finally {
    bank._favLoading = false
  }
}

onMounted(async () => {
  const tasks = [
    statsStore.fetchStats(),
    bankStore.fetchHotBanks(6),
    questionStore.fetchLatestQuestions(5),
    categoryStore.fetchCategories()
  ]
  if (userStore.isLoggedIn) {
    tasks.push(userStore.fetchUserInfo())
    tasks.push(interactionStore.fetchFavoriteQuestions())
    tasks.push(interactionStore.fetchFollowing())
  }
  await Promise.all(tasks)
})
</script>

<style lang="scss" scoped>
// ── 全局变量 ──────────────────────────────────────────
:root {
  --radius: 12px;
  --border: #edf0f5;
}

.home-page {
  padding: 4px 0;
}

// ── 统计卡片 ──────────────────────────────────────────
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;

  &--clickable {
    cursor: pointer;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08) !important;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
    position: relative;
    z-index: 1;
  }

  .stat-icon-wrap {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: color-mix(in srgb, var(--accent) 12%, white);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-icon {
    font-size: 26px;
    color: var(--accent);
  }

  .stat-value {
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
    color: #1a1a2e;
    letter-spacing: -0.5px;
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 6px;
  }

  // 背景装饰图标
  .stat-bg-icon {
    position: absolute;
    right: -8px;
    bottom: -8px;
    font-size: 80px;
    color: color-mix(in srgb, var(--accent) 7%, white);
    pointer-events: none;
  }
}

// ── 通用卡片 ──────────────────────────────────────────
.section-card {
  margin-bottom: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border);
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

// ── 卡片头部 ──────────────────────────────────────────
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

.more-btn {
  font-size: 13px;
  padding: 0;

  .el-icon {
    font-size: 12px;
    transition: transform 0.2s;
  }

  &:hover .el-icon {
    transform: translateX(3px);
  }
}

// ── 题库卡片 ──────────────────────────────────────────
.bank-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.09);

    .bank-cover-icon {
      transform: scale(1.1) rotate(-5deg);
    }
  }

  .bank-cover {
    height: 72px;
    background: linear-gradient(135deg, #e8f4ff 0%, #dbeeff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .bank-cover-icon {
    font-size: 32px;
    color: #409eff;
    opacity: 0.6;
    transition: transform 0.3s;
  }

  .bank-body {
    padding: 12px;
  }

  .bank-name {
    font-weight: 600;
    font-size: 14px;
    color: #303133;
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bank-desc {
    color: #909399;
    font-size: 12px;
    margin-bottom: 10px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.5;
  }

  .bank-meta {
    display: flex;
    gap: 12px;
    color: #b1b8c4;
    font-size: 12px;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }

    .bank-fav {
      cursor: pointer;

      .el-icon {
        color: #d3d7de;
        transition: color 0.15s, transform 0.15s;
      }

      &--active .el-icon {
        color: #f7ba2a;
        transform: scale(1.05);
      }

      &:hover .el-icon {
        color: #f7d06a;
      }
    }
  }
}

// ── 题目列表 ──────────────────────────────────────────
.question-list {
  .question-item {
    padding: 13px 0;
    border-bottom: 1px solid #f2f4f8;
    cursor: pointer;
    border-radius: 6px;
    transition: background 0.15s, padding 0.15s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f7f9fc;
      padding: 13px 12px;
    }

    .question-content {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .type-tag {
      flex-shrink: 0;
      border-radius: 4px;
    }

    .question-text {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .question-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #b1b8c4;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

// ── 分类导航 ──────────────────────────────────────────
.category-list {
  .category-item {
    display: flex;
    align-items: center;
    padding: 10px 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;

    &:hover {
      background: #f0f6ff;
      color: #409eff;

      .category-icon-wrap {
        background: #d6e9ff;
        color: #409eff;
      }
    }

    .category-icon-wrap {
      width: 30px;
      height: 30px;
      border-radius: 8px;
      background: #f2f4f8;
      color: #909399;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;
      flex-shrink: 0;
      transition: background 0.15s, color 0.15s;
    }

    .category-name {
      font-size: 14px;
      color: inherit;
      flex: 1;
    }

    .category-badge {
      :deep(.el-badge__content) {
        background: #edf0f5;
        color: #909399;
        border: none;
        font-weight: 500;
      }
    }
  }
}

</style>