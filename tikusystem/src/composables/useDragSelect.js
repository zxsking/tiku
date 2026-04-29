import { ref, onUnmounted } from 'vue'

/**
 * 滑动批量选择 composable
 *
 * @param {Ref<Array>} rows        - 当前页数据列表
 * @param {Ref<Array>} selectedIds - 已选 id 数组（会被直接修改）
 * @param {string}     rowKey      - 行唯一键字段名，默认 'id'
 */
export function useDragSelect(rows, selectedIds, rowKey = 'id') {
  const tableRef = ref(null)
  const isDragging = ref(false)

  // 拖拽开始时的快照
  let dragStartIndex = -1
  let preIds = []       // 拖拽开始前的已选 id 快照
  let addMode = true    // true=添加选中，false=取消选中
  let frameId = null

  /** 获取鼠标 clientY 对应的行索引（-1 表示未命中） */
  function getRowIndex(clientY) {
    const el = tableRef.value?.$el
    if (!el) return -1
    const trs = el.querySelectorAll('.el-table__body tbody tr')
    for (let i = 0; i < trs.length; i++) {
      const { top, bottom } = trs[i].getBoundingClientRect()
      if (clientY >= top && clientY <= bottom) return i
    }
    if (!trs.length) return -1
    if (clientY < trs[0].getBoundingClientRect().top) return 0
    return trs.length - 1
  }

  /** 根据起止范围重新计算选中列表并同步 el-table */
  function applyRange(from, to) {
    const table = tableRef.value
    if (!table) return

    const min = Math.min(from, to)
    const max = Math.max(from, to)

    // 从快照出发，叠加本次拖拽范围
    const result = new Set(preIds)
    for (let i = min; i <= max; i++) {
      const row = rows.value[i]
      if (!row) continue
      addMode ? result.add(row[rowKey]) : result.delete(row[rowKey])
    }

    selectedIds.value = [...result]

    // 同步 el-table 勾选 UI
    rows.value.forEach(row => {
      table.toggleRowSelection(row, result.has(row[rowKey]))
    })
  }

  function onMouseDown(e) {
    if (e.button !== 0) return
    // 点在 checkbox 本身时让原生逻辑处理，不介入
    if (e.target.closest('.el-checkbox')) return

    const el = tableRef.value?.$el
    if (!el || !el.contains(e.target)) return
    const tr = e.target.closest('.el-table__body tbody tr')
    if (!tr) return

    const trs = el.querySelectorAll('.el-table__body tbody tr')
    dragStartIndex = Array.from(trs).indexOf(tr)
    if (dragStartIndex < 0) return

    const startRow = rows.value[dragStartIndex]
    if (!startRow) return

    // 快照当前选中状态
    preIds = [...selectedIds.value]
    // 起始行已选中 → 本次拖拽为"取消"模式，否则为"添加"模式
    addMode = !preIds.includes(startRow[rowKey])

    isDragging.value = false
    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    e.preventDefault()
  }

  function onMouseMove(e) {
    if (dragStartIndex < 0) return
    isDragging.value = true
    if (frameId) cancelAnimationFrame(frameId)
    frameId = requestAnimationFrame(() => {
      const idx = getRowIndex(e.clientY)
      if (idx >= 0) applyRange(dragStartIndex, idx)
    })
  }

  function onMouseUp() {
    dragStartIndex = -1
    preIds = []
    isDragging.value = false
    if (frameId) { cancelAnimationFrame(frameId); frameId = null }
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousedown', onMouseDown)

  onUnmounted(() => {
    document.removeEventListener('mousedown', onMouseDown)
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    if (frameId) cancelAnimationFrame(frameId)
  })

  return { tableRef, isDragging }
}
