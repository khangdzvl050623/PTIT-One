import { Fragment } from 'react'

import { PERIODS } from '../data/timetable'
import {
  WEEKDAYS,
  WEEKDAY_LABELS,
  dayOf,
  formatDayMonth,
  formatTime,
  isSameDay,
  laneLayout,
} from '../lib/week'
import type { HinhThucHoc, Term, TimetableEntry } from '../types'
import styles from './WeekGrid.module.scss'

const HINH_THUC_LABELS: Record<HinhThucHoc, string | null> = {
  TRUC_TIEP: null,
  TRUC_TUYEN: 'Trực tuyến',
  KET_HOP: 'Kết hợp',
}

/** Buổi ngắn từng này tiết trở xuống: mỗi dòng phụ chỉ một dòng để vừa thẻ. */
const COMPACT_MAX_TIET = 3
/** Độ thụt của mỗi thẻ trùng lịch so với thẻ trước nó. */
const CASCADE_PX = 20

export interface WeekGridProps {
  term: Term
  tuan: number
  /** Đã lọc theo tuần. */
  entries: readonly TimetableEntry[]
  today: Date
  onPrev: () => void
  onNext: () => void
  canPrev: boolean
  canNext: boolean
}

/**
 * Lưới tuần: cột 1 là tiết, cột 2–8 là thứ 2 → Chủ nhật (trùng số `thu` của
 * API nên `thu` dùng thẳng làm số cột), cột 9 là rìa phải giữ nút "Sau".
 * Hàng 1 là tiêu đề, hàng `n + 1` là tiết `n`.
 */
export function WeekGrid({ term, tuan, entries, today, onPrev, onNext, canPrev, canNext }: WeekGridProps) {
  const days = WEEKDAYS.map((thu) => ({ thu, date: dayOf(term, tuan, thu) }))
  const todayThu = days.find((d) => isSameDay(d.date, today))?.thu
  const layout = laneLayout(entries)
  const lastRow = PERIODS.length + 2

  /** Hàng "← Trước · Thứ 2 … Chủ nhật · Sau →", dùng cho cả đầu và đáy lưới. */
  function headerRow(row: number, edge: 'top' | 'bottom') {
    return (
      <>
        <button
          className={`${styles.nav} ${edge === 'bottom' ? styles.bottom : ''}`}
          style={{ gridRow: row, gridColumn: 1 }}
          type="button"
          onClick={onPrev}
          disabled={!canPrev}
        >
          ← Trước
        </button>
        {days.map(({ thu, date }) => (
          <div
            key={`${edge}-${thu}`}
            className={`${styles.dayHead} ${edge === 'bottom' ? styles.bottom : ''} ${thu === todayThu ? styles.today : ''}`}
            style={{ gridRow: row, gridColumn: thu }}
            aria-hidden={edge === 'bottom' ? true : undefined}
          >
            <span>{WEEKDAY_LABELS[thu]}</span>
            <small>{formatDayMonth(date)}</small>
          </div>
        ))}
        <button
          className={`${styles.nav} ${edge === 'bottom' ? styles.bottom : ''}`}
          style={{ gridRow: row, gridColumn: 9 }}
          type="button"
          onClick={onNext}
          disabled={!canNext}
        >
          Sau →
        </button>
      </>
    )
  }

  return (
    <div className={styles.scroll}>
      <div
        className={styles.grid}
        style={{ gridTemplateRows: `44px repeat(${PERIODS.length}, 38px) 44px` }}
      >
        {headerRow(1, 'top')}

        {PERIODS.map((p) => (
          <Fragment key={p.soTiet}>
            <div className={styles.period} style={{ gridRow: p.soTiet + 1, gridColumn: 1 }}>
              <span>Tiết {p.soTiet}</span>
              <small>{formatTime(p.gioBatDau)}</small>
            </div>
            {WEEKDAYS.map((thu) => (
              <div
                key={thu}
                aria-hidden="true"
                className={`${styles.cell} ${thu === todayThu ? styles.todayCell : ''} ${p.soTiet === 5 ? styles.noonBreak : ''}`}
                style={{ gridRow: p.soTiet + 1, gridColumn: thu }}
              />
            ))}
            <div
              aria-hidden="true"
              className={styles.rail}
              style={{ gridRow: p.soTiet + 1, gridColumn: 9 }}
            />
          </Fragment>
        ))}

        {/* Lặp lại hàng tiêu đề ở đáy: chuyển tuần không phải cuộn lên đầu. */}
        {headerRow(lastRow, 'bottom')}

        {entries.map((e, index) => {
          const hinhThuc = HINH_THUC_LABELS[e.hinhThucHoc]
          const { lane, lanes, clash } = layout[index] ?? { lane: 0, lanes: 1, clash: false }
          return (
            <article
              key={`${e.maLopHP}-${e.thu}-${e.tietBatDau}`}
              className={[
                styles.entry,
                e.hinhThucHoc === 'TRUC_TUYEN' ? styles.online : '',
                clash ? styles.clash : '',
                e.soTiet <= COMPACT_MAX_TIET || lanes > 1 ? styles.compact : '',
              ].join(' ')}
              title={[
                `${e.tenMonHoc} (${e.maMonHoc})`,
                `Lớp: ${e.maLopHP}`,
                `Phòng: ${e.phongHoc ?? '—'}`,
                `GV: ${e.tenGiangVien ?? 'Chưa phân công'}`,
                clash ? 'Trùng lịch với buổi khác cùng tiết' : null,
              ]
                .filter(Boolean)
                .join('\n')}
              style={{
                gridColumn: e.thu,
                gridRow: `${e.tietBatDau + 1} / span ${e.soTiet}`,
                // Chồng tiết: xếp lệch như bài lá — thẻ sau thụt vào và nằm trên,
                // thẻ nào cũng gần đủ bề ngang thay vì bị chia đôi cột hẹp.
                width: `calc(100% - ${(lanes - 1) * CASCADE_PX + 4}px)`,
                marginLeft: `${lane * CASCADE_PX + 2}px`,
                zIndex: clash ? 2 + lane : undefined,
                animationDelay: `${index * 40}ms`,
              }}
            >
              <h4 className={styles.course}>
                {e.tenMonHoc} ({e.maMonHoc})
              </h4>
              <p className={styles.lop}>
                <b>Lớp:</b> {e.maLopHP}
              </p>
              <p>
                <b>Phòng:</b> {e.phongHoc ?? '—'}
              </p>
              <p>
                <b>GV:</b> {e.tenGiangVien ?? 'Chưa phân công'}
              </p>
              <p className={styles.time}>
                <span className={styles.timeRange}>
                  {formatTime(e.gioBatDau)} – {formatTime(e.gioKetThuc)}
                </span>
                {clash ? <span className={`${styles.badge} ${styles.badgeClash}`}>Trùng lịch</span> : null}
                {hinhThuc ? <span className={styles.badge}>{hinhThuc}</span> : null}
              </p>
            </article>
          )
        })}
      </div>
    </div>
  )
}
