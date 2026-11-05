/** 日期与状态派生工具（全站复用） */

/** ISO → YYYY-MM-DD */
export function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

/** ISO → MM-DD HH:mm */
export function formatDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 逾期派生口径：status='active' AND due_at < NOW()（不落库，一律查询派生） */
export function isOverdueDue(dueAt: string): boolean {
  return new Date(dueAt).getTime() < Date.now()
}
