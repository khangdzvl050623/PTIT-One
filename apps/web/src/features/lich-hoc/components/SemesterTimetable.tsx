import { useMemo, useState } from 'react'

import { downloadCsv } from '@/shared/lib'
import { Icon, Select } from '@/shared/ui'

import { dayOf, formatTime, termOn } from '../lib/week'
import type { HinhThucHoc, Term, TimetableEntry } from '../types'
import styles from './SemesterTimetable.module.scss'

const HINH_THUC: Record<HinhThucHoc, string> = {
  TRUC_TIEP: 'Trực tiếp',
  TRUC_TUYEN: 'Trực tuyến',
  KET_HOP: 'Kết hợp',
}

const CSV_HEADER = [
  'Mã MH',
  'Tên môn học',
  'Lớp',
  'Hình thức',
  'Thứ',
  'Tiết bắt đầu',
  'Số tiết',
  'Giờ học',
  'Phòng',
  'Giảng viên',
  'Từ ngày',
  'Đến ngày',
  'Ghi chú',
]

interface ClassGroup {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  hinhThucHoc: HinhThucHoc
  sessions: TimetableEntry[]
}

export interface SemesterTimetableProps {
  terms: readonly Term[]
  /** Buổi học cả học kỳ — giống `GET /api/me/timetable?maHocKy=` không gửi `tuan`. */
  timetableOf: (maHocKy: string) => readonly TimetableEntry[]
  /** Tên tệp khi xuất, không gồm đuôi; mã học kỳ được nối thêm. */
  exportName: string
  today?: Date
}

/**
 * Thời khoá biểu dạng học kỳ: mỗi lớp một nhóm, mỗi buổi trong tuần một dòng
 * kèm khoảng ngày học thật (suy từ tuần bắt đầu/kết thúc và thứ).
 */
export function SemesterTimetable({
  terms,
  timetableOf,
  exportName,
  today = new Date(),
}: SemesterTimetableProps) {
  const [maHocKy, setMaHocKy] = useState(
    () => (termOn(terms, today) ?? terms[0])?.maHocKy ?? '',
  )
  const term = terms.find((t) => t.maHocKy === maHocKy) ?? terms[0]
  const groups = useMemo(
    () => (term ? groupByClass(timetableOf(term.maHocKy)) : []),
    [term, timetableOf],
  )

  if (!term) return <p className={styles.empty}>Chưa có học kỳ nào.</p>

  function exportCsv(t: Term) {
    const rows = groups.flatMap((g) =>
      g.sessions.map((s) => [
        g.maMonHoc,
        g.tenMonHoc,
        g.maLopHP,
        HINH_THUC[g.hinhThucHoc],
        s.thu === 8 ? 'CN' : s.thu,
        s.tietBatDau,
        s.soTiet,
        `${formatTime(s.gioBatDau)}-${formatTime(s.gioKetThuc)}`,
        s.phongHoc,
        s.tenGiangVien,
        shortDate(dayOf(t, s.tuanBatDau, s.thu)),
        shortDate(dayOf(t, s.tuanKetThuc, s.thu)),
        s.laDayBu ? 'Dạy bù' : null,
      ]),
    )
    downloadCsv([CSV_HEADER, ...rows], `${exportName}-${t.maHocKy}.csv`)
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Học kỳ"
          className={styles.termSelect}
          value={term.maHocKy}
          options={terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
          onChange={setMaHocKy}
        />
        <button type="button" className={styles.tool} onClick={() => window.print()}>
          <Icon name="printer" size="15px" />
          <span>In</span>
        </button>
        <button
          type="button"
          className={styles.tool}
          onClick={() => exportCsv(term)}
          disabled={groups.length === 0}
          title="Tải tệp CSV — mở trực tiếp bằng Excel"
        >
          <Icon name="download" size="15px" />
          <span>Xuất Excel</span>
        </button>
      </div>

      <p className={styles.printTitle}>{term.tenHocKy}</p>

      {groups.length === 0 ? (
        <p className={styles.empty}>Học kỳ này chưa có lớp nào trên thời khoá biểu.</p>
      ) : (
        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Mã MH</th>
                <th scope="col" className={styles.left}>
                  Tên môn học
                </th>
                <th scope="col">Lớp</th>
                <th scope="col">Thứ</th>
                <th scope="col">Tiết bắt đầu</th>
                <th scope="col">Số tiết</th>
                <th scope="col">Giờ học</th>
                <th scope="col">Phòng</th>
                <th scope="col" className={styles.left}>
                  Giảng viên
                </th>
                <th scope="col" className={styles.left}>
                  Thời gian học
                </th>
              </tr>
            </thead>
            {groups.map((g) => (
              /* Một <tbody> mỗi lớp: viền đậm tách lớp, hover cả nhóm. */
              <tbody key={g.maLopHP} className={styles.group}>
                {g.sessions.map((s, i) => (
                  <tr key={`${s.thu}-${s.tietBatDau}-${s.tuanBatDau}`}>
                    {i === 0 ? (
                      <>
                        <th scope="rowgroup" rowSpan={g.sessions.length} className={styles.code}>
                          {g.maMonHoc}
                        </th>
                        <td rowSpan={g.sessions.length} className={styles.name}>
                          {g.tenMonHoc}
                          {g.hinhThucHoc !== 'TRUC_TIEP' ? (
                            <span className={styles.mode}>{HINH_THUC[g.hinhThucHoc]}</span>
                          ) : null}
                        </td>
                        <td rowSpan={g.sessions.length} className={styles.center}>
                          {g.maLopHP}
                        </td>
                      </>
                    ) : null}
                    <td className={styles.center}>{s.thu === 8 ? 'CN' : s.thu}</td>
                    <td className={styles.center}>{s.tietBatDau}</td>
                    <td className={styles.center}>{s.soTiet}</td>
                    <td className={`${styles.center} ${styles.nowrap}`}>
                      {formatTime(s.gioBatDau)}–{formatTime(s.gioKetThuc)}
                    </td>
                    <td className={styles.center}>{s.phongHoc ?? '—'}</td>
                    <td>{s.tenGiangVien ?? 'Chưa phân công'}</td>
                    <td className={styles.dates}>
                      {shortDate(dayOf(term, s.tuanBatDau, s.thu))} đến{' '}
                      {shortDate(dayOf(term, s.tuanKetThuc, s.thu))}
                      {s.laDayBu ? <span className={styles.makeup}>Dạy bù</span> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            ))}
          </table>
        </div>
      )}
    </div>
  )
}

/** Gom buổi theo lớp; lớp theo mã môn, buổi theo ngày học đầu tiên. */
function groupByClass(entries: readonly TimetableEntry[]): ClassGroup[] {
  const map = new Map<string, ClassGroup>()
  for (const e of entries) {
    const g = map.get(e.maLopHP) ?? {
      maLopHP: e.maLopHP,
      maMonHoc: e.maMonHoc,
      tenMonHoc: e.tenMonHoc,
      hinhThucHoc: e.hinhThucHoc,
      sessions: [],
    }
    g.sessions.push(e)
    map.set(e.maLopHP, g)
  }
  const firstDay = (e: TimetableEntry) => e.tuanBatDau * 10 + e.thu
  const bySession = (a: TimetableEntry, b: TimetableEntry) =>
    firstDay(a) - firstDay(b) || a.tietBatDau - b.tietBatDau
  return [...map.values()]
    .map((g) => ({ ...g, sessions: g.sessions.sort(bySession) }))
    .sort((a, b) => a.maMonHoc.localeCompare(b.maMonHoc) || a.maLopHP.localeCompare(b.maLopHP))
}

/** `13/08/26` như cổng gốc. */
function shortDate(date: Date): string {
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: '2-digit' })
}
