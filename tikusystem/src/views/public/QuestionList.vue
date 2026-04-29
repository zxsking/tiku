<template>
  <div class="question-list-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #67c23a"></span>
            <span class="header-title">题目广场</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ total }} 道</el-tag>
          </div>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索题目内容..."
          class="filter-search"
          clearable
          @keyup.enter="loadQuestions"
          @clear="loadQuestions"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="filter-right">
          <el-select v-model="filters.sortBy" style="width: 130px" @change="onSortChange">
            <el-option label="最新更新" value="latest" />
            <el-option label="最热浏览" value="hot" />
          </el-select>
          <el-select v-model="filters.type" placeholder="全部题型" clearable style="width: 120px" @change="loadQuestions">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="简答题" value="essay" />
          </el-select>
          <el-select v-model="filters.difficulty" placeholder="全部难度" clearable style="width: 120px" @change="loadQuestions">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </div>
      </div>

      <!-- 题目列表 -->
      <div v-loading="loading" class="question-list">
        <div
          v-for="q in questions"
          :key="q.id"
          class="question-item"
          @click="$router.push(`/questions/${q.id}`)"
        >
          <div class="question-main">
            <!-- 题型 + 难度标签 -->
            <div class="question-tags">
              <el-tag :type="typeTagColor(q.type)" size="small" class="type-tag">
                {{ typeName(q.type) }}
              </el-tag>
              <el-tag :type="difficultyTagColor(q.difficulty)" size="small" effect="plain" class="diff-tag">
                {{ difficultyName(q.difficulty) }}
              </el-tag>
            </div>
            <!-- 题目内容 -->
            <div class="question-content">{{ q.content }}</div>
            <!-- 选项预览（仅单选/多选） -->
            <div v-if="q.options?.length" class="question-options">
              <span
                v-for="(opt, idx) in q.options.slice(0, 4)"
                :key="idx"
                class="option-item"
              >
                <span class="option-label">{{ String.fromCharCode(65 + idx) }}.</span>
                {{ getOptionText(opt) }}
              </span>
            </div>
          </div>
          <div class="question-meta">
            <span class="meta-bank" @click.stop="$router.push(`/banks/${q.bankId}`)">
              <el-icon><Collection /></el-icon>{{ q.bankName || '未知题库' }}
            </span>
            <span class="meta-stat" title="浏览量"><el-icon><View /></el-icon>{{ q.viewCount ?? 0 }}</span>
            <span class="meta-stat" title="点赞数"><el-icon><Pointer /></el-icon>{{ q.likeCount ?? 0 }}</span>
            <span class="meta-arrow"><el-icon><ArrowRight /></el-icon></span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !questions.length" description="暂无题目" class="empty-state" />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[15, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useQuestionStore } from '@/store/question'

const questionStore = useQuestionStore()

const loading = ref(false)
const questions = ref([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  type: '',
  difficulty: '',
  sortBy: 'latest',
})

const pagination = reactive({
  page: 1,
  pageSize: 15,
})

function onSortChange() {
  pagination.page = 1
  loadQuestions()
}

async function loadQuestions() {
  loading.value = true
  try {
    const data = await questionStore.fetchQuestions({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      difficulty: filters.difficulty || undefined,
      sortBy: filters.sortBy || 'latest',
    })
    questions.value = data?.records || data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadQuestions()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadQuestions()
}

// ── 工具函数 ──────────────────────────────────────────
function getOptionText(opt) {
  if (typeof opt === 'string') return opt
  if (opt && typeof opt === 'object') {
    return opt.value ?? opt.text ?? opt.content ?? JSON.stringify(opt)
  }
  return String(opt)
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

onMounted(loadQuestions)
</script>

<style lang="scss" scoped>
.question-list-page {
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
    max-width: 360px;

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

// ── 题目列表 ──────────────────────────────────────────
.question-list {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  cursor: pointer;
  background: #fff;
  transition: box-shadow 0.2s, border-color 0.2s, transform 0.15s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
    transform: translateY(-1px);

    .meta-arrow {
      color: #409eff;
      transform: translateX(3px);
    }
  }
}

.question-main {
  flex: 1;
  min-width: 0;
}

.question-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;

  .type-tag, .diff-tag {
    border-radius: 6px;
    font-size: 11px;
  }
}

.question-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;

  .option-item {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 200px;

    .option-label {
      font-weight: 600;
      color: #606266;
      margin-right: 3px;
    }
  }
}

.question-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  min-width: 120px;
}

.meta-bank {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

.meta-stat {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #b1b8c4;
}

.meta-arrow {
  color: #c0c4cc;
  font-size: 14px;
  transition: color 0.2s, transform 0.2s;
}

// ── 空状态 ────────────────────────────────────────────
.empty-state {
  padding: 60px 0;
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
