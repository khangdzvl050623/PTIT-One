import type { TeachingScheduleEntry } from '../types'

/** `thu` của API: 2 = thứ Hai … 8 = Chủ nhật. */
const THU_LABELS: Readonly<Record<number, string>> = {
  2: 'T2',
  3: 'T3',
  4: 'T4',
  5: 'T5',
  6: 'T6',
  7: 'T7',
  8: 'CN',
}

/** Một buổi dạy gọn trong một dòng: `T6 (1–3) · 1A105 · tuần 1–15`. */
export function formatLich(e: TeachingScheduleEntry): string {
  const thu = THU_LABELS[e.thu] ?? `T${e.thu}`
  const tiet = e.soTiet === 1 ? `${e.tietBatDau}` : `${e.tietBatDau}–${e.tietBatDau + e.soTiet - 1}`
  const tuan = e.tuanBatDau === e.tuanKetThuc ? `tuần ${e.tuanBatDau}` : `tuần ${e.tuanBatDau}–${e.tuanKetThuc}`
  return `${thu} (${tiet}) · ${e.phongHoc ?? '—'} · ${tuan}`
}

/** Ngày giờ kiểu Việt Nam. Chuỗi không đọc được thì trả gạch, không ném lỗi. */
export function vnDateTime(iso: string): string {
  const value = Date.parse(iso)
  return Number.isNaN(value) ? '—' : new Date(value).toLocaleString('vi-VN')
}
