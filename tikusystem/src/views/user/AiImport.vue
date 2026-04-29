<template>
  <div class="ai-import-page">
    <el-card shadow="never" class="main-card">




      <!-- ======== Step 1：输入 ======== -->
      <div v-if="step === 0" class="step-content">
        <el-tabs v-model="inputTab" class="input-tabs">
          <el-tab-pane label="粘贴文本" name="text">
            <div class="tab-tip">将题目文字直接粘贴到下方文本框中，支持多道题目</div>
            <el-input
              v-model="textInput"
              type="textarea"
              :rows="14"
              placeholder="例如：
1. 下列哪个是 Java 的基本数据类型？
A. String  B. int  C. ArrayList  D. HashMap
答案：B

2. 判断题：Java 中 int 和 Integer 是同一类型。（×）"
              class="text-input"
            />
          </el-tab-pane>

          <el-tab-pane label="上传文件" name="file">
            <div class="tab-tip">支持 TXT、Word（.docx）、PDF 文件，文件内容将被提取为文本后识别</div>
            <el-upload
              class="file-upload"
              drag
              :auto-upload="false"
              :limit="1"
              accept=".txt,.docx,.pdf"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :file-list="fileList"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
              <template #tip>
                <div class="upload-tip">支持 .txt / .docx / .pdf，单文件不超过 10MB</div>
              </template>
            </el-upload>
          </el-tab-pane>

          <el-tab-pane label="上传图片" name="image">
            <div class="tab-tip">上传题目截图或扫描件，AI 将直接识别图片中的题目内容（使用多模态模型）</div>
            <el-upload
              class="image-upload"
              list-type="picture-card"
              :auto-upload="false"
              accept=".jpg,.jpeg,.png"
              :on-change="onImageChange"
              :on-remove="onImageRemove"
              :file-list="imageList"
              multiple
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="upload-tip">支持 JPG / PNG，可上传多张，单张不超过 5MB</div>
              </template>
            </el-upload>
          </el-tab-pane>
        </el-tabs>

        <div class="step-actions">
          <el-button
            type="primary"
            size="large"
            :loading="parsing"
            :disabled="!canParse"
            @click="startParse"
            class="parse-btn"
          >
            <el-icon v-if="!parsing"><MagicStick /></el-icon>
            {{ parsing ? 'AI 识别中...' : '开始识别' }}
          </el-button>
        </div>
      </div>

      <!-- ======== Step 2：确认题目（翻页模式） ======== -->
      <div v-if="step === 1" class="step-content">

        <!-- 识别进度横幅 -->
        <div v-if="parsing" class="parsing-banner">
          <div class="parsing-left">
            <span class="parsing-dot"></span>
            <span>AI 识别中... 已识别 <strong>{{ parsedCount }}</strong> 道题目</span>
          </div>
          <el-button size="small" plain @click="cancelParse">中止识别</el-button>
        </div>

        <!-- 顶部操作栏 -->
        <div class="result-header">
          <div class="result-summary">
            <span v-if="parsing">正在识别，已发现 <strong>{{ drafts.length }}</strong> 道题目</span>
            <span v-else>共识别到 <strong>{{ drafts.length }}</strong> 道题目，请确认并选择目标题库</span>
          </div>
          <div class="result-actions-top">
            <el-select
              v-model="targetBankId"
              placeholder="选择目标题库"
              style="width: 200px"
              filterable
              clearable
              :loading="banksLoading"
            >
              <el-option
                v-for="bank in myBanks"
                :key="bank.id"
                :label="bank.name"
                :value="bank.id"
              />
              <template v-if="!banksLoading && myBanks.length === 0" #empty>
                <div style="padding: 10px 16px; color: #909399; font-size: 13px; text-align: center">
                  暂无题库，请先
                  <el-button link type="primary" @click="openCreateBankDialog(true)">创建题库并导入</el-button>
                </div>
              </template>
            </el-select>
            <el-button type="primary" plain :disabled="parsing" @click="openCreateBankDialog(true)">新建题库</el-button>
            <el-button :disabled="parsing" @click="step = 0" plain>重新识别</el-button>
            <el-popconfirm title="确定清除所有草稿吗？" @confirm="resetAll">
              <template #reference>
                <el-button plain type="danger">清除草稿</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <!-- 翻页卡片区 -->
        <div v-if="drafts.length > 0" class="pager-area">

          <!-- 左箭头 -->
          <button
            class="nav-arrow nav-arrow--left"
            :disabled="currentIdx === 0"
            @click="goTo(currentIdx - 1)"
          >
            <el-icon><ArrowLeft /></el-icon>
          </button>

          <!-- 单张题目卡片 -->
          <transition :name="slideDir" mode="out-in">
            <el-card
              :key="currentIdx"
              class="draft-card"
              shadow="never"
            >
              <!-- 卡片头部 -->
              <div class="draft-header">
                <div class="draft-index">
                  <el-tag :type="getTypeTagColor(currentDraft.type)" size="small">
                    {{ getTypeName(currentDraft.type) }}
                  </el-tag>
                  <span class="draft-num">第 {{ currentIdx + 1 }} / {{ drafts.length }} 题</span>
                </div>
                <el-button type="danger" link size="small" @click="removeDraft(currentIdx)">
                  <el-icon><Delete /></el-icon>删除本题
                </el-button>
              </div>

              <!-- 内容区 -->
              <div class="draft-body">
                <!-- 题干 -->
                <div class="draft-field">
                  <label>题干</label>
                  <el-input
                    :model-value="currentDraft.content"
                    @update:model-value="val => currentDraft.content = val"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 6 }"
                    placeholder="题目内容（可手动编辑）"
                  />
                </div>


                <!-- 选项（单选/多选）- 改为两列布局 -->
                <div v-if="currentDraft.options && currentDraft.options.length" class="draft-field">
                  <label>选项</label>
                  <div class="options-grid">
                    <div v-for="(opt, oi) in currentDraft.options" :key="oi" class="option-row">
                      <span class="opt-label">{{ opt.label }}.</span>
                      <el-input v-model="opt.text" size="small" />
                    </div>
                  </div>
                </div>

                <!-- 答案 + 难度 同行 -->
                <div class="draft-field draft-field--row">
                  <div class="field-group">
                    <label>答案</label>
                    <el-input
                      :model-value="currentDraft.answer"
                      @update:model-value="val => currentDraft.answer = val"
                      size="small"
                      style="width: 180px"
                      :placeholder="getAnswerPlaceholder(currentDraft.type)"
                    />
                  </div>
                  <div class="field-group">
                    <label>难度</label>
                    <el-radio-group v-model="currentDraft.difficulty" size="small">
                      <el-radio-button value="easy">简单</el-radio-button>
                      <el-radio-button value="medium">中等</el-radio-button>
                      <el-radio-button value="hard">困难</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>

                <!-- 解析 -->
                <div class="draft-field">
                  <label>解析 <span class="optional">（可选）</span></label>
                  <el-input
                    :model-value="currentDraft.analysis"
                    @update:model-value="val => currentDraft.analysis = val"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    placeholder="暂无解析"
                  />
                </div>
              </div>
            </el-card>
          </transition>

          <!-- 右箭头 -->
          <button
            class="nav-arrow nav-arrow--right"
            :disabled="currentIdx === drafts.length - 1"
            @click="goTo(currentIdx + 1)"
          >
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <!-- 识别中但还没有题目时的占位 -->
        <div v-else-if="parsing" class="pager-placeholder">
          <el-icon class="placeholder-icon rotating"><Loading /></el-icon>
          <p>AI 正在识别，题目识别完成后将在此显示…</p>
        </div>

        <!-- 底部分页指示点 -->
        <div v-if="drafts.length > 0" class="pager-dots">
          <span
            v-for="(_, i) in drafts"
            :key="i"
            class="dot"
            :class="{ active: i === currentIdx }"
            @click="goTo(i)"
          />
        </div>

        <!-- 底部操作栏 -->
        <div class="step-actions">
          <el-button size="large" @click="step = 0" plain>上一步</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!targetBankId || drafts.length === 0 || parsing"
            @click="submitAll"
            class="submit-btn"
          >
            {{ submitting
              ? `提交中 ${submitProgress}/${drafts.length}...`
              : parsing
                ? '识别中，请稍候...'
                : `提交全部 ${drafts.length} 道题目` }}
          </el-button>
        </div>
      </div>

      <!-- ======== Step 3：完成 ======== -->
      <div v-if="step === 2" class="step-content step-done">
        <el-result
          icon="success"
          :title="`成功导入 ${successCount} 道题目`"
          :sub-title="failCount > 0 ? `${failCount} 道题目导入失败` : '所有题目已成功导入'"
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/user/questions')">查看我的题目</el-button>
            <el-button @click="resetAll">继续导入</el-button>
          </template>
        </el-result>
      </div>
    </el-card>

    <!-- 创建题库对话框 -->
    <el-dialog v-model="createBankDialogVisible" title="创建题库并导入" width="520px" :close-on-click-modal="false">
      <el-form :model="createBankForm" label-width="90px">
        <el-form-item label="题库名称" required>
          <el-input v-model="createBankForm.name" placeholder="请输入题库名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-cascader
            v-model="createBankForm.categoryId"
            :options="categoryStore.categories"
            :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: true }"
            placeholder="请选择分类或子分类"
            style="width: 100%"
            :loading="categoriesLoading"
            clearable
          />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="createBankForm.visibility">
            <el-radio value="public">公开</el-radio>
            <el-radio value="private">私有</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createBankForm.description" type="textarea" :rows="3" placeholder="可选，简单描述一下题库内容" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="createBankAutoImport">创建后立即导入当前识别的题目</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBankDialogVisible = false" :disabled="createBankLoading">取消</el-button>
        <el-button type="primary" :loading="createBankLoading" @click="doCreateBankAndMaybeImport">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, reactive, nextTick } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { MagicStick, UploadFilled, Plus, Delete, ArrowLeft, ArrowRight, Loading } from '@element-plus/icons-vue'
import { parseQuestionsStream } from '@/api/ai'
import { useQuestionStore } from '@/store/question'
import { useBankStore } from '@/store/bank'
import { useCategoryStore } from '@/store/category'

const questionStore = useQuestionStore()
const bankStore = useBankStore()
const categoryStore = useCategoryStore()

const STORAGE_KEY = 'ai_import_draft'

const step = ref(0)
const inputTab = ref('text')

// 文本输入
const textInput = ref('')

// 文件上传
const fileList = ref([])
const fileBase64 = ref('')
const fileType = ref('')
const fileReading = ref(false)

// 图片上传
const imageList = ref([])
const imageBase64List = ref([])

// 识别结果
const drafts = ref([])
const parsing = ref(false)
const parsedCount = ref(0)
let activeEventSource = null

// 翻页状态
const currentIdx = ref(0)
const slideDir = ref('slide-left')

// 当前展示的题目（直接引用 drafts[currentIdx]，支持双向编辑）
const currentDraft = computed(() => drafts.value[currentIdx.value] ?? {})

function goTo(idx) {
  if (idx < 0 || idx >= drafts.value.length) return
  slideDir.value = idx > currentIdx.value ? 'slide-left' : 'slide-right'
  currentIdx.value = idx
}

function removeDraft(idx) {
  drafts.value.splice(idx, 1)
  if (currentIdx.value >= drafts.value.length) {
    currentIdx.value = Math.max(0, drafts.value.length - 1)
  }
}

// 提交
const targetBankId = ref(null)
const myBanks = ref([])
const banksLoading = ref(false)
const submitting = ref(false)
const successCount = ref(0)
const failCount = ref(0)
const submitProgress = ref(0)

// 创建题库
const createBankDialogVisible = ref(false)
const createBankLoading = ref(false)
const createBankAutoImport = ref(true)
const categoriesLoading = ref(false)
const createBankForm = reactive({
  name: '',
  categoryId: null,
  visibility: 'public',
  description: ''
})

function openCreateBankDialog(autoImport = true) {
  createBankAutoImport.value = !!autoImport
  createBankForm.name = ''
  createBankForm.categoryId = null
  createBankForm.visibility = 'public'
  createBankForm.description = ''
  createBankDialogVisible.value = true
  ensureCategoriesLoaded()
}

async function ensureCategoriesLoaded() {
  if (categoryStore.categories?.length) return
  categoriesLoading.value = true
  try {
    await categoryStore.fetchCategories()
  } catch {
    ElMessage.error('加载分类失败，请稍后重试')
  } finally {
    categoriesLoading.value = false
  }
}

async function doCreateBankAndMaybeImport() {
  if (!createBankForm.name.trim()) { ElMessage.warning('请输入题库名称'); return }
  if (!createBankForm.categoryId) { ElMessage.warning('请选择分类'); return }
  createBankLoading.value = true
  try {
    const bank = await bankStore.createBank({
      name: createBankForm.name.trim(),
      categoryId: createBankForm.categoryId,
      visibility: createBankForm.visibility,
      description: createBankForm.description?.trim() || ''
    })
    await loadMyBanks()
    targetBankId.value = bank?.id || targetBankId.value
    createBankDialogVisible.value = false
    ElMessage.success('题库创建成功')
    if (createBankAutoImport.value) { await nextTick(); submitAll() }
  } catch (e) {
    ElMessage.error(e?.message || '创建题库失败，请稍后重试')
  } finally {
    createBankLoading.value = false
  }
}

// 草稿持久化
function loadDraftFromStorage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) return
    const { drafts: savedDrafts, targetBankId: savedBankId } = JSON.parse(saved)
    if (Array.isArray(savedDrafts) && savedDrafts.length > 0) {
      drafts.value = savedDrafts
      targetBankId.value = savedBankId || null
      currentIdx.value = 0
      step.value = 1
      ElNotification({ title: '草稿已恢复', message: `上次识别的 ${savedDrafts.length} 道题目已自动恢复`, type: 'info', duration: 4000 })
    }
  } catch { localStorage.removeItem(STORAGE_KEY) }
}

function saveDraftToStorage() {
  try {
    if (drafts.value.length > 0) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ drafts: drafts.value, targetBankId: targetBankId.value, savedAt: Date.now() }))
    }
  } catch {}
}

function clearDraftStorage() { localStorage.removeItem(STORAGE_KEY) }

watch([drafts, targetBankId], saveDraftToStorage, { deep: true })

async function loadMyBanks() {
  banksLoading.value = true
  try {
    const data = await bankStore.fetchMyBanks({ page: 1, size: 200 })
    const list = Array.isArray(data) ? data : (data?.records || data?.list || data?.data || [])
    myBanks.value = list
    if (list.length === 0) ElMessage.warning('你还没有题库，请先创建一个题库再导入题目')
  } catch {
    ElMessage.error('加载题库列表失败，请刷新页面重试')
  } finally {
    banksLoading.value = false
  }
}

onMounted(async () => {
  await loadMyBanks()
  ensureCategoriesLoaded()
  loadDraftFromStorage()
})

const canParse = computed(() => {
  if (inputTab.value === 'text') return textInput.value.trim().length > 0
  if (inputTab.value === 'file') return !fileReading.value && fileList.value.length > 0 && fileBase64.value.length > 0
  if (inputTab.value === 'image') return imageBase64List.value.length > 0
  return false
})

function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => {
      const result = e.target.result
      resolve(result.includes(',') ? result.split(',')[1] : result)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsText(file, 'UTF-8')
  })
}

async function onFileChange(file) {
  fileReading.value = true
  fileBase64.value = ''
  fileType.value = ''
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['txt', 'docx', 'pdf'].includes(ext)) {
    ElMessage.error('只支持 .txt / .docx / .pdf 文件')
    fileList.value = []; fileReading.value = false; return
  }
  fileList.value = [file]
  fileType.value = ext
  const raw = file.raw || file
  if (!raw) { ElMessage.error('读取文件失败，请重新选择文件'); fileReading.value = false; return }
  if (ext === 'txt') {
    try {
      textInput.value = await readFileAsText(raw)
      inputTab.value = 'text'
      ElMessage.info('TXT 文件已自动切换为文本模式')
    } catch { ElMessage.error('读取 TXT 文件失败，请重试'); fileList.value = []; fileType.value = '' }
    finally { fileReading.value = false }
  } else {
    try { fileBase64.value = await readFileAsBase64(raw) }
    catch { ElMessage.error('读取文件失败，请重试'); fileList.value = []; fileType.value = ''; fileBase64.value = '' }
    finally { fileReading.value = false }
  }
}

function onFileRemove() { fileList.value = []; fileBase64.value = ''; fileType.value = ''; fileReading.value = false }

async function onImageChange(file) { imageBase64List.value.push(await readFileAsBase64(file.raw)) }
function onImageRemove(file) {
  const idx = imageList.value.findIndex(f => f.uid === file.uid)
  if (idx >= 0) imageBase64List.value.splice(idx, 1)
}

function normalizeDraft(d) {
  return {
    type: normalizeType(d.type),
    content: d.content || '',
    options: Array.isArray(d.options) ? d.options.map((o, i) => ({
      label: o?.label || String.fromCharCode(65 + i),
      text: typeof o === 'string' ? o : (o?.text || ''),
    })) : null,
    answer: Array.isArray(d.answer) ? d.answer.join(',') : String(d.answer ?? ''),
    analysis: d.analysis || '',
    difficulty: normalizeDifficulty(d.difficulty),
  }
}

async function startParse() {
  if (inputTab.value === 'file' && fileReading.value) { ElMessage.info('文件读取中，请稍候…'); return }
  if (inputTab.value === 'file' && (!fileBase64.value || fileBase64.value.trim().length === 0)) {
    const uf = fileList.value?.[0]
    const raw = uf?.raw || uf
    if (!raw) { ElMessage.error('未读取到文件内容，请重新选择文件'); return }
    try {
      fileReading.value = true
      if (!fileType.value && uf?.name) fileType.value = uf.name.split('.').pop().toLowerCase()
      fileBase64.value = await readFileAsBase64(raw)
    } catch { ElMessage.error('读取文件失败，请重试'); return }
    finally { fileReading.value = false }
    if (!fileBase64.value?.trim()) { ElMessage.error('文件读取失败，请重新选择文件'); return }
  }

  parsing.value = true
  parsedCount.value = 0
  drafts.value = []
  currentIdx.value = 0
  step.value = 1

  const payload = inputTab.value === 'text'
    ? { text: textInput.value }
    : inputTab.value === 'file'
      ? { fileBase64: fileBase64.value, fileType: fileType.value }
      : { imageBase64List: imageBase64List.value }

  activeEventSource = parseQuestionsStream(
    payload,
    (draft) => {
      drafts.value.push(normalizeDraft(draft))
      parsedCount.value++
      saveDraftToStorage()
    },
    () => {
      parsing.value = false
      activeEventSource = null
      if (drafts.value.length === 0) { ElMessage.warning('未识别到任何题目，请检查内容格式'); step.value = 0 }
      else ElMessage.success(`识别完成，共 ${drafts.value.length} 道题目`)
    },
    (errMsg) => {
      parsing.value = false
      activeEventSource = null
      ElMessage.error(errMsg || 'AI 识别失败，请稍后重试')
      if (drafts.value.length === 0) step.value = 0
    }
  )
}

function cancelParse() {
  activeEventSource?.close()
  activeEventSource = null
  parsing.value = false
  if (drafts.value.length === 0) step.value = 0
}

async function submitAll() {
  if (!targetBankId.value) { ElMessage.warning('请先选择目标题库'); return }
  submitting.value = true
  successCount.value = 0
  failCount.value = 0
  submitProgress.value = 0

  for (const draft of drafts.value) {
    submitProgress.value++
    try {
      const options = draft.options?.length ? draft.options.map(o => typeof o === 'string' ? o : o.text || '') : null
      let answer = draft.answer
      if (draft.type === 'multiple' && typeof answer === 'string') {
        answer = answer.split(',').map(s => s.trim()).filter(Boolean)
      }
      await questionStore.createQuestion({
        bankId: targetBankId.value, type: draft.type, content: draft.content,
        options, answer, analysis: draft.analysis || null,
        difficulty: draft.difficulty, status: 'published',
      })
      successCount.value++
    } catch { failCount.value++ }
  }

  submitting.value = false
  clearDraftStorage()
  step.value = 2
}

function resetAll() {
  activeEventSource?.close()
  activeEventSource = null
  clearDraftStorage()
  step.value = 0; inputTab.value = 'text'; textInput.value = ''
  fileList.value = []; fileBase64.value = ''; fileType.value = ''
  imageList.value = []; imageBase64List.value = []
  drafts.value = []; parsedCount.value = 0; currentIdx.value = 0
  targetBankId.value = null; successCount.value = 0; failCount.value = 0
}

function normalizeType(type) {
  if (!type) return 'essay'
  const t = type.trim().toLowerCase()
  if (['single', '单选', '单选题', 'choice', 'single_choice'].includes(t)) return 'single'
  if (['multiple', '多选', '多选题', 'multiple_choice'].includes(t)) return 'multiple'
  if (['judge', '判断', '判断题', 'true_false', 'truefalse'].includes(t)) return 'judge'
  if (['fill', '填空', '填空题', 'blank', 'fill_blank'].includes(t)) return 'fill'
  return 'essay'
}

function normalizeDifficulty(d) {
  if (!d) return 'medium'
  const v = d.trim().toLowerCase()
  if (['easy', '简单', '容易'].includes(v)) return 'easy'
  if (['hard', '困难', '难'].includes(v)) return 'hard'
  return 'medium'
}

function getTypeName(type) {
  return { single: '单选', multiple: '多选', judge: '判断', fill: '填空', essay: '简答' }[type] || type
}
function getTypeTagColor(type) {
  return { single: undefined, multiple: 'warning', judge: 'success', fill: 'info', essay: 'danger' }[type]
}
function getAnswerPlaceholder(type) {
  return { single: '如：A', multiple: '如：A,C（逗号分隔）', judge: 'true 或 false', fill: '填空答案', essay: '参考答案' }[type] || '答案'
}
</script>

<style lang="scss" scoped>
.ai-import-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
}

.main-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 28px;

  .header-left { display: flex; align-items: center; gap: 14px; }
  .header-icon { font-size: 36px; color: #409eff; background: #ecf5ff; padding: 10px; border-radius: 10px; }
  .page-title { margin: 0 0 4px; font-size: 20px; font-weight: 600; color: #303133; }
  .page-desc { margin: 0; font-size: 13px; color: #909399; }
}

.steps-bar { margin-bottom: 32px; }
.step-content { min-height: 300px; }

.tab-tip {
  font-size: 13px; color: #909399; margin-bottom: 12px;
  padding: 8px 12px; background: #f7f9fc; border-radius: 6px;
}

.text-input {
  :deep(.el-textarea__inner) { font-family: 'Courier New', monospace; font-size: 13px; line-height: 1.7; }
}

.file-upload { width: 100%; }
.image-upload {
  width: 100%; min-height: 160px;
  :deep(.el-upload-list--picture-card) { min-height: 110px; }
}
.upload-icon { font-size: 48px; color: #c0c4cc; margin-bottom: 8px; }
.upload-text { font-size: 14px; color: #606266; em { color: #409eff; font-style: normal; } }
.upload-tip { font-size: 12px; color: #909399; margin-top: 8px; text-align: center; }

/* 识别进度横幅 */
.parsing-banner {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; margin-bottom: 12px;
  background: #ecf5ff; border: 1px solid #b3d8ff; border-radius: 8px;
  font-size: 13px; color: #409eff;

  .parsing-left { display: flex; align-items: center; gap: 8px; }
  .parsing-dot {
    width: 8px; height: 8px; border-radius: 50%; background: #409eff;
    animation: pulse 1.2s ease-in-out infinite;
  }
  strong { color: #1a6fd4; font-size: 15px; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

/* 顶部操作栏 */
.result-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; padding: 12px 16px;
  background: #f7f9fc; border-radius: 8px; flex-wrap: wrap; gap: 10px;

  .result-summary { font-size: 14px; color: #606266; strong { color: #409eff; font-size: 18px; } }
  .result-actions-top { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
}

/* ===== 翻页布局 ===== */
.pager-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

/* 箭头按钮 */
.nav-arrow {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.18s;
  font-size: 16px;

  &:hover:not(:disabled) {
    border-color: #409eff;
    color: #409eff;
    background: #ecf5ff;
  }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

/* 题目卡片 */
.draft-card {
  flex: 1;
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 10px;

  :deep(.el-card__body) { padding: 0; }

  .draft-header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;
    background: #f7f9fc;
    border-radius: 10px 10px 0 0;
  }

  .draft-index { display: flex; align-items: center; gap: 8px; }
  .draft-num { font-size: 13px; color: #909399; }

  .draft-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

/* 字段 */
.draft-field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 12px; color: #909399; font-weight: 500;
    .optional { font-weight: normal; color: #c0c4cc; }
  }

  :deep(.el-textarea__inner) { font-size: 14px; line-height: 1.65; }
  :deep(.el-input__inner) { font-size: 14px; }

  &.draft-field--row {
    flex-direction: row;
    align-items: flex-end;
    gap: 28px;
    flex-wrap: wrap;
  }

  .field-group { display: flex; flex-direction: column; gap: 6px; }
}

/* 选项两列网格布局 */
.options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .opt-label {
    font-size: 13px;
    font-weight: 600;
    color: #606266;
    min-width: 24px;
    flex-shrink: 0;
  }
  
  :deep(.el-input__inner) {
    height: 28px;
    font-size: 13px;
  }
}

// 响应式：当屏幕较窄时改为单列
@media (max-width: 500px) {
  .options-grid {
    grid-template-columns: 1fr;
  }
}
.option-row {
  display: flex; align-items: center; gap: 8px;
  .opt-label { font-size: 13px; font-weight: 600; color: #606266; min-width: 20px; flex-shrink: 0; }
}

/* 分页指示点 */
.pager-dots {
  display: flex;
  justify-content: center;
  gap: 5px;
  margin-bottom: 4px;
  flex-wrap: wrap;
  max-height: 40px;
  overflow: hidden;

  .dot {
    width: 7px; height: 7px;
    border-radius: 50%;
    background: #dcdfe6;
    cursor: pointer;
    transition: background 0.2s, transform 0.2s;
    flex-shrink: 0;

    &.active { background: #409eff; transform: scale(1.35); }
    &:hover:not(.active) { background: #a0cfff; }
  }
}

/* 识别中占位 */
.pager-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 200px; color: #909399; font-size: 14px; gap: 12px;
  .placeholder-icon { font-size: 36px; color: #409eff; }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
.rotating { animation: rotating 1.2s linear infinite; }

/* 翻页过渡动画 */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.2s ease;
}
.slide-left-enter-from  { opacity: 0; transform: translateX(32px); }
.slide-left-leave-to    { opacity: 0; transform: translateX(-32px); }
.slide-right-enter-from { opacity: 0; transform: translateX(-32px); }
.slide-right-leave-to   { opacity: 0; transform: translateX(32px); }

/* 底部操作栏 */
.step-actions {
  display: flex; justify-content: center; gap: 12px;
  margin-top: 20px; padding: 16px 20px;
  border-top: 1px solid #ebeef5; background: #fff;
  position: sticky; bottom: 0; z-index: 10;
  border-radius: 0 0 12px 12px;

  .parse-btn, .submit-btn { min-width: 160px; }
}

/* Step 3 */
.step-done {
  display: flex; justify-content: center; align-items: center; padding: 40px 0;
}

.input-tabs {
  :deep(.el-tabs__nav-wrap) { margin-bottom: 0; }
}
</style>