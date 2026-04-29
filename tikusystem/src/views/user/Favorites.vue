<template>
  <div class="favorites-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #f56c6c"></span>
            <span class="header-title">我的收藏</span>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="favorite-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="题库" name="banks">
          <div v-loading="loading">
            <el-row :gutter="16">
              <el-col :span="8" v-for="bank in bankFavorites" :key="bank.id">
                <div class="bank-card" @click="$router.push(`/banks/${bank.id}`)">
                  <div class="bank-cover">
                    <div class="bank-avatar">{{ bank.name.charAt(0) }}</div>
                    <div class="bank-cover-bg"></div>
                  </div>
                  <div class="bank-body">
                    <div class="bank-name">{{ bank.name }}</div>
                    <div class="bank-desc">{{ bank.description }}</div>
                    <div class="bank-meta">
                      <span><el-icon><Document /></el-icon>{{ bank.questionCount }} 题</span>
                    </div>
                  </div>
                  <button class="remove-btn" @click.stop="removeFavorite('bank', bank.id)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
              </el-col>
            </el-row>
            <el-empty v-if="!loading && bankFavorites.length === 0" description="暂无收藏的题库" class="empty-state" />
            <div class="pagination-wrapper" v-if="bankTotal > 0">
              <el-pagination
                v-model:current-page="bankPage"
                :page-size="12"
                :total="bankTotal"
                background
                layout="total, prev, pager, next"
                @current-change="loadFavoriteBanks"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="题目" name="questions">
          <div v-loading="loading">
            <div class="question-list">
              <div
                v-for="question in questionFavorites"
                :key="question.id"
                class="question-item"
                @click="$router.push(`/questions/${question.id}`)"
              >
                <div class="question-content">
                  <el-tag :type="getTypeTag(question.type)" size="small" class="type-tag">{{ getTypeName(question.type) }}</el-tag>
                  <span class="question-text">{{ question.content }}</span>
                </div>
                <div class="question-actions">
                  <span class="question-meta">
                    <el-tag size="small" :type="getDifficultyTag(question.difficulty)">{{ question.difficulty || '未知难度' }}</el-tag>
                  </span>
                  <button class="op-btn op-btn--delete" @click.stop="removeFavorite('question', question.id)">取消收藏</button>
                </div>
              </div>
            </div>
            <el-empty v-if="!loading && questionFavorites.length === 0" description="暂无收藏的题目" class="empty-state" />
            <div class="pagination-wrapper" v-if="questionTotal > 0">
              <el-pagination
                v-model:current-page="questionPage"
                :page-size="20"
                :total="questionTotal"
                background
                layout="total, prev, pager, next"
                @current-change="loadFavoriteQuestions"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useBankStore } from '@/store/bank'

const bankStore = useBankStore()

const activeTab = ref('banks')
const loading = ref(false)

const bankFavorites = ref([])
const bankPage = ref(1)
const bankTotal = ref(0)

const questionFavorites = ref([])
const questionPage = ref(1)
const questionTotal = ref(0)

function getTypeName(type) {
  const map = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }
  return map[type] || type
}

function getTypeTag(type) {
  const map = { single: undefined, multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }
  return map[type]
}

function getDifficultyTag(difficulty) {
  const map = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

async function loadFavoriteBanks() {
  loading.value = true
  try {
    const data = await bankStore.fetchFavorites({ type: 'banks', page: bankPage.value, size: 12 })
    bankFavorites.value = data?.records || data?.list || []
    bankTotal.value = data?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载收藏题库失败')
  } finally {
    loading.value = false
  }
}

async function loadFavoriteQuestions() {
  loading.value = true
  try {
    const data = await bankStore.fetchFavorites({ type: 'questions', page: questionPage.value, size: 20 })
    questionFavorites.value = data?.records || data?.list || []
    questionTotal.value = data?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载收藏题目失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab) {
  if (tab === 'banks') {
    bankPage.value = 1
    loadFavoriteBanks()
  } else {
    questionPage.value = 1
    loadFavoriteQuestions()
  }
}

async function removeFavorite(type, id) {
  try {
    await bankStore.removeFavorite(type, id)
    ElMessage.success('已取消收藏')
    // 从列表中移除，无需重新请求
    if (type === 'bank') {
      bankFavorites.value = bankFavorites.value.filter(b => b.id !== id)
      bankTotal.value = Math.max(0, bankTotal.value - 1)
    } else {
      questionFavorites.value = questionFavorites.value.filter(q => q.id !== id)
      questionTotal.value = Math.max(0, questionTotal.value - 1)
    }
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

onMounted(() => {
  loadFavoriteBanks()
})
</script>

<style lang="scss" scoped>
.favorites-page {
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

.favorite-tabs {
  :deep(.el-tabs__header) {
    margin: 0 0 20px;
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

.bank-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.09);

    .bank-avatar {
      transform: scale(1.05);
    }

    .remove-btn {
      opacity: 1;
    }
  }

  .remove-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid #edf0f5;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    opacity: 0;
    transition: all 0.2s;
    color: #f56c6c;
    z-index: 2;

    &:hover {
      background: #fff;
      border-color: #f56c6c;
    }
  }

  .bank-cover {
    height: 72px;
    background: linear-gradient(135deg, #e8f4ff 0%, #dbeeff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }

  .bank-avatar {
    width: 46px;
    height: 46px;
    border-radius: 12px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
    transition: transform 0.2s;
    position: relative;
    z-index: 1;
  }

  .bank-cover-bg {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 70% 50%, rgba(64, 158, 255, 0.08), transparent 70%);
  }

  .bank-body {
    padding: 12px 14px;
  }

  .bank-name {
    font-weight: 600;
    font-size: 14px;
    color: #1a1a2e;
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bank-desc {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
    margin-bottom: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 36px;
  }

  .bank-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #b1b8c4;
    font-size: 12px;
    padding-top: 8px;
    border-top: 1px solid #f2f4f8;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }
  }
}

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
      flex: 1;
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .question-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .question-meta {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #b1b8c4;
      }
    }
  }
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
}
</style>