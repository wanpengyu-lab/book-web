/**
 * 全站内联 SVG 图标（v0.9 禁 emoji 规则自 v0.8 起遵循；lucide 线稿，stroke 风格）
 * 用法：<SearchIcon class="size-4" />
 */
import { h, type FunctionalComponent } from 'vue'

const base: Record<string, string> = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '2',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
  'aria-hidden': 'true',
}

function make(...ds: string[]): FunctionalComponent {
  return () => h('svg', base, ds.map((d) => h('path', { d })))
}

export const SearchIcon = make('M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16z', 'm21 21-4.3-4.3')

export const BellIcon = make(
  'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9',
  'M10.3 21a1.94 1.94 0 0 0 3.4 0',
)

export const SunIcon = make(
  'M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32 1.41 1.41M2 12h2m16 0h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41',
  'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
)

export const MoonIcon = make('M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z')

export const MonitorIcon = make(
  'M2 5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z',
  'M8 21h8',
  'M12 17v4',
)

export const GridIcon = make(
  'M3 3h7v7H3z',
  'M14 3h7v7h-7z',
  'M14 14h7v7h-7z',
  'M3 14h7v7H3z',
)

export const TableIcon = make(
  'M3 5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
  'M3 10h18',
  'M3 15h18',
)

export const ChevronLeftIcon = make('m15 18-6-6 6-6')
export const ChevronRightIcon = make('m9 18 6-6-6-6')

export const XIcon = make('M18 6 6 18', 'm6 6 12 12')

export const BookIcon = make(
  'M4 19.5A2.5 2.5 0 0 1 6.5 17H20',
  'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z',
)

export const UserIcon = make(
  'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2',
  'M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
)

export const LogOutIcon = make('M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'm16 17 5-5-5-5', 'M21 12H9')

export const ClockIcon = make('M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z', 'M12 6v6l4 2')

export const CheckIcon = make('M20 6 9 17l-5-5')

export const ArrowLeftIcon = make('m12 19-7-7 7-7', 'M19 12H5')

export const SparklesIcon = make(
  'M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z',
  'M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z',
)
