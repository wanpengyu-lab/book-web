/**
 * ECharts 按需注册（PRD v0.9 强制规范：禁止全量 import）
 * 本版仅用 Line / Pie；图表组件统一从这里引 VChart
 */
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

/** 与令牌体系一致的图表调色（primary + 语义色系，深浅主题均可读） */
export const CHART_PALETTE = ['#8b7cf7', '#3ecf9a', '#5b9cf5', '#e8b64c', '#e06a6a', '#9a9aa8']

export const CHART_TEXT = '#8a8a96'
export const CHART_AXIS = 'rgba(128, 128, 140, 0.25)'

export { VChart }
