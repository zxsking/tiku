<template>
  <div class="user-profile-page" v-loading="loading">
    <template v-if="user">
      <!-- 用户信息卡片 -->
      <el-card shadow="never" class="profile-card">
        <div class="profile-header">
          <el-avatar :size="72" :src="user.avatar" class="profile-avatar">
            {{ user.username?.charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="profile-info">
            <div class="profile-name">{{ user.username }}</div>
            <div class="profile-bio">{{ user.bio || '这个人很懒，什么都没有留下~' }}</div>
            <div class="profile-meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                加入于 {{ formatDate(user.createdAt) }}
              </span>
            </div>
          </div>
          <div class="profile-actions" v-if="!isOwner">
            <button
              class="follow-btn"
              :class="{ 'follow-btn--active': isFollowed }"
              :disabled="followLoading"
              @click="toggleFollow"
            >
              {{ isFollowed ? '已关注' : '+ 关注' }}
            </button>
          </div>
          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ user.bankCount ?? 0 }}</div>
              <div class="stat-label">题库</div>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <div class="stat-value">{{ user.questionCount ?? 0 }}</div>
              <div class="stat-label">题目</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 内容区域 -->
      <el-tabs v-model="activeTab" class="content-tabs">
        <!-- 题库 Tab -->
        <el-tab-pane name="banks">
          <template #label>
            <span class="tab-label">
              <el-icon><Collection /></el-icon>题库
              <el-tag size="small" type="info" class="tab-count">{{ bankTotal }}</el-tag>
            </span>
          </template>

          <div v-loading="bankLoading" class="tab-content">
            <el-row :gutter="16">
              <el-col :span="8" v-for="bank in banks" :key="bank.id">
                <div class="bank-card" @click="$router.push(`/banks/${bank.id}`)">
                  <div class="bank-cover">
                    <div class="bank-avatar">{{ bank.name?.charAt(0) }}</div>
                  </div>
                  <div class="bank-body">
                    <div class="bank-name">{{ bank.name }}</div>
                    <div class="bank-desc">{{ bank.description || '暂无描述' }}</div>
                    <div class="bank-footer">
                      <el-tag v-if="bank.categoryName" size="small" class="category-tag">{{ bank.categoryName }}</el-tag>
                      <div class="bank-stats">
                        <span title="题目数量"><el-icon><Document /></el-icon>{{ bank.questionCount }} 题</span>
                        <span title="浏览量"><el-icon><View /></el-icon>{{ bank.viewCount }}</span>
                        <span title="收藏数"><el-icon><Star /></el-icon>{{ bank.favoriteCount }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
            <el-empty v-if="!bankLoading && !banks.length" description="暂无公开题库" />
            <div class="pagination-wrapper" v-if="bankTotal > bankPageSize">
              <el-pagination
                v-model:current-page="bankPage"
                :page-size="bankPageSize"
                :total="bankTotal"
                background
                layout="prev, pager, next"
                @current-change="loadBanks"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 题目 Tab -->
        <el-tab-pane name="questions">
          <template #label>
            <span class="tab-label">
              <el-icon><List /></el-icon>题目
              <el-tag size="small" type="info" class="tab-count">{{ questionTotal }}</el-tag>
            </span>
          </template>

          <div v-loading="questionLoading" class="tab-content">
            <div class="question-list">
              <div
                v-for="q in questions"
                :key="q.id"
                class="question-item"
                @click="$router.push(`/questions/${q.id}`)"
              >
                <div class="question-main">
                  <div class="question-tags">
                    <el-tag :type="typeTagColor(q.type)" size="small">{{ typeName(q.type) }}</el-tag>
                    <el-tag :type="difficultyTagColor(q.difficulty)" size="small" effect="plain">{{ difficultyName(q.difficulty) }}</el-tag>
                  </div>
                  <div class="question-content">{{ q.content }}</div>
                </div>
                <el-icon class="question-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
            <el-empty v-if="!questionLoading && !questions.length" description="暂无公开题目" />
            <div class="pagination-wrapper" v-if="questionTotal > questionPageSize">
              <el-pagination
                v-model:current-page="questionPage"
                :page-size="questionPageSize"
                :total="questionTotal"
                background
                layout="prev, pager, next"
                @current-change="loadQuestions"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-empty v-else-if="!loading" description="用户不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { useInteractionStore } from '@/store/interaction'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const interactionStore = useInteractionStore()

const loading = ref(false)
const user = ref(null)
const activeTab = ref('banks')

const banks = ref([])
const bankPage = ref(1)
const bankPageSize = 12
const bankTotal = ref(0)
const bankLoading = ref(false)

const questions = ref([])
const questionPage = ref(1)
const questionPageSize = 15
const questionTotal = ref(0)
const questionLoading = ref(false)
const followLoading = ref(false)
const isFollowed = ref(false)

const isOwner = computed(() => Number(userStore.userInfo?.id) === Number(route.params.id))

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

function typeName(type) {
  return { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }[type] || type
}

function typeTagColor(type) {
  return { single: undefined, multiple: 'warning', judge: 'success', fill: 'info', essay: 'danger' }[type]
}

function difficultyName(d) {
  return { easy: '简单', medium: '中等', hard: '困难' }[d] || d
}

function difficultyTagColor(d) {
  return { easy: 'success', medium: 'warning', hard: 'danger' }[d]
}

async function loadProfile() {
  loading.value = true
  try {
    user.value = await request.get(`/user/${route.params.id}/profile`)
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

async function syncFollowState() {
  if (!userStore.isLoggedIn || isOwner.value) {
    isFollowed.value = false
    return
  }
  try {
    const data = await interactionStore.fetchFollowing()
    const list = Array.isArray(data) ? data : (data?.list ?? [])
    isFollowed.value = list.some(item => Number(item?.id) === Number(route.params.id))
  } catch {
    isFollowed.value = false
  }
}

async function loadBanks() {
  bankLoading.value = true
  try {
    const data = await request.get(`/user/${route.params.id}/banks`, {
      params: { page: bankPage.value, size: bankPageSize }
    })
    banks.value = data?.records || []
    bankTotal.value = data?.total || 0
  } finally {
    bankLoading.value = false
  }
}

async function loadQuestions() {
  questionLoading.value = true
  try {
    const data = await request.get(`/user/${route.params.id}/questions`, {
      params: { page: questionPage.value, size: questionPageSize }
    })
    questions.value = data?.records || []
    questionTotal.value = data?.total || 0
  } finally {
    questionLoading.value = false
  }
}

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再关注')
    router.push('/login')
    return
  }
  if (isOwner.value) return
  if (followLoading.value) return

  followLoading.value = true
  try {
    await interactionStore.toggleFollow(route.params.id)
    isFollowed.value = !isFollowed.value
    ElMessage.success(isFollowed.value ? '关注成功' : '已取消关注')
    await userStore.fetchUserInfo()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

onMounted(async () => {
  await loadProfile()
  await syncFollowState()
  await Promise.all([loadBanks(), loadQuestions()])
})
</script>

<style lang="scss" scoped>
.user-profile-page {
  padding: 4px 0;
}

// ── 用户信息卡片 ──────────────────────────────────────
.profile-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  margin-bottom: 20px;

  :deep(.el-card__body) { padding: 24px 28px; }
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-actions {
  flex-shrink: 0;
}

.follow-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid #409eff;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(:disabled) {
    opacity: 0.88;
  }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
  }

  &--active {
    background: #fff;
    color: #606266;
    border-color: #dcdfe6;
  }
}

.profile-avatar {
  flex-shrink: 0;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  color: #fff;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 6px;
}

.profile-bio {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
}

.profile-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.profile-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
  padding: 12px 24px;
  background: #f7f9fc;
  border-radius: 10px;
  border: 1px solid #edf0f5;
}

.stat-item {
  text-align: center;

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #409eff;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e4e7ed;
}

// ── Tabs ──────────────────────────────────────────────
.content-tabs {
  :deep(.el-tabs__header) { margin-bottom: 16px; }
  :deep(.el-tabs__item) { font-size: 14px; font-weight: 500; }
  :deep(.el-tabs__item.is-active) { font-weight: 600; }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.tab-count {
  border-radius: 10px;
  :deep(.el-tag__content) { color: #909399; }
}

.tab-content {
  min-height: 200px;
}

// ── 题库卡片 ──────────────────────────────────────────
.bank-card {
  border: 1px solid #edf0f5;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  }

  .bank-cover {
    height: 64px;
    background: linear-gradient(135deg, #e8f4ff, #dbeeff);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .bank-avatar {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .bank-body { padding: 12px; }

  .bank-name {
    font-weight: 600;
    font-size: 14px;
    color: #1a1a2e;
    margin-bottom: 4px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 40px;
    line-height: 1.45;
  }

  .bank-desc {
    color: #909399;
    font-size: 12px;
    line-height: 1.5;
    margin-bottom: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 36px;
  }

  .bank-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .category-tag { border-radius: 5px; font-size: 11px; }

  .bank-stats {
    display: flex;
    gap: 8px;
    color: #b1b8c4;
    font-size: 12px;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }
  }
}

// ── 题目列表 ──────────────────────────────────────────
.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  cursor: pointer;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.15s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 2px 10px rgba(64,158,255,0.08);
  }
}

.question-main { flex: 1; min-width: 0; }

.question-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.question-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-arrow {
  color: #c0c4cc;
  font-size: 14px;
  flex-shrink: 0;
  margin-left: 12px;
}

// ── 分页 ──────────────────────────────────────────────
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #edf0f5;
}
</style>
