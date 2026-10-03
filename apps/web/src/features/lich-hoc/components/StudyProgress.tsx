import { useMemo, useState } from 'react'
import type { CSSProperties } from 'react'

import { Icon } from '@/shared/ui'

import {
  WEEKDAY_LABELS,
  formatDate,
  formatTime,
  isSameDay,
  parseDate,
  sessionsOn,
  termOn,
  thuOf,
} from '../lib/week'
import type { Term, TimetableEntry } from '../types'
import styles from './StudyProgress.module.scss'

const WEEK_HEAD = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'] as const

type TermStatus = 'done' | 'current' | 'future'

export interface StudyProgressProps {
  /** Thứ tự bất kỳ — dòng thời gian tự xếp theo ngày bắt đầu. */
  terms: readonly Term[]
  timetableOf: (maHocKy: string) => readonly TimetableEntry[]
  today?: Date
}

/**
 * Tiến trình học tập: dòng thời gian các học kỳ (đã xong · đang học · sắp tới)
 * và lịch tháng chấm đỏ những ngày có buổi học. Bấm một ngày để xem các buổi.
 */
export function StudyProgress({ terms, timetableOf, today = new Date() }: StudyProgressProps) {
  const ordered = useMemo(
    () => [...terms].sort((a, b) => a.ngayBatDau.localeCompare(b.ngayBatDau)),
    [terms],
  )
  const [month, setMonth] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1))
  const [selected, setSelected] = useState<Date>(today)

  const currentIndex = ordered.findIndex((t) => statusOf(t, today) === 'current')
  // Tiến độ thanh: tới kỳ đang học; giữa hai kỳ thì tới kỳ vừa xong.
  const reached =
    currentIndex >= 0
      ? currentIndex
      : ordered.reduce((last, t, i) => (statusOf(t, today) === 'done' ? i : last), -1)

  const cells = useMemo(() => monthCells(month), [month])
  const busy = useMemo(
    () => cells.map((d) => sessionsOn(ordered, timetableOf, d).length),
    [cells, ordered, timetableOf],
  )
  const daySessions = sessionsOn(ordered, timetableOf, selected)

  function shiftMonth(delta: number) {
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1))
  }

  function jumpToTerm(term: Term) {
    // Kỳ đang học thì về tháng hiện tại; kỳ khác thì về tháng bắt đầu kỳ.
    const anchor = statusOf(term, today) === 'current' ? today : parseDate(term.ngayBatDau)
    setMonth(new Date(anchor.getFullYear(), anchor.getMonth(), 1))
    setSelected(anchor)
  }

  return (
    <div className={styles.wrap}>
      <ol
        className={styles.timeline}
        style={
          {
            '--n': Math.max(ordered.length, 1),
            '--steps': Math.max(ordered.length - 1, 1),
            '--reached': Math.max(reached, 0),
          } as CSSProperties
        }
      >
        {ordered.map((term) => {
          const status = statusOf(term, today)
          const active = termOn([term], selected) !== undefined
          return (
            <li key={term.maHocKy} className={styles.step}>
              <button
                type="button"
                className={`${styles.dot} ${styles[status]} ${active ? styles.dotActive : ''}`}
                onClick={() => jumpToTerm(term)}
                aria-label={`${term.tenHocKy}, từ ${formatDate(parseDate(term.ngayBatDau))} đến ${formatDate(parseDate(term.ngayKetThuc))}`}
              />
              <span className={styles.stepLabel}>{shortTermLabel(term.maHocKy)}</span>
              <span className={styles.tooltip} aria-hidden="true">
                <strong>{term.tenHocKy}</strong>
                <span>
                  Ngày BĐ: <b>{formatDate(parseDate(term.ngayBatDau))}</b>
                </span>
                <span>
                  Ngày KT: <b>{formatDate(parseDate(term.ngayKetThuc))}</b>
                </span>
              </span>
            </li>
          )
        })}
      </ol>

      <div className={styles.calendar}>
        <div className={styles.monthBar}>
          <button
            type="button"
            className={styles.monthNav}
            onClick={() => shiftMonth(-1)}
            aria-label="Tháng trước"
          >
            <Icon name="chevronRight" size="14px" className={styles.flip} />
          </button>
          <p className={styles.monthTitle} aria-live="polite">
            <span>Tháng {month.getMonth() + 1}</span>
            <span>{month.getFullYear()}</span>
          </p>
          <button
            type="button"
            className={styles.monthNav}
            onClick={() => shiftMonth(1)}
            aria-label="Tháng sau"
          >
            <Icon name="chevronRight" size="14px" />
          </button>
        </div>

        <div className={styles.days}>
          {WEEK_HEAD.map((d) => (
            <span key={d} className={`${styles.head} ${d === 'CN' ? styles.sunday : ''}`}>
              {d}
            </span>
          ))}
          {cells.map((date, i) => {
            const outside = date.getMonth() !== month.getMonth()
            const count = busy[i] ?? 0
            return (
              <button
                key={date.toISOString()}
                type="button"
                className={[
                  styles.day,
                  outside ? styles.outside : '',
                  thuOf(date) === 8 ? styles.sunday : '',
                  isSameDay(date, today) ? styles.today : '',
                  isSameDay(date, selected) ? styles.selected : '',
                ].join(' ')}
                aria-pressed={isSameDay(date, selected)}
                aria-label={`${formatDate(date)}${count ? `, ${count} buổi học` : ''}`}
                onClick={() => setSelected(date)}
              >
                <span className={styles.num}>{date.getDate()}</span>
                {count ? <span className={styles.mark} aria-hidden="true" /> : null}
              </button>
            )
          })}
        </div>

        <section className={styles.detail} aria-live="polite">
          <h4 className={styles.detailTitle}>
            {WEEKDAY_LABELS[thuOf(selected)]}, {formatDate(selected)}
            {termOn(ordered, selected) ? (
              <small> · {termOn(ordered, selected)?.tenHocKy}</small>
            ) : null}
          </h4>
          {daySessions.length === 0 ? (
            <p className={styles.none}>Không có buổi học.</p>
          ) : (
            <ul className={styles.sessions}>
              {daySessions.map((e) => (
                <li key={`${e.maLopHP}-${e.tietBatDau}`}>
                  <span className={styles.when}>
                    {formatTime(e.gioBatDau)} – {formatTime(e.gioKetThuc)}
                  </span>
                  <span className={styles.what}>
                    <b>{e.tenMonHoc}</b> · Phòng {e.phongHoc ?? '—'} · Tiết {e.tietBatDau}–
                    {e.tietBatDau + e.soTiet - 1}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}

function statusOf(term: Term, today: Date): TermStatus {
  if (termOn([term], today)) return 'current'
  return parseDate(term.ngayKetThuc) < today ? 'done' : 'future'
}

/** 42 ô (6 tuần) bắt đầu từ thứ Hai trên hoặc trước ngày 1. */
function monthCells(first: Date): Date[] {
  const offset = (first.getDay() + 6) % 7 // thứ Hai = 0
  return Array.from(
    { length: 42 },
    (_, i) => new Date(first.getFullYear(), first.getMonth(), 1 - offset + i),
  )
}

/** `2026-2027-HK1` → `HK1 2026-27`; mã lạ thì giữ nguyên. */
function shortTermLabel(maHocKy: string): string {
  const m = /^(\d{4})-(\d{4})-HK(\d)$/.exec(maHocKy)
  return m ? `HK${m[3]} ${m[1]}-${m[2]?.slice(2)}` : maHocKy
}
