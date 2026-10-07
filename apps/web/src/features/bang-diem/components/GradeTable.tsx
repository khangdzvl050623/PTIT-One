import { Fragment, useMemo, useState } from 'react'

import { TRONG_SO, diemChu, diemHe4 } from '@/shared/lib'
import { Icon } from '@/shared/ui'

import { downloadGradesCsv } from '../lib/exportCsv'
import { summarize } from '../lib/grading'
import type { StudentGrade, TermSummary } from '../types'
import styles from './GradeTable.module.scss'

type SortKey = 'maMonHoc' | 'tenMonHoc' | 'soTinChi' | 'diemTongKet'
type SortDir = 'asc' | 'desc'

const SORTABLE: Record<SortKey, string> = {
  maMonHoc: 'Mã MH',
  tenMonHoc: 'Tên môn học',
  soTinChi: 'Số tín chỉ',
  diemTongKet: 'Điểm TK (10)',
}

/** Số cột của bảng — dòng tiêu đề kỳ, tổng kết và chi tiết trải hết bề ngang. */
const COLS = 11

export interface GradeTableProps {
  grades: readonly StudentGrade[]
  /** Tên tệp khi xuất, không gồm đuôi. */
  exportName: string
}

/** Bảng điểm theo học kỳ (F07): mỗi kỳ một nhóm, cuối nhóm là dòng tổng kết. */
export function GradeTable({ grades, exportName }: GradeTableProps) {
  const terms = useMemo(() => summarize(grades), [grades])
  const [sort, setSort] = useState<{ key: SortKey; dir: SortDir }>({ key: 'maMonHoc', dir: 'asc' })
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set())

  function toggleSort(key: SortKey) {
    setSort((s) => ({ key, dir: s.key === key && s.dir === 'asc' ? 'desc' : 'asc' }))
  }

  function toggleDetail(id: string) {
    setOpen((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <button type="button" className={styles.tool} onClick={() => window.print()}>
          <Icon name="printer" size="15px" />
          <span>In</span>
        </button>
        <button
          type="button"
          className={styles.tool}
          onClick={() => downloadGradesCsv(terms, `${exportName}.csv`)}
          title="Tải tệp CSV — mở trực tiếp bằng Excel"
        >
          <Icon name="download" size="15px" />
          <span>Xuất Excel</span>
        </button>
      </div>

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col" className={styles.narrow}>
                STT
              </th>
              {sortHeader('maMonHoc')}
              <th scope="col">Lớp</th>
              {sortHeader('tenMonHoc', styles.left)}
              {sortHeader('soTinChi')}
              <th scope="col">Điểm thi</th>
              {sortHeader('diemTongKet')}
              <th scope="col">Điểm TK (4)</th>
              <th scope="col">Điểm TK (C)</th>
              <th scope="col">Kết quả</th>
              <th scope="col" className={styles.narrow} data-print="hide">
                Chi tiết
              </th>
            </tr>
          </thead>

          {terms.map((term) => (
            <tbody key={term.maHocKy}>
              <tr className={styles.termRow}>
                <th scope="colgroup" colSpan={COLS}>
                  {term.tenHocKy}
                </th>
              </tr>
              {sorted(term.grades, sort.key, sort.dir).map((g, i) => {
                const id = `${g.maHocKy}:${g.maLopHP}`
                const expanded = open.has(id)
                return (
                  <Fragment key={id}>
                    <tr className={expanded ? styles.rowOpen : undefined}>
                      <td className={styles.center}>{i + 1}</td>
                      <td className={styles.center}>{g.maMonHoc}</td>
                      <td className={styles.center} title={g.maLopHP}>
                        {nhomLop(g.maLopHP)}
                      </td>
                      <td>{g.tenMonHoc}</td>
                      <td className={styles.center}>{g.soTinChi}</td>
                      <td className={styles.center}>{fmt(g.diemCuoiKy, 1)}</td>
                      <td className={`${styles.center} ${styles.strong}`}>{fmt(g.diemTongKet, 1)}</td>
                      <td className={styles.center}>
                        {g.diemTongKet === null ? '—' : fmt(diemHe4(g.diemTongKet), 1)}
                      </td>
                      <td className={styles.center}>
                        {g.diemTongKet === null ? '—' : diemChu(g.diemTongKet)}
                      </td>
                      <td className={styles.center}>
                        <ResultBadge grade={g} />
                      </td>
                      <td className={styles.center} data-print="hide">
                        <button
                          type="button"
                          className={styles.detailBtn}
                          aria-expanded={expanded}
                          aria-label={`Chi tiết điểm ${g.tenMonHoc}`}
                          onClick={() => toggleDetail(id)}
                        >
                          <Icon
                            name="chevronDown"
                            size="12px"
                            className={expanded ? styles.caretOpen : styles.caret}
                          />
                        </button>
                      </td>
                    </tr>
                    {expanded ? (
                      <tr className={styles.detailRow}>
                        <td colSpan={COLS}>
                          <Detail grade={g} />
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                )
              })}
              <tr className={styles.summaryRow}>
                <td colSpan={COLS}>
                  <Summary term={term} />
                </td>
              </tr>
            </tbody>
          ))}
        </table>
      </div>
    </div>
  )

  function sortHeader(key: SortKey, className?: string) {
    const active = sort.key === key
    return (
      <th
        scope="col"
        className={className}
        aria-sort={active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none'}
      >
        <button type="button" className={styles.sortBtn} onClick={() => toggleSort(key)}>
          {SORTABLE[key]}
          <span className={active ? styles.sortOn : styles.sortOff} aria-hidden="true">
            {active && sort.dir === 'desc' ? '▼' : '▲'}
          </span>
        </button>
      </th>
    )
  }
}

function ResultBadge({ grade }: { grade: StudentGrade }) {
  if (grade.ketQua === 'DAT') return <span className={`${styles.badge} ${styles.pass}`}>Đạt</span>
  if (grade.ketQua === 'KHONG_DAT') {
    return <span className={`${styles.badge} ${styles.fail}`}>Không đạt</span>
  }
  return <span className={styles.pending}>Chưa có điểm</span>
}

function Detail({ grade }: { grade: StudentGrade }) {
  if (!grade.daCongBo) {
    return <p className={styles.detailNote}>Điểm môn này chưa được công bố.</p>
  }
  const parts = [
    ['Chuyên cần', grade.diemChuyenCan, TRONG_SO.chuyenCan],
    ['Giữa kỳ', grade.diemGiuaKy, TRONG_SO.giuaKy],
    ['Cuối kỳ', grade.diemCuoiKy, TRONG_SO.cuoiKy],
  ] as const
  return (
    <div className={styles.detail}>
      {parts.map(([label, diem, w]) => (
        <span key={label} className={styles.part}>
          {label} <small>({Math.round(w * 100)}%)</small>
          <b>{fmt(diem, 1)}</b>
        </span>
      ))}
      <span className={styles.part}>
        Tổng kết
        <b>{fmt(grade.diemTongKet, 1)}</b>
      </span>
      <span className={styles.detailNote}>
        Lớp {grade.maLopHP}
        {grade.ngayCongBo
          ? ` · công bố ${new Date(grade.ngayCongBo).toLocaleDateString('vi-VN')}`
          : ''}
      </span>
    </div>
  )
}

function Summary({ term }: { term: TermSummary }) {
  return (
    <dl className={styles.summary}>
      <div>
        <dt>Điểm trung bình học kỳ hệ 4:</dt>
        <dd>{fmt(term.tbHocKy4, 2)}</dd>
      </div>
      <div>
        <dt>Điểm trung bình tích luỹ hệ 4:</dt>
        <dd>{fmt(term.tbTichLuy4, 2)}</dd>
      </div>
      <div>
        <dt>Phân loại điểm trung bình HK:</dt>
        <dd>{term.xepLoaiHocKy ?? '—'}</dd>
      </div>
      <div>
        <dt>Điểm trung bình học kỳ hệ 10:</dt>
        <dd>{fmt(term.tbHocKy10, 2)}</dd>
      </div>
      <div>
        <dt>Điểm trung bình tích luỹ hệ 10:</dt>
        <dd>{fmt(term.tbTichLuy10, 2)}</dd>
      </div>
      <div />
      <div>
        <dt>Số tín chỉ đạt học kỳ:</dt>
        <dd>{term.tbHocKy4 === null ? '—' : term.tinChiDatHocKy}</dd>
      </div>
      <div>
        <dt>Số tín chỉ tích luỹ:</dt>
        <dd>{term.tinChiTichLuy}</dd>
      </div>
    </dl>
  )
}

/** `null` hiện "—" — không bao giờ in 0 cho điểm chưa có. */
function fmt(value: number | null, digits: number): string {
  return value === null ? '—' : value.toFixed(digits)
}

/** `INT1339-2026-1-HCM01` → `HCM01` (nhóm lớp); mã đầy đủ ở tooltip. */
function nhomLop(maLopHP: string): string {
  return maLopHP.slice(maLopHP.lastIndexOf('-') + 1)
}

function sorted(list: readonly StudentGrade[], key: SortKey, dir: SortDir): StudentGrade[] {
  const sign = dir === 'asc' ? 1 : -1
  return [...list].sort((a, b) => {
    const x = a[key]
    const y = b[key]
    // Chưa có điểm luôn nằm cuối, dù sắp tăng hay giảm.
    if (x === null) return y === null ? 0 : 1
    if (y === null) return -1
    return (typeof x === 'number' && typeof y === 'number'
      ? x - y
      : String(x).localeCompare(String(y), 'vi')) * sign
  })
}
