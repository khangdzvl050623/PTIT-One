import { useMemo, useState } from 'react'

import { Icon, Select } from '@/shared/ui'

import { entriesInWeek, formatDate, termOn, weekCount, weekOf, weekStart } from '../lib/week'
import type { Term, TimetableEntry } from '../types'
import { WeekGrid } from './WeekGrid'
import styles from './WeekTimetable.module.scss'

export interface WeekTimetableProps {
  /** Mới nhất trước; mặc định chọn kỳ đầu. */
  terms: readonly Term[]
  /** Buổi học cả học kỳ — giống `GET /api/me/timetable?maHocKy=` không gửi `tuan`. */
  timetableOf: (maHocKy: string) => readonly TimetableEntry[]
  /** Truyền vào để test và demo được ngày bất kỳ. */
  today?: Date
}

/** Thời khoá biểu dạng tuần: chọn học kỳ, chọn tuần, lưới tuần, nút in. */
export function WeekTimetable({ terms, timetableOf, today = new Date() }: WeekTimetableProps) {
  /* Mở học kỳ đang diễn ra; ngoài mọi học kỳ thì lấy kỳ đầu danh sách. */
  const [maHocKy, setMaHocKy] = useState(
    () => (termOn(terms, today) ?? terms[0])?.maHocKy ?? '',
  )
  const term = terms.find((t) => t.maHocKy === maHocKy) ?? terms[0]
  /* Mặc định tuần chứa hôm nay — đổi học kỳ thì tính lại theo kỳ mới. */
  const [tuan, setTuan] = useState(() => (term ? weekOf(term, today) : 1))

  const entries = useMemo(
    () => (term ? entriesInWeek(timetableOf(term.maHocKy), tuan) : []),
    [term, tuan, timetableOf],
  )

  if (!term) {
    return <p className={styles.empty}>Chưa có học kỳ nào.</p>
  }

  const soTuan = weekCount(term)

  function changeTerm(next: string) {
    const nextTerm = terms.find((t) => t.maHocKy === next)
    setMaHocKy(next)
    if (nextTerm) setTuan(weekOf(nextTerm, today))
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Học kỳ"
          value={term.maHocKy}
          options={terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
          onChange={changeTerm}
        />

        <Select
          ariaLabel="Tuần"
          className={styles.weekSelect}
          value={tuan}
          options={Array.from({ length: soTuan }, (_, i) => ({
            value: i + 1,
            label: weekLabel(term, i + 1),
          }))}
          onChange={setTuan}
        />

        <button className={styles.print} type="button" onClick={() => window.print()}>
          <Icon name="printer" size="15px" />
          <span>In</span>
        </button>
      </div>

      <ul className={styles.legend} aria-label="Chú thích màu">
        <li>
          <span className={`${styles.swatch} ${styles.swatchClash}`} /> Trùng lịch
        </li>
        <li>
          <span className={`${styles.swatch} ${styles.swatchOnline}`} /> Trực tuyến
        </li>
      </ul>

      {/* Chỉ hiện khi in: trang giấy cần biết đang là tuần nào. */}
      <p className={styles.printTitle}>
        {term.tenHocKy} · {weekLabel(term, tuan)}
      </p>

      {/* key theo tuần: đổi tuần thì thẻ buổi học chạy lại hiệu ứng hiện. */}
      <WeekGrid
        key={`${term.maHocKy}-${tuan}`}
        term={term}
        tuan={tuan}
        entries={entries}
        today={today}
        canPrev={tuan > 1}
        canNext={tuan < soTuan}
        onPrev={() => setTuan((n) => Math.max(1, n - 1))}
        onNext={() => setTuan((n) => Math.min(soTuan, n + 1))}
      />

      {entries.length === 0 ? (
        <p className={styles.empty}>Tuần này không có buổi học nào.</p>
      ) : null}
    </div>
  )
}

function weekLabel(term: Term, tuan: number): string {
  const start = weekStart(term, tuan)
  const end = new Date(start)
  end.setDate(end.getDate() + 6)
  return `Tuần ${tuan} [từ ngày ${formatDate(start)} đến ngày ${formatDate(end)}]`
}
