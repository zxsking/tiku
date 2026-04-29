<template>
  <div class="bank-edit-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #409eff"></span>
            <span class="header-title">{{ isEdit ? '编辑题库' : '创建题库' }}</span>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="edit-form">
        <el-form-item label="题库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入题库名称" />
        </el-form-item>

        <el-form-item label="所属分类" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryStore.categories"
            :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: true }"
            placeholder="请选择分类"
            style="width: 100%"
            clearable
          />
        </el-form-item>

        <el-form-item label="题库描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入题库描述" />
        </el-form-item>

        <el-form-item label="可见性" prop="visibility">
          <el-radio-group v-model="form.visibility" class="visibility-group">
            <el-radio value="public">公开</el-radio>
            <el-radio value="private">私有</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <button class="submit-btn" :disabled="loading" @click="handleSubmit">
              <span v-if="loading">提交中...</span>
              <span v-else>提交</span>
            </button>
            <button class="cancel-btn" @click="$router.push('/user/banks')">取消</button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBankStore } from '@/store/bank'
import { useCategoryStore } from '@/store/category'

const route = useRoute()
const router = useRouter()
const bankStore = useBankStore()
const categoryStore = useCategoryStore()

const isEdit = computed(() => !!route.params.id)
const formRef = ref()
const loading = ref(false)

const form = reactive({
  name: '',
  categoryId: '',
  description: '',
  visibility: 'public'
})

const rules = {
  name: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入题库描述', trigger: 'blur' }]
}

async function handleSubmit() {
  // 表单校验
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完整填写必填项')
    return
  }

  loading.value = true

  try {
    let result
    if (isEdit.value) {
      result = await bankStore.updateBank(route.params.id, form)
    } else {
      result = await bankStore.createBank(form)
    }

    // 根据返回结果判断是否成功
    if (result && (result.code === 0 || result.success || result.id)) {
      const successMsg = form.visibility === 'private'
        ? '保存成功'
        : '已提交审核，审核通过后将对外公开'
      ElMessage.success(successMsg)
      router.push('/user/banks')
    } else {
      // 返回了结果但业务状态异常
      const msg = result?.message || result?.msg || (isEdit.value ? '修改失败，请重试' : '创建失败，请重试')
      ElMessage.error(msg)
    }
  } catch (error) {
    // 网络错误或接口抛出异常
    const msg = error?.response?.data?.message
      || error?.response?.data?.msg
      || error?.message
      || (isEdit.value ? '修改失败，请检查网络后重试' : '创建失败，请检查网络后重试')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}


onMounted(async () => {
  try {
    await categoryStore.fetchCategories()
  } catch {
    ElMessage.warning('分类加载失败，请刷新页面重试')
  }

  if (isEdit.value) {
    loading.value = true
    try {
      const bank = await bankStore.fetchBank(route.params.id)
      if (bank) {
        Object.assign(form, {
          name: bank.name,
          categoryId: bank.categoryId,
          description: bank.description,
          visibility: bank.visibility
        })
      } else {
        ElMessage.error('题库不存在或已被删除')
        router.push('/user/banks')
      }
    } catch (error) {
      ElMessage.error(error?.message || '题库信息加载失败，请刷新页面重试')
    } finally {
      loading.value = false
    }
  }
})
</script>

<style lang="scss" scoped>
.bank-edit-page {
  padding: 4px 0;
  max-width: 800px;
  margin: 0 auto;
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
    padding: 24px;
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

.edit-form {
  max-width: 600px;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }

  :deep(.el-textarea__inner) {
    border-radius: 8px;
  }

  :deep(.el-select .el-input__wrapper) {
    border-radius: 8px;
  }
}

.visibility-group {
  :deep(.el-radio) {
    margin-right: 20px;
  }
}

.form-actions {
  display: flex;
  gap: 10px;

  .submit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 40px;
    padding: 0 24px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    border: none;
    background: linear-gradient(135deg, #409eff, #2d87f0);
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    transition: opacity 0.2s, transform 0.15s;

    &:hover { opacity: 0.88; }
    &:active { transform: scale(0.97); }
  }

  .cancel-btn {
    height: 40px;
    padding: 0 24px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    border: 1px solid #dcdfe6;
    background: #fff;
    color: #606266;
    transition: all 0.15s;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
  }
}
</style>