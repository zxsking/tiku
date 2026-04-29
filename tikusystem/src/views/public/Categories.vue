<template>
  <div class="categories-page">
    <el-row :gutter="20">

      <!-- 左侧分类树 -->
      <el-col :span="6">
        <el-card shadow="never" class="side-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #e6a23c"></span>
              <span class="header-title">分类目录</span>
            </div>
          </template>

          <el-tree
            :data="categories"
            :props="{ label: 'name', children: 'children' }"
            :default-expanded-keys="expandedKeys"
            :highlight-current="true"
            node-key="id"
            class="category-tree"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon v-if="data.children?.length" class="node-icon folder-icon"><Folder /></el-icon>
                <el-icon v-else class="node-icon file-icon"><Collection /></el-icon>
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.count" class="node-count">{{ data.count }}</span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧题库内容 -->
      <el-col :span="18">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <el-breadcrumb separator="/" class="breadcrumb">
                  <el-breadcrumb-item :to="{ path: '/categories' }">全部分类</el-breadcrumb-item>
                  <el-breadcrumb-item v-if="currentCategory">{{ currentCategory.name }}</el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <el-tag v-if="total > 0" type="info" size="small" class="total-tag">共 {{ total }} 个题库</el-tag>
            </div>
          </template>

          <!-- 题库网格 -->
          <el-row :gutter="16">
            <el-col :span="8" v-for="bank in banks" :key="bank.id">
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
                    <span><el-icon><Star /></el-icon>{{ bank.favoriteCount }}</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 空状态 -->
          <el-empty v-if="banks.length === 0" description="该分类下暂无题库" class="empty-state" />

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="total > 0">
            <el-pagination
              v-model:current-page="page"
              :page-size="12"
              :total="total"
              background
              layout="prev, pager, next"
            />
          </div>
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCategoryStore } from '@/store/category'
import { useBankStore } from '@/store/bank'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const bankStore = useBankStore()

const categories = ref([])
const currentCategory = ref(null)
const expandedKeys = ref([])
const page = ref(1)
const total = ref(0)
const banks = ref([])

function handleNodeClick(data) {
  currentCategory.value = data
  if (data.id) {
    router.push({ path: '/categories', query: { id: data.id } })
  } else {
    router.push({ path: '/categories' })
  }
}

async function loadBanks(categoryId = null) {
  const params = { page: page.value, size: 12 }
  if (categoryId) {
    params.categoryId = categoryId
  }
  const data = await bankStore.fetchBanks(params)
  banks.value = data.records || []
  total.value = data.total || 0
}

// 递归在树中查找分类节点
function findCategory(nodes, id) {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findCategory(node.children, id)
      if (found) return found
    }
  }
  return null
}

watch(() => route.query.id, (id) => {
  if (id) {
    currentCategory.value = findCategory(categories.value, parseInt(id))
    loadBanks(parseInt(id))
  } else {
    currentCategory.value = null
    loadBanks()
  }
}, { immediate: true })

onMounted(async () => {
  const data = await categoryStore.fetchCategories()
  categories.value = data || []
  expandedKeys.value = categories.value.map(c => c.id)
  loadBanks(route.query.id ? parseInt(route.query.id) : null)
})
</script>

<style lang="scss" scoped>
// ── 布局 ──────────────────────────────────────────────
.categories-page {
  padding: 4px 0;
}

// ── 通用卡片 ──────────────────────────────────────────
%card-base {
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

.side-card {
  @extend %card-base;
  position: sticky;
  top: 20px;
}

.main-card {
  @extend %card-base;
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

.breadcrumb {
  font-size: 15px;
  font-weight: 600;

  :deep(.el-breadcrumb__inner) {
    font-weight: 600;
    color: #303133;
  }

  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: #409eff;
  }
}

.total-tag {
  border-radius: 20px;
  :deep(.el-tag__content) { color: #909399; }
}

// ── 分类树 ────────────────────────────────────────────
.category-tree {
  :deep(.el-tree-node__content) {
    height: 38px;
    border-radius: 8px;
    margin-bottom: 2px;
    padding-right: 8px;

    &:hover {
      background: #f0f6ff;
    }
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: #e8f4ff;
    color: #409eff;

    .node-icon { color: #409eff; }
    .node-label { color: #409eff; font-weight: 600; }
  }
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  overflow: hidden;
}

.node-icon {
  font-size: 14px;
  flex-shrink: 0;

  &.folder-icon { color: #e6a23c; }
  &.file-icon   { color: #909399; }
}

.node-label {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.node-count {
  font-size: 11px;
  color: #b1b8c4;
  background: #f2f4f8;
  padding: 1px 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

// ── 题库卡片 ──────────────────────────────────────────
.bank-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.09);

    .bank-avatar { transform: scale(1.05); }
  }

  .bank-cover {
    height: 72px;
    background: linear-gradient(135deg, #e8f4ff, #dbeeff);
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
    gap: 14px;
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

// ── 空状态 / 分页 ─────────────────────────────────────
.empty-state {
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #edf0f5;
}
</style>