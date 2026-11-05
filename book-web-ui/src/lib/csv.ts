/** CSV 导出工具：UTF-8 BOM（PRD v0.9 交互项） */
export function downloadCsv(filename: string, rows: (string | number)[][]): void {
  const escape = (v: string | number): string => {
    const s = String(v)
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  // UTF-8 BOM：保证 Excel 直接打开中文不乱码
  const content = '\uFEFF' + rows.map((r) => r.map(escape).join(',')).join('\n')
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
