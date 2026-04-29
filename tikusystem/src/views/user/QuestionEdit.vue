<template>
  <div class="question-edit-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-dot" style="background: #67c23a"></span>
            <span class="header-title">{{ isEdit ? '编辑题目' : '添加题目' }}</span>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="edit-form">
        <el-form-item label="所属题库" prop="bankId">
          <el-select
            v-model="form.bankId"
            placeholder="请选择题库"
            filterable
            clearable
            style="width: 360px"
          >
            <el-option
              v-for="bank in myBanks"
              :key="bank.id"
              :label="bankLabel(bank)"
              :value="bank.id"
            />
          </el-select>
          <div v-if="!loadingBanks && !myBanks.length" class="bank-empty-hint">暂无题库，请先创建题库后再添加题目</div>
        </el-form-item>

        <el-form-item label="题目类型" prop="type">
          <el-radio-group v-model="form.type" @change="handleTypeChange" class="type-radio-group">
            <el-radio value="single">单选题</el-radio>
            <el-radio value="multiple">多选题</el-radio>
            <el-radio value="judge">判断题</el-radio>
            <el-radio value="fill">填空题</el-radio>
            <el-radio value="essay">简答题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>

        <template v-if="['single', 'multiple'].includes(form.type)">
          <el-form-item label="选项" prop="options">
            <div class="options-wrapper">
              <div v-for="(option, index) in form.options" :key="index" class="option-row">
                <div class="option-label">{{ String.fromCharCode(65 + index) }}</div>
                <el-input v-model="form.options[index]" :placeholder="`选项 ${String.fromCharCode(65 + index)}`" class="option-input" />
                <button type="button" v-if="form.options.length > 2" class="remove-option-btn" @click="removeOption(index)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
              <button type="button" v-if="form.options.length < 6" class="add-option-btn" @click="addOption">
                <el-icon><Plus /></el-icon>添加选项
              </button>
            </div>
          </el-form-item>

          <el-form-item label="正确答案" prop="answer">
            <el-checkbox-group v-if="form.type === 'multiple'" v-model="form.answer" class="answer-group">
              <el-checkbox v-for="(opt, idx) in form.options" :key="idx" :value="idx" class="answer-checkbox">
                {{ String.fromCharCode(65 + idx) }}
              </el-checkbox>
            </el-checkbox-group>
            <el-radio-group v-else v-model="form.answer" class="answer-group">
              <el-radio v-for="(opt, idx) in form.options" :key="idx" :value="idx">
                {{ String.fromCharCode(65 + idx) }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-if="form.type === 'judge'">
          <el-form-item label="正确答案" prop="answer">
            <el-radio-group v-model="form.answer" class="answer-group">
              <el-radio :value="true">正确</el-radio>
              <el-radio :value="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-if="form.type === 'fill'">
          <el-form-item label="正确答案" prop="answer">
            <el-input v-model="form.answer" placeholder="多个空用 | 分隔，如：答案 1|答案 2" />
          </el-form-item>
        </template>

        <template v-if="form.type === 'essay'">
          <el-form-item label="参考答案" prop="answer">
            <el-input v-model="form.answer" type="textarea" :rows="4" placeholder="请输入参考答案" />
          </el-form-item>
        </template>

        <el-form-item label="难度等级" prop="difficulty">
          <el-radio-group v-model="form.difficulty" class="difficulty-group">
            <el-radio value="easy">简单</el-radio>
            <el-radio value="medium">中等</el-radio>
            <el-radio value="hard">困难</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="请输入答案解析（可选）" />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">提交</button>
            <button type="button" class="cancel-btn" @click="goBack()">取消</button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useQuestionStore } from '@/store/question'
import { useBankStore } from '@/store/bank'

const route = useRoute()
const router = useRouter()
const questionStore = useQuestionStore()
const bankStore = useBankStore()

const isEdit = computed(() => !!route.params.id)
const formRef = ref()
const loading = ref(false)
const loadingBanks = ref(true)

const form = reactive({
  bankId: null,
  type: 'single',
  content: '',
  options: ['', '', '', ''],
  answer: null,
  difficulty: 'medium',
  analysis: ''
})

const rules = {
  bankId: [
    {
      validator(_rule, value, callback) {
        if (value === null || value === undefined || value === '' || Number.isNaN(Number(value))) {
          callback(new Error('请选择题库'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请设置正确答案', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const myBanks = ref([])

function bankLabel(bank) {
  const vis = bank.visibility === 'public' ? '公开' : '私有'
  const st = bank.status === 'published' ? '已发布' : bank.status === 'pending' ? '审核中' : (bank.status || '')
  const extra = st ? `（${vis} · ${st}）` : `（${vis}）`
  return `${bank.name || '未命名'}${extra}`
}

/** 拉取当前用户全部题库（分页累加），避免默认每页 10 条导致下拉框缺题库 */
async function loadAllMyBanks() {
  loadingBanks.value = true
  const all = []
  let page = 1
  const size = 100
  try {
    while (true) {
      const data = await bankStore.fetchMyBanks({ page, size })
      const records = data?.records || data?.list || []
      all.push(...records)
      const total = data?.total ?? 0
      if (records.length < size || all.length >= total) break
      page += 1
    }
    myBanks.value = all
  } finally {
    loadingBanks.value = false
  }
}

function handleTypeChange() {
  form.options = ['', '', '', '']
  form.answer = null
}

function addOption() {
  form.options.push('')
}

function removeOption(index) {
  form.options.splice(index, 1)
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const bankId = Number(form.bankId)
    const payload = {
      bankId,
      type: form.type,
      content: form.content,
      options: form.options,
      answer: form.answer,
      difficulty: form.difficulty,
      analysis: form.analysis || undefined
    }
    if (isEdit.value) {
      await questionStore.updateQuestion(route.params.id, payload)
    } else {
      await questionStore.createQuestion(payload)
    }
    ElMessage.success('保存成功')
    goBack()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}


function goBack() {
  const raw = form.bankId ?? route.query.bankId
  const bankId = raw !== '' && raw !== undefined && raw !== null ? Number(raw) : NaN
  if (!Number.isNaN(bankId)) {
    router.push(`/banks/${bankId}`)
  } else {
    router.push('/user/questions')
  }
}

onMounted(async () => {
  await loadAllMyBanks()

  // 从题库详情页跳转时预填 bankId（与 el-option 的 number 类型一致）
  const q = route.query.bankId
  if (q !== undefined && q !== null && q !== '') {
    const id = Number(q)
    if (!Number.isNaN(id)) {
      form.bankId = id
    }
  }

  if (isEdit.value) {
    loading.value = true
    try {
      const question = await questionStore.fetchQuestion(route.params.id)
      Object.assign(form, {
        bankId: question.bankId != null ? Number(question.bankId) : null,
        type: question.type,
        content: question.content,
        options: (question.options || ['', '', '', '']).map(o =>
          typeof o === 'string' ? o : (o?.value ?? o?.text ?? o?.content ?? '')
        ),
        answer: question.answer,
        difficulty: question.difficulty,
        analysis: question.analysis
      })
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }
})
</script>

<style lang="scss" scoped>
.question-edit-page {
  padding: 4px 0;
  max-width: 900px;
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

.bank-empty-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #e6a23c;
}

.edit-form {
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

.type-radio-group {
  :deep(.el-radio) {
    margin-right: 20px;
    margin-bottom: 8px;
  }
}

.options-wrapper {
  width: 100%;

  .option-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;

    .option-label {
      width: 28px;
      height: 28px;
      border-radius: 8px;
      background: linear-gradient(135deg, #409eff, #2d87f0);
      color: #fff;
      font-size: 14px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .option-input {
      flex: 1;
    }

    .remove-option-btn {
      width: 32px;
      height: 32px;
      border-radius: 6px;
      background: #fff2f2;
      border: 1px solid #fde0e0;
      color: #f56c6c;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      flex-shrink: 0;
      transition: all 0.15s;

      &:hover {
        background: #fde0e0;
      }
    }
  }

  .add-option-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 36px;
    padding: 0 16px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    border: 1px dashed #c6e0ff;
    background: #f0f6ff;
    color: #409eff;
    transition: all 0.15s;

    &:hover {
      background: #e0efff;
      border-color: #409eff;
    }
  }
}

.answer-group {
  :deep(.el-checkbox) {
    margin-right: 16px;
  }

  :deep(.el-radio) {
    margin-right: 16px;
  }
}

.difficulty-group {
  :deep(.el-radio) {
    margin-right: 16px;
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