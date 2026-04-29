<template>
  <div class="practice-start-page">
    <div class="page-title">
      <el-icon><VideoPlay /></el-icon>
      练习中心
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="never" class="start-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <span class="header-title">
                  {{ bankInfo ? `练习题库：${bankInfo.name}` : '开始练习' }}
                </span>
              </div>
              <el-button
                link
                type="primary"
                size="small"
                @click="$router.push('/user/practice/history')"
              >
                <el-icon><List /></el-icon>历史记录
              </el-button>
            </div>
          </template>

          <el-form :model="form" label-width="100px" class="start-form">

            <!-- 无 bankId 时显示题库选择 -->
            <el-form-item v-if="!route.params.bankId" label="选择题库">
              <el-select
                v-model="form.bankId"
                placeholder="全部题库（随机出题）"
                clearable
                filterable
                style="width: 320px"
                :loading="banksLoading"
                @focus="loadBanks"
              >
                <el-option
                  v-for="b in bankOptions"
                  :key="b.id"
                  :label="b.name"
                  :value="b.id"
                />
              </el-select>
              <span class="field-hint">不选则从所有题库中随机出题</span>
            </el-form-item>

            <el-form-item label="题目数量">
              <el-slider
                v-model="form.count"
                :min="5"
                :max="maxCount"
                :step="5"
                show-input
                :input-size="'small'"
                style="width: 360px"
              />
            </el-form-item>

            <el-form-item label="时间限制">
              <el-radio-group v-model="form.timeLimitMode">
                <el-radio value="none">不限时</el-radio>
                <el-radio value="custom">自定义</el-radio>
              </el-radio-group>
              <div v-if="form.timeLimitMode === 'custom'" class="time-input-wrap">
                <el-input-number
                  v-model="form.timeLimit"
                  :min="1"
                  :max="180"
                  controls-position="right"
                  style="width: 120px; margin-left: 12px"
                />
                <span class="unit-label">分钟</span>
              </div>
            </el-form-item>

            <el-form-item label="题目类型">
              <el-checkbox-group v-model="form.types">
                <el-checkbox value="single">单选题</el-checkbox>
                <el-checkbox value="multiple">多选题</el-checkbox>
                <el-checkbox value="judge">判断题</el-checkbox>
                <el-checkbox value="fill">填空题</el-checkbox>
                <el-checkbox value="essay">简答题</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="难度">
              <el-checkbox-group v-model="form.difficulties">
                <el-checkbox value="easy">简单</el-checkbox>
                <el-checkbox value="medium">中等</el-checkbox>
                <el-checkbox value="hard">困难</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="出题顺序">
              <el-radio-group v-model="form.shuffle">
                <el-radio :value="false">按原顺序</el-radio>
                <el-radio :value="true">随机打乱</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <button class="start-btn" :disabled="practiceStore.loading" @click="handleStart">
                <el-icon><VideoPlay /></el-icon>
                {{ practiceStore.loading ? '创建中...' : '开始练习' }}
              </button>
              <button class="cancel-btn" @click="$router.back()">取消</button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧 -->
      <el-col :span="8">
        <!-- 题库信息（有 bankId 时显示） -->
        <el-card v-if="bankInfo" shadow="never" class="bank-info-card">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #409eff"></span>
              <span class="header-title">题库信息</span>
            </div>
          </template>
          <div class="bank-info-list">
            <div class="info-row">
              <span class="info-label">题库名称</span>
              <span class="info-val">{{ bankInfo.name }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">题目总数</span>
              <span class="info-val primary">{{ bankInfo.questionCount }} 题</span>
            </div>
            <div class="info-row" v-if="bankInfo.categoryName">
              <span class="info-label">分类</span>
              <span class="info-val">{{ bankInfo.categoryName }}</span>
            </div>
            <div class="info-row" v-if="bankInfo.description">
              <span class="info-label">描述</span>
              <span class="info-val desc">{{ bankInfo.description }}</span>
            </div>
          </div>
        </el-card>

        <!-- 练习说明 -->
        <el-card shadow="never" class="tips-card" :style="bankInfo ? 'margin-top: 16px' : ''">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #e6a23c"></span>
              <span class="header-title">练习说明</span>
            </div>
          </template>
          <ul class="tips-list">
            <li>客观题（单选/多选/判断）将自动判分</li>
            <li>提交后可查看每题解析</li>
            <li>答错的题目会自动加入错题本</li>
            <li>答题过程中可标记题目稍后复查</li>
            <li>页面刷新不会丢失答题进度</li>
          </ul>
        </el-card>

        <!-- 快捷入口 -->
        <el-card shadow="never" class="quick-card" style="margin-top: 16px">
          <template #header>
            <div class="header-left">
              <span class="header-dot" style="background: #67c23a"></span>
              <span class="header-title">快捷入口</span>
            </div>
          </template>
          <div class="quick-links">
            <div class="quick-link" @click="$router.push('/user/wrong-book')">
              <el-icon color="#f56c6c"><WarningFilled /></el-icon>
              <span>错题本</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
            <div class="quick-link" @click="$router.push('/user/practice/history')">
              <el-icon color="#409eff"><List /></el-icon>
              <span>历史记录</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, List, WarningFilled, ArrowRight } from '@element-plus/icons-vue'
import { usePracticeStore } from '@/store/practice'
import { useBankStore } from '@/store/bank'

const router = useRouter()
const route = useRoute()
const practiceStore = usePracticeStore()
const bankStore = useBankStore()

const bankInfo = ref(null)
const bankOptions = ref([])
const banksLoading = ref(false)
const maxCount = ref(50)

const form = reactive({
  bankId: null,
  count: 20,
  timeLimitMode: 'none',
  timeLimit: 30,
  types: ['single', 'multiple', 'judge'],
  difficulties: ['easy', 'medium', 'hard'],
  shuffle: true
})

async function loadBanks() {
  banksLoading.value = true
  try {
    // 公共题库（已发布）+ 我的题库（包含私有），合并去重
    const [publicData, myData] = await Promise.all([
      bankStore.fetchBanks({ page: 1, size: 100 }),
      bankStore.fetchMyBanks({ page: 1, size: 100 })
    ])

    const publicList = publicData.records || publicData.list || []
    const myListRaw = Array.isArray(myData)
      ? myData
      : (myData?.records || myData?.list || [])

    const map = new Map()
    // 先放我的题库，保证私有题库优先展示
    for (const b of myListRaw) {
      if (!b || !b.id) continue
      map.set(b.id, b)
    }
    // 再合并公共题库
    for (const b of publicList) {
      if (!b || !b.id) continue
      if (!map.has(b.id)) map.set(b.id, b)
    }

    bankOptions.value = Array.from(map.values())
  } finally {
    banksLoading.value = false
  }
}

onMounted(async () => {
  const bankId = route.params.bankId
  if (bankId) {
    form.bankId = bankId
    try {
      const data = await bankStore.fetchBank(bankId)
      bankInfo.value = data
      maxCount.value = Math.min(data.questionCount || 50, 100)
      form.count = Math.min(20, maxCount.value)
    } catch {
      // 忽略加载失败
    }
  }
})

async function handleStart() {
  if (form.types.length === 0) {
    ElMessage.warning('请至少选择一种题目类型')
    return
  }
  if (form.difficulties.length === 0) {
    ElMessage.warning('请至少选择一种难度')
    return
  }

  try {
    const payload = {
      bankId: form.bankId || null,
      count: form.count,
      timeLimit: form.timeLimitMode === 'custom' ? form.timeLimit * 60 : 0,
      types: form.types,
      difficulties: form.difficulties,
      shuffle: form.shuffle
    }
    const session = await practiceStore.startSession(payload)
    router.push(`/user/practice/exam/${session.id}`)
  } catch (e) {
    ElMessage.error(e?.message || '创建练习失败，请稍后重试')
  }
}
</script>

<style lang="scss" scoped>
.practice-start-page {
  padding: 4px 0;
}

.start-card,
.tips-card,
.bank-info-card,
.quick-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f5;
    background: #fafbfd;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.start-form {
  padding: 8px 0 0;
}

.field-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #b1b8c4;
}

.time-input-wrap {
  display: inline-flex;
  align-items: center;
}

.unit-label {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 24px;
  background: linear-gradient(135deg, #409eff, #2d87f0);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: opacity 0.2s, transform 0.15s;

  &:hover { opacity: 0.88; }
  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.cancel-btn {
  display: inline-flex;
  align-items: center;
  height: 38px;
  padding: 0 20px;
  margin-left: 12px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s;

  &:hover { border-color: #409eff; color: #409eff; }
}

.tips-list {
  padding-left: 20px;
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 2.2;
}

.bank-info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid #edf0f5;
  font-size: 14px;

  &:last-child { border-bottom: none; }
}

.info-label {
  color: #909399;
  flex-shrink: 0;
  margin-right: 12px;
}

.info-val {
  color: #303133;
  font-weight: 500;
  text-align: right;

  &.primary { color: #409eff; }
  &.desc {
    font-weight: 400;
    color: #606266;
    max-width: 160px;
  }
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  transition: background 0.15s;

  &:hover { background: #f7f9fc; }

  .arrow {
    margin-left: auto;
    color: #b1b8c4;
  }
}
</style>
