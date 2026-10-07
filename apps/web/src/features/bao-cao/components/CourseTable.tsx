import { useMemo, useState } from 'react'

import { downloadCsv } from '@/shared/lib'
import { Icon } from '@/shared/ui'

import type { CourseRow } from '../types'
import styles from './CourseTable.module.scss'

type SortKey = 'maMonHoc' | 'soLop' | 'luotDangKy' | 'tiLeLapDay' | 'soDat' | 'soTruot' | 'chuaCoKetQua'

const COLUMNS: readonly { key: SortKey; label: string }[] = [
  { key: 'soLop', label: 'Số lớp' },
  { key: 'luotDangKy', label: 'Lượt ĐK' },
  { key: 'tiLeLapDay', label: 'Lấp đầy' },
  { key: 'soDat', label: 'Đạt' },
  { key: 'soTruot', label: 'Trượt' },
  { key: 'chuaCoKetQua', label: 'Chưa có KQ' },
]

function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

export interface CourseTableProps {
  rows: readonly CourseRow[]
  /** Tên tệp khi xuất, không gồm đuôi. */
  exportName: string
}

/** Chi tiết theo môn — cũng là bản bảng của các biểu đồ phía trên. */
export function CourseTable({ rows, exportName }: CourseTableProps) {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: 'tiLeLapDay', desc: true })

  const visible = useMemo(() => {
    const q = fold(query.trim())
    const sign = sort.desc ? -1 : 1
    return rows
      .filter((r) => !q || fold(`${r.maMonHoc} ${r.tenMonHoc}`).includes(q))
      .sort((a, b) => {
        const x = a[sort.key]
        const y = b[sort.key]
        if (typeof x === 'string' && typeof y === 'string') return sign * x.localeCompare(y)
        return sign * ((x ?? -1) as number) - sign * ((y ?? -1) as number)
      })
  }, [rows, query, sort])

  function toggle(key: SortKey) {
    setSort((s) => ({ key, desc: s.key === key ? !s.desc : key !== 'maMonHoc' }))
  }

  function exportCsv() {
    downloadCsv(
      [
        ['Mã MH', 'Tên môn học', 'Số lớp', 'Lượt ĐK', 'Sức chứa', 'Lấp đầy (%)', 'Đạt', 'Trượt', 'Chưa có KQ'],
        ...visible.map((r) => [
          r.maMonHoc,
          r.tenMonHoc,
          r.soLop,
          r.luotDangKy,
          r.tongSucChua,
          r.tiLeLapDay === null ? null : Math.round(r.tiLeLapDay * 1000) / 10,
          r.soDat,
          r.soTruot,
          r.chuaCoKetQua,
        ]),
      ],
      `${exportName}.csv`,
    )
  }

  const header = (key: SortKey, label: string, left = false) => (
    <th
      key={key}
      scope="col"
      className={left ? styles.left : undefined}
      aria-sort={sort.key === key ? (sort.desc ? 'descending' : 'ascending') : 'none'}
    >
      <button type="button" className={styles.sortBtn} onClick={() => toggle(key)}>
        {label}
        <span className={sort.key === key ? styles.on : styles.off} aria-hidden="true">
          {sort.key === key && sort.desc ? '▼' : '▲'}
        </span>
      </button>
    </th>
  )

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã hoặc tên môn…"
            aria-label="Tìm môn"
          />
        </label>
        <button type="button" className={styles.tool} onClick={exportCsv} disabled={visible.length === 0}>
          <Icon name="download" size="15px" />
          Xuất Excel
        </button>
      </div>

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              {header('maMonHoc', 'Mã MH')}
              <th scope="col" className={styles.left}>
                Tên môn học
              </th>
              {COLUMNS.map((c) => header(c.key, c.label))}
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 ? (
              <tr>
                <td colSpan={8} className={styles.empty}>
                  Không tìm thấy dữ liệu
                </td>
              </tr>
            ) : (
              visible.map((r) => (
                <tr key={r.maMonHoc}>
                  <td className={`${styles.num} ${styles.code}`}>{r.maMonHoc}</td>
                  <td>{r.tenMonHoc}</td>
                  <td className={styles.num}>{r.soLop}</td>
                  <td className={styles.num}>
                    {r.luotDangKy}
                    <small>/{r.tongSucChua}</small>
                  </td>
                  <td className={styles.num}>
                    <span className={styles.fill}>
                      <span className={styles.track}>
                        <span style={{ width: `${(r.tiLeLapDay ?? 0) * 100}%` }} />
                      </span>
                      {r.tiLeLapDay === null ? '—' : `${Math.round(r.tiLeLapDay * 1000) / 10}%`}
                    </span>
                  </td>
                  <td className={styles.num}>{r.soDat}</td>
                  <td className={`${styles.num} ${r.soTruot ? styles.bad : ''}`}>{r.soTruot}</td>
                  <td className={`${styles.num} ${styles.muted}`}>{r.chuaCoKetQua}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
