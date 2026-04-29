<template>
  <div class="search-page">

    <!-- 搜索框区域 -->
    <div class="search-hero" :class="{ 'search-hero--compact': hasSearched }">
      <div class="search-box" :class="{ 'is-focused': inputFocused }">
        <!-- 自定义类型选择 -->
        <div class="type-selector" @click.stop="typeDropOpen = !typeDropOpen" tabindex="-1">
          <span class="type-label">{{ typeOptions.find(o => o.value === searchType)?.label }}</span>
          <svg class="type-caret" :class="{ open: typeDropOpen }" viewBox="0 0 10 6" width="10" height="6">
            <path d="M0 0l5 6 5-6z" fill="currentColor"/>
          </svg>
          <transition name="drop">
            <ul v-if="typeDropOpen" class="type-dropdown" @click.stop>
              <li
                v-for="opt in typeOptions"
                :key="opt.value"
                :class="{ active: searchType === opt.value }"
                @click="searchType = opt.value; typeDropOpen = false; keyword && handleSearch()"
              >{{ opt.label }}</li>
            </ul>
          </transition>
        </div>
        <div class="divider" />
        <input
          ref="searchInputRef"
          v-model="keyword"
          placeholder="搜索题库或题目..."
          class="search-input"
          @keyup.enter="handleSearch"
          @focus="inputFocused = true"
          @blur="inputFocused = false; typeDropOpen = false"
        />
        <button class="search-btn" @click="handleSearch">
          <el-icon><Search /></el-icon>
        </button>
      </div>
    </div>

    <!-- 结果区域 -->
    <div v-if="hasSearched" class="result-area" v-loading="loading">

      <!-- 结果摘要 -->
      <div class="result-summary">
        <span class="summary-keyword">"{{ keyword }}"</span> 的搜索结果
        <span class="summary-count" v-if="totalCount > 0">共 {{ totalCount }} 条</span>
        <span class="summary-empty" v-else>暂无结果</span>
      </div>

      <!-- 题库结果 -->
      <template v-if="searchType !== 'questions' && searchStore.bankResults?.length > 0">
        <div class="section-label">
          <span class="label-icon" style="background:#409eff"></span>题库
          <span class="label-count">{{ searchStore.bankResults.length }}</span>
        </div>
        <el-row :gutter="16" class="bank-grid">
          <el-col :xs="24" :sm="12" :md="8" v-for="bank in searchStore.bankResults" :key="bank.id">
            <div class="bank-card" @click="$router.push(`/banks/${bank.id}`)">
              <div class="bank-avatar-wrap">
                <div class="bank-avatar">{{ bank.name.charAt(0) }}</div>
              </div>
              <div class="bank-info">
                <div class="bank-name">{{ bank.name }}</div>
                <div class="bank-desc">{{ bank.description || '暂无描述' }}</div>
                <div class="bank-stats">
                  <span><el-icon><Document /></el-icon>{{ bank.questionCount }} 题</span>
                  <span><el-icon><Star /></el-icon>{{ bank.favoriteCount }}</span>
                </div>
              </div>
              <el-icon class="bank-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
        </el-row>
      </template>

      <!-- 题目结果 -->
      <template v-if="searchType !== 'banks' && searchStore.questionResults?.length > 0">
        <div class="section-label">
          <span class="label-icon" style="background:#67c23a"></span>题目
          <span class="label-count">{{ searchStore.questionResults.length }}</span>
        </div>
        <div class="question-list">
          <div
            v-for="question in searchStore.questionResults"
            :key="question.id"
            class="question-item"
            @click="$router.push(`/questions/${question.id}`)"
          >
            <div class="question-left">
              <el-tag :type="getTypeTag(question.type)" size="small" class="type-tag">{{ getTypeName(question.type) }}</el-tag>
              <span class="question-text">{{ question.content }}</span>
            </div>
            <div class="question-right">
              <el-tag :type="getDifficultyTag(question.difficulty)" size="small" class="diff-tag">{{ getDifficultyName(question.difficulty) }}</el-tag>
              <span class="bank-name-tag"><el-icon><Collection /></el-icon>{{ question.bankName }}</span>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-if="!loading && totalCount === 0" description="没有找到相关内容，换个关键词试试" class="empty-state" />
    </div>

    <!-- 未搜索时的提示 -->
    <div v-else class="search-hint">
      <el-icon class="hint-icon"><Search /></el-icon>
      <p>输入关键词搜索题库或题目</p>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSearchStore } from '@/store/search'

const route = useRoute()
const router = useRouter()
const searchStore = useSearchStore()

const searchInputRef = ref()
const keyword = ref('')
const searchType = ref('all')
const loading = ref(false)
const hasSearched = ref(false)
const inputFocused = ref(false)
const typeDropOpen = ref(false)

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '题库', value: 'banks' },
  { label: '题目', value: 'questions' },
]

const totalCount = computed(() => {
  const b = searchType.value !== 'questions' ? (searchStore.bankResults?.length || 0) : 0
  const q = searchType.value !== 'banks' ? (searchStore.questionResults?.length || 0) : 0
  return b + q
})

function getTypeName(type) {
  const map = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }
  return map[type] || type
}

function getTypeTag(type) {
  const map = { single: undefined, multiple: 'success', judge: 'warning', fill: 'info', essay: 'danger' }
  return map[type]
}

function getDifficultyName(diff) {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[diff] || diff
}

function getDifficultyTag(diff) {
  return { easy: 'success', medium: 'warning', hard: 'danger' }[diff]
}

async function handleSearch() {
  if (!keyword.value.trim()) return

  loading.value = true
  hasSearched.value = true

  try {
    searchStore.bankResults.splice(0)
    searchStore.questionResults.splice(0)

    if (searchType.value === 'all' || searchType.value === 'banks') {
      await searchStore.searchBanks({ keyword: keyword.value })
    }
    if (searchType.value === 'all' || searchType.value === 'questions') {
      await searchStore.searchQuestions({ keyword: keyword.value })
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(() => route.query, (query) => {
  if (query.keyword) {
    keyword.value = query.keyword
    searchType.value = query.type || 'all'
    handleSearch()
  }
}, { immediate: true })

onMounted(async () => {
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    handleSearch()
  }
  // 页面挂载后自动聚焦输入框
  await nextTick()
  searchInputRef.value?.focus()
})
</script>

<style lang="scss" scoped>
// ── 页面容器 ──────────────────────────────────────────
.search-page {
  min-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

// ── 搜索英雄区 ────────────────────────────────────────
.search-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 20px 40px;
  transition: padding 0.3s ease;

  &--compact {
    padding: 24px 20px 20px;
  }
}

// ── 搜索框 ────────────────────────────────────────────
.search-box {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 620px;
  height: 48px;
  background: #fff;
  border: 1.5px solid #dcdfe6;
  border-radius: 24px;
  overflow: visible;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;

  &.is-focused {
    border-color: #409eff;
    box-shadow: 0 4px 24px rgba(64, 158, 255, 0.2);
  }
}

// 类型选择器
.type-selector {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 14px 0 18px;
  height: 100%;
  cursor: pointer;
  user-select: none;
  border-radius: 24px 0 0 24px;
  transition: background 0.15s;

  &:hover { background: #f5f7fa; }

  .type-label {
    font-size: 13px;
    font-weight: 600;
    color: #409eff;
    white-space: nowrap;
  }

  .type-caret {
    color: #409eff;
    opacity: 0.7;
    transition: transform 0.2s;
    &.open { transform: rotate(180deg); }
  }
}

// 下拉菜单
.type-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 100px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  list-style: none;
  margin: 0;
  z-index: 999;
  overflow: hidden;

  li {
    padding: 9px 18px;
    font-size: 13px;
    color: #606266;
    cursor: pointer;
    transition: background 0.12s, color 0.12s;

    &:hover { background: #f5f7fa; color: #303133; }
    &.active {
      color: #409eff;
      font-weight: 600;
      background: #ecf5ff;
    }
  }
}

// 下拉动画
.drop-enter-active, .drop-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.drop-enter-from, .drop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.divider {
  width: 1px;
  height: 18px;
  background: #e4e7ed;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 12px;
  font-size: 14px;
  color: #303133;
  font-family: inherit;

  &::placeholder { color: #c0c4cc; }
}

.search-btn {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 0 22px 22px 0;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: background 0.15s;
  margin: -1px -1px -1px 0;

  &:hover { background: #2d87f0; }
  &:active { background: #1a6fd4; }
}

// ── 未搜索提示 ────────────────────────────────────────
.search-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  padding-bottom: 80px;

  .hint-icon {
    font-size: 52px;
    margin-bottom: 14px;
    opacity: 0.4;
  }

  p {
    font-size: 14px;
    margin: 0;
  }
}

// ── 结果区域 ──────────────────────────────────────────
.result-area {
  padding: 0 4px;
}

.result-summary {
  font-size: 13px;
  color: #909399;
  margin-bottom: 20px;
  padding: 0 2px;

  .summary-keyword {
    color: #303133;
    font-weight: 600;
  }

  .summary-count {
    margin-left: 6px;
    color: #409eff;
  }

  .summary-empty {
    margin-left: 6px;
    color: #f56c6c;
  }
}

// ── 分区标签 ──────────────────────────────────────────
.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 14px;

  &:first-of-type { margin-top: 0; }

  .label-icon {
    width: 3px;
    height: 16px;
    border-radius: 2px;
    flex-shrink: 0;
  }

  .label-count {
    margin-left: 2px;
    font-size: 12px;
    font-weight: 400;
    color: #909399;
    background: #f2f4f8;
    border-radius: 10px;
    padding: 1px 8px;
  }
}

// ── 题库卡片（横向列表风格）────────────────────────────
.bank-grid { margin-bottom: 8px; }

.bank-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  margin-bottom: 10px;
  transition: background 0.15s, box-shadow 0.15s, transform 0.15s;

  &:hover {
    background: #f7fbff;
    box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
    transform: translateY(-1px);

    .bank-avatar { transform: scale(1.06); }
  }

  .bank-avatar-wrap { flex-shrink: 0; }

  .bank-avatar {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 3px 10px rgba(64, 158, 255, 0.3);
    transition: transform 0.2s;
  }

  .bank-info {
    flex: 1;
    min-width: 0;
  }

  .bank-name {
    font-weight: 600;
    font-size: 14px;
    color: #1a1a2e;
    margin-bottom: 3px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bank-desc {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 6px;
  }

  .bank-stats {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #b1b8c4;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }
  }

  .bank-arrow {
    flex-shrink: 0;
    color: #c0c4cc;
    font-size: 14px;
  }
}

// ── 题目列表 ──────────────────────────────────────────
.question-list {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  margin-bottom: 16px;

  .question-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 13px 16px;
    border-bottom: 1px solid #f2f4f8;
    cursor: pointer;
    transition: background 0.15s;

    &:last-child { border-bottom: none; }
    &:hover { background: #f7fbff; }

    .question-left {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;
      min-width: 0;
    }

    .type-tag { flex-shrink: 0; }

    .question-text {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .question-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    .diff-tag { flex-shrink: 0; }

    .bank-name-tag {
      display: flex;
      align-items: center;
      gap: 3px;
      font-size: 12px;
      color: #b1b8c4;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

</style>