<template>
  <div class="stats-page">
    <div class="page-back">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回错题本
      </el-button>
    </div>

    <div class="page-title">
      <el-icon><DataAnalysis /></el-icon>
      错题统计面板
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else>
      <!-- 顶部概览卡片 -->
      <el-row :gutter="16" class="overview-row">
        <el-col :span="6">
          <div class="overview-card" style="border-top: 3px solid #f56c6c">
            <div class="ov-num" style="color: #f56c6c">{{ stats.total || 0 }}</div>
            <div class="ov-label">总错题数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card" style="border-top: 3px solid #67c23a">
            <div class="ov-num" style="color: #67c23a">{{ masteredCount }}</div>
            <div class="ov-label">已掌握</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card" style="border-top: 3px solid #409eff">
            <div class="ov-num" style="color: #409eff">{{ todayReviewCount }}</div>
            <div class="ov-label">今日待复习</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card" style="border-top: 3px solid #e6a23c">
            <div class="ov-num" style="color: #e6a23c">{{ totalReviewed }}</div>
            <div class="ov-label">累计复习次数</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <!-- 左列：饼图 + 掌握进度 -->
        <el-col :span="12">
          <!-- 题型分布饼图 -->
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #409eff"></span>
                <span class="header-title">题型分布</span>
              </div>
            </template>
            <div v-if="stats.byType && stats.byType.length" ref="typeChartRef" class="chart-container"></div>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </el-card>

          <!-- 难度分布饼图 -->
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #e6a23c"></span>
                <span class="header-title">难度分布</span>
              </div>
            </template>
            <div v-if="stats.byDifficulty && stats.byDifficulty.length" ref="diffChartRef" class="chart-container"></div>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </el-card>
        </el-col>

        <!-- 右列：题库掌握进度 + 热力图 -->
        <el-col :span="12">
          <!-- 题库掌握进度 -->
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="header-left">
                <span class="header-dot" style="background: #67c23a"></span>
                <span class="header-title">各题库错题数</span>
              </div>
            </template>
            <div v-if="stats.byBank && stats.byBank.length" class="bank-progress-list">
              <div
                v-for="item in stats.byBank"
                :key="item.bankId"
                class="bank-progress-item"
              >
                <div class="bank-progress-header">
                  <span class="bank-name">{{ item.bankName }}</span>
                  <span class="bank-count" style="color: #f56c6c">{{ item.count }} 题</span>
                </div>
                <el-progress
                  :percentage="Math.round((item.count / (stats.total || 1)) * 100)"
                  color="#f56c6c"
                  :show-text="false"
                  :stroke-width="8"
                />
              </div>
            </div>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </el-card>

          <!-- 复习热力图 -->
          <el-card shadow="never" class="chart-card heatmap-card">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <span class="header-dot" style="background: #67c23a"></span>
                  <span class="header-title">复习热力图</span>
                </div>
                <div class="heatmap-legend">
                  <span class="legend-label">少</span>
                  <span class="legend-cell" style="background:#ebedf0"></span>
                  <span class="legend-cell" style="background:#9be9a8"></span>
                  <span class="legend-cell" style="background:#40c463"></span>
                  <span class="legend-cell" style="background:#30a14e"></span>
                  <span class="legend-cell" style="background:#216e39"></span>
                  <span class="legend-label">多</span>
                </div>
              </div>
            </template>
            <div class="heatmap-wrap">
              <div class="heatmap-weekday">
                <span></span>
                <span>Mon</span>
                <span></span>
                <span>Wed</span>
                <span></span>
                <span>Fri</span>
                <span></span>
              </div>
              <div class="heatmap-scroll">
                <div class="heatmap-months" ref="monthLabelRef"></div>
                <div class="heatmap-grid" ref="heatmapRef"></div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getWrongQuestionStats, getHeatmap, getTodayReview } from '@/api/wrongBook'

const router = useRouter()

const loading = ref(false)
const stats = ref({})
const heatmapData = ref([])
const masteredCount = ref(0)
const todayReviewCount = ref(0)
const totalReviewed = ref(0)

const typeChartRef = ref(null)
const diffChartRef = ref(null)
const heatmapRef = ref(null)
const monthLabelRef = ref(null)

const typeNameMap = {
  single: '单选题',
  multiple: '多选题',
  judge: '判断题',
  fill: '填空题',
  essay: '简答题'
}

const typeColorMap = {
  single: '#409eff',
  multiple: '#e6a23c',
  judge: '#67c23a',
  fill: '#909399',
  essay: '#c0c4cc'
}

const diffNameMap = {
  easy: '简单',
  medium: '中等',
  hard: '困难'
}

const diffColorMap = {
  easy: '#67c23a',
  medium: '#e6a23c',
  hard: '#f56c6c'
}

function initTypeChart() {
  if (!typeChartRef.value || !stats.value.byType?.length) return
  const chart = echarts.init(typeChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 题 ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '42%'],
      label: { show: true, formatter: '{b}' },
      data: stats.value.byType.map(item => ({
        name: typeNameMap[item.type] || item.type,
        value: item.count,
        itemStyle: { color: typeColorMap[item.type] || '#909399' }
      }))
    }]
  })
  chart.resize()
}

function initDiffChart() {
  if (!diffChartRef.value || !stats.value.byDifficulty?.length) return
  const chart = echarts.init(diffChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 题 ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '42%'],
      label: { show: true, formatter: '{b}' },
      data: stats.value.byDifficulty.map(item => ({
        name: diffNameMap[item.difficulty] || item.difficulty,
        value: item.count,
        itemStyle: { color: diffColorMap[item.difficulty] || '#909399' }
      }))
    }]
  })
  chart.resize()
}

function getHeatmapColor(count, max) {
  if (count === 0) return '#ebedf0'
  const ratio = count / max
  if (ratio <= 0.25) return '#9be9a8'
  if (ratio <= 0.5)  return '#40c463'
  if (ratio <= 0.75) return '#30a14e'
  return '#216e39'
}

function formatDateStr(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function initHeatmap() {
  if (!heatmapRef.value) return

  const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  const WEEKS = 39
  const GAP   = 2
  // 根据容器宽度动态计算格子大小，最大 14px
  const containerW = heatmapRef.value.parentElement?.offsetWidth || 400
  const CELL = Math.min(14, Math.floor((containerW - WEEKS * GAP) / WEEKS))

  // 构建日期 -> count 映射
  const dateMap = {}
  heatmapData.value.forEach(item => {
    dateMap[item.date] = Number(item.count)
  })
  const maxCount = Math.max(...Object.values(dateMap), 1)

  // 计算起止日期：结束=今天，开始=往前推 39 周（约9个月）的周日
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // 找到今天所在周的周六（结束列末尾）
  const endSat = new Date(today)
  endSat.setDate(today.getDate() + (6 - today.getDay()))

  // 往前推 39 周，从周日开始
  const startSun = new Date(endSat)
  startSun.setDate(endSat.getDate() - 39 * 7 + 1)

  // 按列（周）组织数据
  const weeks = []
  const cur = new Date(startSun)
  while (cur <= endSat) {
    const week = []
    for (let d = 0; d < 7; d++) {
      const dateStr = formatDateStr(cur)
      week.push({ date: dateStr, count: dateMap[dateStr] || 0, future: cur > today })
      cur.setDate(cur.getDate() + 1)
    }
    weeks.push(week)
  }

  // 渲染月份标签
  if (monthLabelRef.value) {
    monthLabelRef.value.innerHTML = ''
    let lastMonth = -1
    weeks.forEach((week, wi) => {
      const firstDay = new Date(week[0].date)
      const mo = firstDay.getMonth()
      if (mo !== lastMonth) {
        lastMonth = mo
        const span = document.createElement('span')
        span.textContent = MONTHS[mo]
        span.style.cssText = `
          position: absolute;
          left: ${wi * (CELL + GAP)}px;
          font-size: 11px;
          color: #57606a;
          white-space: nowrap;
        `
        monthLabelRef.value.appendChild(span)
      }
    })
    monthLabelRef.value.style.cssText = `
      position: relative;
      height: 18px;
      margin-bottom: 4px;
    `
  }

  // 渲染格子
  const grid = heatmapRef.value
  grid.innerHTML = ''
  grid.style.cssText = `
    display: flex;
    gap: ${GAP}px;
    align-items: flex-start;
  `

  weeks.forEach(week => {
    const col = document.createElement('div')
    col.style.cssText = `display: flex; flex-direction: column; gap: ${GAP}px;`

    week.forEach(({ date, count, future }) => {
      const cell = document.createElement('div')
      cell.style.cssText = `
        width: ${CELL}px;
        height: ${CELL}px;
        border-radius: 2px;
        background: ${future ? 'transparent' : getHeatmapColor(count, maxCount)};
        cursor: ${count > 0 ? 'pointer' : 'default'};
        position: relative;
      `
      // tooltip
      cell.title = future ? '' : count > 0 ? `${date}：复习 ${count} 次` : `${date}：无复习`
      col.appendChild(cell)
    })
    grid.appendChild(col)
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const [statsData, heatmap, todayList] = await Promise.all([
      getWrongQuestionStats(),
      getHeatmap(90),
      getTodayReview()
    ])
    stats.value = statsData
    heatmapData.value = Array.isArray(heatmap) ? heatmap : []
    todayReviewCount.value = Array.isArray(todayList) ? todayList.length : 0
    totalReviewed.value = heatmapData.value.reduce((sum, item) => sum + Number(item.count || 0), 0)
  } catch {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
    // 必须等 loading=false 后 v-else 的图表容器 DOM 渲染完毕再初始化
    await nextTick()
    initTypeChart()
    initDiffChart()
    initHeatmap()
  }
})
</script>

<style lang="scss" scoped>
.stats-page {
  padding: 4px 0;
}

.page-back {
  margin-bottom: 12px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

.loading-wrap {
  padding: 20px;
}

// ── 概览卡片 ──────────────────────────────────────────
.overview-row {
  margin-bottom: 16px;
}

.overview-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 16px 20px;
  text-align: center;
}

.ov-num {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
}

.ov-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

// ── 图表卡片 ──────────────────────────────────────────
.chart-card {
  border: 1px solid #edf0f5;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;

  :deep(.el-card__header) {
    padding: 14px 20px;
    background: #fafbfd;
    border-bottom: 1px solid #edf0f5;
  }

  :deep(.el-card__body) { padding: 16px 20px; }
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

.heatmap-hint {
  font-size: 12px;
  color: #b1b8c4;
}

.chart-container {
  width: 100%;
  height: 220px;
}

// ── 热力图 ────────────────────────────────────────────
.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 3px;
}

.legend-label {
  font-size: 11px;
  color: #57606a;
  margin: 0 2px;
}

.legend-cell {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  display: inline-block;
}

.heatmap-wrap {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.heatmap-weekday {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-top: 22px;
  flex-shrink: 0;

  span {
    font-size: 10px;
    color: #57606a;
    height: 15px;
    line-height: 15px;
    text-align: right;
    white-space: nowrap;
  }
}

.heatmap-scroll {
  overflow-x: hidden;
  flex: 1;
  min-width: 0;
}

.heatmap-months {
  position: relative;
  height: 18px;
  margin-bottom: 4px;
}

.heatmap-grid {
  display: flex;
  gap: 2px;
}

// ── 题库进度 ──────────────────────────────────────────
.bank-progress-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bank-progress-item {
  // empty
}

.bank-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}

.bank-name {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.bank-count {
  font-weight: 700;
  flex-shrink: 0;
}
</style>
