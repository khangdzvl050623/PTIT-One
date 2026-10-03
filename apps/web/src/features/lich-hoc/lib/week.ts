import type { Term, TimetableEntry } from '../types'

const DAY_MS = 24 * 60 * 60 * 1000

/** `thu` của backend: 2 = thứ Hai … 8 = Chủ nhật. */
export const WEEKDAYS = [2, 3, 4, 5, 6, 7, 8] as const

export const WEEKDAY_LABELS: Record<number, string> = {
  2: 'Thứ 2',
  3: 'Thứ 3',
  4: 'Thứ 4',
  5: 'Thứ 5',
  6: 'Thứ 6',
  7: 'Thứ 7',
  8: 'Chủ nhật',
}

/* Ngày ISO đọc theo giờ địa phương — `new Date('2026-08-31')` là nửa đêm UTC,
   ở Việt Nam đã thành 7 giờ sáng và lệch ngày khi so sánh. */
export function parseDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y ?? 1970, (m ?? 1) - 1, d ?? 1)
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

/** Tuần `n` bắt đầu từ `ngayBatDau + 7·(n−1)` — đúng quy ước của API. */
export function weekStart(term: Term, tuan: number): Date {
  return addDays(parseDate(term.ngayBatDau), 7 * (tuan - 1))
}

/** Ngày cụ thể của một `thu` trong tuần. */
export function dayOf(term: Term, tuan: number, thu: number): Date {
  return addDays(weekStart(term, tuan), thu - 2)
}

export function weekCount(term: Term): number {
  const days = Math.round(
    (parseDate(term.ngayKetThuc).getTime() - parseDate(term.ngayBatDau).getTime()) / DAY_MS,
  )
  return Math.max(1, Math.floor(days / 7) + 1)
}

/** Tuần chứa `today`; ngoài học kỳ thì kẹp về tuần đầu hoặc cuối. */
export function weekOf(term: Term, today: Date): number {
  const start = parseDate(term.ngayBatDau)
  const midnight = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const tuan = Math.floor((midnight.getTime() - start.getTime()) / DAY_MS / 7) + 1
  return Math.min(Math.max(tuan, 1), weekCount(term))
}

export function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  )
}

export function formatDate(date: Date): string {
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function formatDayMonth(date: Date): string {
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

/** `07:00:00` → `07:00`. */
export function formatTime(hhmmss: string): string {
  return hhmmss.slice(0, 5)
}

/** Lọc như server khi gửi `?tuan=`: buổi có diễn ra trong tuần đó. */
export function entriesInWeek(
  entries: readonly TimetableEntry[],
  tuan: number,
): TimetableEntry[] {
  return entries.filter((e) => e.tuanBatDau <= tuan && tuan <= e.tuanKetThuc)
}

/** `thu` theo quy ước API cho một ngày: thứ Hai = 2 … Chủ nhật = 8. */
export function thuOf(date: Date): number {
  const day = date.getDay()
  return day === 0 ? 8 : day + 1
}

/** Học kỳ chứa ngày đó, nếu có. */
export function termOn(terms: readonly Term[], date: Date): Term | undefined {
  // Bỏ phần giờ: 14:00 ngày kết thúc vẫn thuộc học kỳ.
  const day = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  return terms.find((t) => parseDate(t.ngayBatDau) <= day && day <= parseDate(t.ngayKetThuc))
}

/** Các buổi học rơi đúng vào một ngày, theo tiết tăng dần. */
export function sessionsOn(
  terms: readonly Term[],
  timetableOf: (maHocKy: string) => readonly TimetableEntry[],
  date: Date,
): TimetableEntry[] {
  const term = termOn(terms, date)
  if (!term) return []
  const thu = thuOf(date)
  return entriesInWeek(timetableOf(term.maHocKy), weekOf(term, date))
    .filter((e) => e.thu === thu)
    .sort((a, b) => a.tietBatDau - b.tietBatDau)
}

/** Vị trí một thẻ trong ô ngày khi có buổi chồng tiết. */
export interface Lane {
  lane: number
  lanes: number
  clash: boolean
}

/**
 * Tìm buổi chồng tiết (cùng thứ, khoảng tiết giao nhau) và chia làn để các
 * thẻ đứng cạnh nhau thay vì đè lên nhau. Trùng lịch không tới được từ luồng
 * đăng ký (server chặn) nhưng vẫn xảy ra khi có lịch dạy bù.
 */
export function laneLayout(entries: readonly TimetableEntry[]): Lane[] {
  const end = (e: TimetableEntry) => e.tietBatDau + e.soTiet - 1
  const overlaps = (a: TimetableEntry, b: TimetableEntry) =>
    a.thu === b.thu && a.tietBatDau <= end(b) && b.tietBatDau <= end(a)

  return entries.map((e) => {
    const group = entries
      .filter((other) => other === e || overlaps(e, other))
      .sort((a, b) => a.tietBatDau - b.tietBatDau || a.maLopHP.localeCompare(b.maLopHP))
    return { lane: group.indexOf(e), lanes: group.length, clash: group.length > 1 }
  })
}
