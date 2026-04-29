<template>
  <div class="bank-detail-page" v-if="bankStore.currentBank">
    <el-row :gutter="20">

      <!-- 左侧主内容 -->
      <el-col :span="18">

        <!-- 题库信息卡片 -->
        <el-card shadow="never" class="section-card bank-info-card">
          <div class="bank-header">
            <!-- 左：头像 + 信息 -->
            <div class="bank-avatar-wrap">
              <div class="bank-avatar">{{ bankStore.currentBank?.name?.charAt(0) || '题' }}</div>
            </div>
            <div class="bank-main-info">
              <h2 class="bank-title">{{ bankStore.currentBank?.name }}</h2>
              <div class="bank-meta">
                <span
                  class="author-link"
                  @click="bankStore.currentBank?.authorId && router.push(`/users/${bankStore.currentBank.authorId}`)"
                ><el-icon><User /></el-icon>{{ bankStore.currentBank?.author }}</span>
                <span><el-icon><Calendar /></el-icon>{{ bankStore.currentBank?.createdAt }}</span>
                <el-tag size="small" class="category-tag">{{ bankStore.currentBank?.categoryName }}</el-tag>
              </div>
              <div class="bank-desc">{{ bankStore.currentBank?.description }}</div>
            </div>
            <!-- 右：操作按钮 -->
            <div class="bank-actions">
              <button
                class="action-btn"
                :class="bankStore.currentBank?.isFavorited ? 'action-btn--active' : 'action-btn--default'"
                @click="toggleFavorite"
              >
                <el-icon><Star /></el-icon>
                {{ bankStore.currentBank?.isFavorited ? '已收藏' : '收藏' }}
              </button>
              <button
                v-if="userStore.isLoggedIn"
                class="action-btn action-btn--primary"
                @click="$router.push(`/user/practice/start/${bankStore.currentBank?.id}`)"
              >
                <el-icon><VideoPlay /></el-icon>开始练习
              </button>
              <button
                v-if="isOwner"
                class="action-btn action-btn--ghost"
                @click="$router.push(`/user/banks/${bankStore.currentBank?.id}/edit`)"
              >
                <el-icon><Edit /></el-icon>编辑
              </button>
            </div>
          </div>

          <!-- 统计数字 -->
          <div class="bank-stats-row">
            <div class="stat-item" v-for="s in statItems" :key="s.label">
              <div class="stat-value" :style="`color: ${s.color}`">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>

        <!-- 题目列表 -->
        <el-card shadow="never" class="section-card questions-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <span class="header-title">题目列表</span>
                <el-tag type="info" size="small" class="total-tag">共 {{ questionTotal }} 题</el-tag>
              </div>
              <div class="filter-group">
                <el-select v-model="questionFilters.type" placeholder="全部题型" clearable size="small" style="width: 100px" @change="loadQuestions">
                  <el-option label="单选" value="single" />
                  <el-option label="多选" value="multiple" />
                  <el-option label="判断" value="judge" />
                  <el-option label="填空" value="fill" />
                  <el-option label="简答" value="essay" />
                </el-select>
                <el-select v-model="questionFilters.difficulty" placeholder="全部难度" clearable size="small" style="width: 100px" @change="loadQuestions">
                  <el-option label="简单" value="easy" />
                  <el-option label="中等" value="medium" />
                  <el-option label="困难" value="hard" />
                </el-select>
                <button v-if="isOwner" class="add-btn" @click="$router.push(`/user/questions/create?bankId=${bankStore.currentBank.id}`)">
                  <el-icon><Plus /></el-icon>添加题目
                </button>
              </div>
            </div>
          </template>

          <el-table :data="questions" class="question-table" :row-class-name="() => 'question-row'">
            <el-table-column label="题型" width="80">
              <template #default="{ row }">
                <el-tag :type="getTypeTag(row.type)" size="small" class="type-tag">{{ getTypeName(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="题目内容" min-width="300">
              <template #default="{ row }">
                <router-link :to="`/questions/${row.id}`" class="question-link">{{ row.content }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="80">
              <template #default="{ row }">
                <el-tag :type="getDifficultyTag(row.difficulty)" size="small">{{ getDifficultyName(row.difficulty) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="$router.push(`/questions/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="questionPage"
              :page-size="10"
              :total="questionTotal"
              background
              layout="prev, pager, next"
              @current-change="loadQuestions"
            />
          </div>
        </el-card>

      </el-col>

      <!-- 右侧边栏 -->
      <el-col :span="6">
        <el-card shadow="never" class="section-card author-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #e6a23c"></span>
              <span class="header-title">作者信息</span>
            </div>
          </template>
          <div class="author-info" @click="bankStore.currentBank?.authorId && router.push(`/users/${bankStore.currentBank.authorId}`)" style="cursor:pointer">
            <div class="author-avatar">{{ bankStore.currentBank?.author?.charAt(0) || '作' }}</div>
            <div class="author-detail">
              <div class="author-name">{{ bankStore.currentBank?.author }}</div>
              <div class="author-stats">已创建 {{ bankStore.currentBank?.authorBankCount }} 个题库</div>
            </div>
          </div>
          <button
            v-if="!isOwner"
            class="follow-btn"
            :class="bankStore.currentBank?.isFollowed ? 'follow-btn--active' : ''"
            @click="toggleFollow"
          >
            {{ bankStore.currentBank?.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </el-card>
      </el-col>

    </el-row>
  </div>
  <div v-else class="bank-detail-page empty-state">
    <el-result
      icon="warning"
      title="题库不可访问"
      sub-title="题库可能已被删除、设为私有或正在审核中"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/banks')">返回题库列表</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useBankStore } from '@/store/bank'
import { useQuestionStore } from '@/store/question'
import { useInteractionStore } from '@/store/interaction'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const bankStore = useBankStore()
const questionStore = useQuestionStore()
const interactionStore = useInteractionStore()

const questions = ref([])
const questionTotal = ref(0)
const questionPage = ref(1)
const questionFilters = reactive({ type: '', difficulty: '' })

const isOwner = computed(() => userStore.userInfo?.id === bankStore.currentBank?.authorId)

const statItems = computed(() => [
  { label: '题目数量', value: bankStore.currentBank?.questionCount, color: '#409eff' },
  { label: '收藏数',   value: bankStore.currentBank?.favoriteCount,  color: '#f56c6c' },
  { label: '浏览量',   value: bankStore.currentBank?.viewCount,      color: '#67c23a' },
  { label: '点赞数',   value: bankStore.currentBank?.likeCount,      color: '#e6a23c' },
])

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

async function loadQuestions() {
  const data = await questionStore.fetchBankQuestions(route.params.id, {
    page: questionPage.value, ...questionFilters
  })
  questions.value = data.records || data.list || []
  questionTotal.value = data.total || 0
}

async function toggleFavorite() {
  await bankStore.toggleFavorite(bankStore.currentBank.id)
  bankStore.currentBank.isFavorited = !bankStore.currentBank.isFavorited
  ElMessage.success(bankStore.currentBank.isFavorited ? '收藏成功' : '已取消收藏')
}

async function toggleFollow() {
  try {
    await interactionStore.toggleFollow(bankStore.currentBank.authorId)
    bankStore.currentBank.isFollowed = !bankStore.currentBank.isFollowed
    ElMessage.success(bankStore.currentBank.isFollowed ? '关注成功' : '已取消关注')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

onMounted(async () => {
  try {
    await bankStore.fetchBank(route.params.id)
    await loadQuestions()
  } catch (e) {
    ElMessage.error(e?.message || '无法访问该题库')
  }
})
</script>

<style lang="scss" scoped>
// ── 页面 ──────────────────────────────────────────────
.bank-detail-page {
  padding: 4px 0;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

// ── 通用卡片 ──────────────────────────────────────────
.section-card {
  margin-bottom: 20px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f5;
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

// ── 头部通用 ──────────────────────────────────────────
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

// ── 题库信息卡片 ──────────────────────────────────────
.bank-info-card {
  :deep(.el-card__body) { padding: 28px; }
}

.bank-header {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.bank-avatar-wrap {
  flex-shrink: 0;
}

.bank-avatar {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.35);
}

.bank-main-info {
  flex: 1;
  min-width: 0;
}

.bank-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 10px;
  letter-spacing: -0.3px;
}

.bank-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .author-link {
    cursor: pointer;
    user-select: none;
    transition: color 0.15s;
    &:hover {
      color: #409eff;
      text-decoration: underline;
    }
  }
}

.category-tag {
  border-radius: 6px;
}

.bank-desc {
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
}

// ── 操作按钮 ──────────────────────────────────────────
.bank-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s, transform 0.15s;
  white-space: nowrap;

  &:active { transform: scale(0.97); }

  &--default {
    background: #f0f6ff;
    color: #409eff;
    border: 1px solid #c6e0ff;

    &:hover { background: #e0efff; }
  }

  &--active {
    background: linear-gradient(135deg, #f56c6c, #e85555);
    color: #fff;
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);

    &:hover { opacity: 0.88; }
  }

  &--primary {
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);

    &:hover { opacity: 0.88; }
  }

  &--ghost {
    background: #fff;
    color: #606266;
    border: 1px solid #dcdfe6;

    &:hover { border-color: #409eff; color: #409eff; }
  }
}

// ── 统计数字行 ────────────────────────────────────────
.bank-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  overflow: hidden;
}

.stat-item {
  padding: 16px 0;
  text-align: center;
  border-right: 1px solid #edf0f5;
  background: #fafbfd;

  &:last-child { border-right: none; }

  .stat-value {
    font-size: 26px;
    font-weight: 700;
    letter-spacing: -0.5px;
    line-height: 1;
    margin-bottom: 6px;
  }

  .stat-label {
    font-size: 12px;
    color: #909399;
  }
}

// ── 题目列表筛选 ──────────────────────────────────────
.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;

  :deep(.el-select .el-input__wrapper) {
    border-radius: 7px;
  }
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  color: #fff;
  box-shadow: 0 3px 10px rgba(64, 158, 255, 0.3);
  transition: opacity 0.2s;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover { opacity: 0.88; }
}

// ── 题目表格 ──────────────────────────────────────────
.question-table {
  border-radius: 8px;
  overflow: hidden;

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
  color: #303133;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.15s;

  &:hover { color: #409eff; }
}

// ── 分页 ──────────────────────────────────────────────
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}

// ── 作者卡片 ──────────────────────────────────────────
.author-card {
  :deep(.el-card__body) { padding: 20px; }
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  border-radius: 8px;
  transition: background 0.15s;
  padding: 4px;
  margin: -4px -4px 12px;

  &:hover {
    background: #f5f7fa;
    .author-name { color: #409eff; }
  }
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e6a23c, #d08f20);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
  flex-shrink: 0;
}

.author-detail {
  flex: 1;
  min-width: 0;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.author-stats {
  color: #909399;
  font-size: 12px;
}

.follow-btn {
  width: 100%;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
    background: #f0f6ff;
  }

  &--active {
    background: #f0f6ff;
    color: #409eff;
    border-color: #c6e0ff;
  }
}
</style>