<template>
  <div class="category-manage-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #67c23a"></span>
            <span class="header-title">分类管理</span>
          </div>
          <button class="create-btn" @click="showAddDialog">
            <el-icon><Plus /></el-icon> 添加分类
          </button>
        </div>
      </template>

      <el-table :data="categories" row-key="id" class="category-table" :row-class-name="() => 'category-row'">
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="count" label="题库数量" width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small" class="count-tag">{{ row.count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <div class="action-group">
              <button class="op-btn op-btn--edit" @click="showEditDialog(row)">编辑</button>
              <button class="op-btn op-btn--add" @click="showAddChildDialog(row)">添加子分类</button>
              <el-popconfirm title="确定删除该分类吗？" @confirm="deleteCategory(row.id)">
                <template #reference>
                  <button class="op-btn op-btn--delete">删除</button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : (categoryForm.parentId ? '添加子分类' : '添加分类')" width="420px" class="category-dialog">
      <el-form :model="categoryForm" label-width="80px" class="category-form">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select
            v-model="categoryForm.parentId"
            placeholder="无（根分类）"
            clearable
            style="width: 100%"
            :disabled="!isEdit && categoryForm.parentId !== null"
          >
            <el-option
              v-for="cat in parentOptions"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-btn dialog-btn--default" @click="dialogVisible = false">取消</button>
        <button class="dialog-btn dialog-btn--primary" @click="saveCategory">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory as deleteCategoryApi
} from '@/api/category'

const dialogVisible = ref(false)
const isEdit = ref(false)

const categories = ref([])

const categoryForm = reactive({
  id: null,
  name: '',
  parentId: null,
  sort: 0
})

// 将树形分类展开为平铺列表，用于父分类选择器
function flattenCategories(list, result = []) {
  for (const cat of list) {
    result.push(cat)
    if (cat.children && cat.children.length) {
      flattenCategories(cat.children, result)
    }
  }
  return result
}

// 父分类候选项：排除自身，防止循环引用
const parentOptions = computed(() =>
  flattenCategories(categories.value).filter(c => c.id !== categoryForm.id)
)

function showAddDialog() {
  isEdit.value = false
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.parentId = null
  categoryForm.sort = 0
  dialogVisible.value = true
}

function showAddChildDialog(parent) {
  isEdit.value = false
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.parentId = parent.id
  categoryForm.sort = 0
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  categoryForm.id = row.id
  categoryForm.name = row.name
  categoryForm.parentId = row.parentId ?? null
  categoryForm.sort = row.sort
  dialogVisible.value = true
}

async function saveCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  const payload = {
    name: categoryForm.name.trim(),
    parentId: categoryForm.parentId,
    sort: categoryForm.sort
  }
  if (isEdit.value) {
    await updateCategory(categoryForm.id, payload)
  } else {
    await createCategory(payload)

  }
  await loadCategories()
  ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
  
  dialogVisible.value = false
  location.reload(true);
}

async function deleteCategory(id) {
  await deleteCategoryApi(id)
  await loadCategories()
  ElMessage.success('删除成功')
}

async function loadCategories() {
  const data = await getCategories()
  categories.value = data || []
}

onMounted(loadCategories)
</script>

<style lang="scss" scoped>
.category-manage-page {
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

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, #67c23a, #52a827);
  color: #fff;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
  transition: opacity 0.2s, transform 0.15s;

  &:hover { opacity: 0.88; }
  &:active { transform: scale(0.97); }
}

.category-table {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-table__header-wrapper th) {
    background: #f7f9fc;
    color: #909399;
    font-weight: 500;
    font-size: 13px;
  }

  :deep(.el-table__row) {
    &:hover > td {
      background: #f7f9fc !important;
    }
  }
}

.count-tag {
  border-radius: 6px;
}

.action-group {
  display: flex;
  gap: 4px;
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

  &--edit {
    background: #f0f6ff;
    color: #409eff;
    &:hover { background: #dbeeff; }
  }

  &--add {
    background: #f0faf0;
    color: #67c23a;
    &:hover { background: #d9f0d9; }
  }

  &--delete {
    background: #fff2f2;
    color: #f56c6c;
    &:hover { background: #fde0e0; }
  }
}

.category-dialog {
  :deep(.el-dialog) {
    border-radius: 12px;
  }

  :deep(.el-dialog__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #edf0f5;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #edf0f5;
  }
}

.category-form {
  :deep(.el-form-item__label) {
    color: #606266;
  }
}

.dialog-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.15s;

  &--default {
    background: #fff;
    border: 1px solid #dcdfe6;
    color: #606266;
    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
  }

  &--primary {
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    &:hover { opacity: 0.88; }
  }
}
</style>