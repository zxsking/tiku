<template>
  <div class="bank-list-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">题库列表</span>
            <el-tag type="info" size="small" class="total-tag">共 {{ bankStore.total }} 个</el-tag>
          </div>
          <el-button v-if="userStore.isLoggedIn" type="primary" class="create-btn" @click="$router.push('/user/banks/create')">
            <el-icon><Plus /></el-icon>创建题库
          </el-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索题库..."
          class="filter-search"
          clearable
          @keyup.enter="loadBanks"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="filter-right">
          <el-select v-model="filters.categoryId" placeholder="全部分类" clearable style="width: 130px" @change="loadBanks">
            <el-option v-for="cat in categoryStore.categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
          <el-select v-model="filters.sortBy" style="width: 110px" @change="loadBanks">
            <el-option label="最新发布" value="latest" />
            <el-option label="最热门" value="popular" />
            <el-option label="收藏最多" value="favorites" />
          </el-select>
        </div>
      </div>

      <!-- 题库网格 -->
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="bank in bankStore.banks" :key="bank.id">
          <div class="bank-card" @click="$router.push(`/banks/${bank.id}`)">
            <!-- 封面 -->
            <div class="bank-cover">
              <div class="bank-avatar">{{ bank.name.charAt(0) }}</div>
              <div class="bank-cover-bg"></div>
            </div>
            <!-- 内容 -->
            <div class="bank-body">
              <div class="bank-name">{{ bank.name }}</div>
              <div class="bank-author">
                <el-icon><User /></el-icon>
                <span
                  class="author-link"
                  @click.stop="$router.push(`/users/${bank.authorId}`)"
                >{{ bank.author }}</span>
              </div>
              <div class="bank-desc">{{ bank.description }}</div>
              <div class="bank-footer">
                <el-tag size="small" class="category-tag">{{ bank.categoryName }}</el-tag>
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

      <!-- 空状态 -->
      <el-empty v-if="!bankStore.banks?.length" description="暂无题库" class="empty-state" />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="bankStore.total"
          :page-sizes="[12, 24, 48]"
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
import { useUserStore } from '@/store/user'
import { useBankStore } from '@/store/bank'
import { useCategoryStore } from '@/store/category'

const userStore = useUserStore()
const bankStore = useBankStore()
const categoryStore = useCategoryStore()

const filters = reactive({
  categoryId: '',
  sortBy: 'latest',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 12
})

async function loadBanks(resetPage = true) {
  if (resetPage) pagination.page = 1
  const params = { page: pagination.page, pageSize: pagination.pageSize }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.sortBy) params.sortBy = filters.sortBy
  await bankStore.fetchBanks(params)
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadBanks(false)
}

function handleCurrentChange(page) {
  pagination.page = page
  loadBanks(false)
}

onMounted(async () => {
  await Promise.all([loadBanks(), categoryStore.fetchCategories()])
})
</script>

<style lang="scss" scoped>
// ── 页面容器 ──────────────────────────────────────────
.bank-list-page {
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
  border-radius: 8px;
  font-weight: 500;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);

  &:hover {
    opacity: 0.88;
    box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
  }
}

// ── 筛选栏 ────────────────────────────────────────────
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 10px;
  border: 1px solid #edf0f5;

  .filter-search {
    flex: 1;
    max-width: 320px;

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

// ── 题库卡片 ──────────────────────────────────────────
.bank-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 20px;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.09);

    .bank-avatar {
      transform: scale(1.05);
    }
  }

  // 封面区域
  .bank-cover {
    height: 80px;
    background: linear-gradient(135deg, #e8f4ff 0%, #dbeeff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }

  .bank-avatar {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    font-size: 22px;
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
    background: radial-gradient(circle at 70% 50%, rgba(64, 158, 255, 0.08) 0%, transparent 70%);
  }

  // 内容区域
  .bank-body {
    padding: 14px;
  }

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

  .bank-author {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #b1b8c4;
    font-size: 12px;
    margin-bottom: 8px;

    .author-link {
      cursor: pointer;
      &:hover { color: #409eff; text-decoration: underline; }
    }
  }

  .bank-desc {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
    margin-bottom: 12px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 38px;
  }

  .bank-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .category-tag {
    border-radius: 6px;
    font-size: 11px;
  }

  .bank-stats {
    display: flex;
    gap: 10px;
    color: #b1b8c4;
    font-size: 12px;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }
  }
}

// ── 空状态 ────────────────────────────────────────────
.empty-state {
  padding: 60px 0;
}

// ── 分页 ──────────────────────────────────────────────
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}
</style>