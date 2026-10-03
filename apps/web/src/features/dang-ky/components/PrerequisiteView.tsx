import { useEffect, useMemo, useState } from 'react'

import { downloadCsv } from '@/shared/lib'
import { Icon, Select } from '@/shared/ui'

import * as api from '../api/mockEnrollmentApi'
import type { BestResults, CourseRelation, RelationKind } from '../types'
import styles from './PrerequisiteView.module.scss'

const KINDS: readonly { value: RelationKind; label: string; hint: string }[] = [
  { value: 'TIEN_QUYET', label: 'Tiên quyết', hint: 'Phải đạt môn yêu cầu trước khi đăng ký.' },
  {
    value: 'HOC_TRUOC',
    label: 'Học trước',
    hint: 'Phải đã học môn yêu cầu (đạt hay chưa đạt đều được).',
  },
  { value: 'SONG_HANH', label: 'Song hành', hint: 'Phải học môn yêu cầu trước hoặc cùng học kỳ.' },
]

const RESULT: Record<'DAT' | 'KHONG_DAT' | 'NONE', { label: string; tone: string }> = {
  DAT: { label: 'Đã đạt', tone: 'pass' },
  KHONG_DAT: { label: 'Chưa đạt', tone: 'fail' },
  NONE: { label: 'Chưa có kết quả', tone: 'none' },
}

/** Không phân biệt hoa thường và dấu: "tieng anh" khớp "Tiếng Anh". */
function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

export interface PrerequisiteViewProps {
  /** Tên tệp khi xuất, không gồm đuôi. */
  exportName: string
}

/**
 * Xem môn tiên quyết (F03, chỉ đọc): môn đăng ký ← môn yêu cầu, kèm kết quả
 * của chính sinh viên ở môn yêu cầu để biết đã đủ điều kiện chưa.
 */
export function PrerequisiteView({ exportName }: PrerequisiteViewProps) {
  const [relations, setRelations] = useState<CourseRelation[] | null>(null)
  const [results, setResults] = useState<BestResults>({})
  const [failed, setFailed] = useState(false)
  const [kind, setKind] = useState<RelationKind>('TIEN_QUYET')
  const [query, setQuery] = useState('')
  const [desc, setDesc] = useState(false)

  useEffect(() => {
    let cancelled = false
    Promise.all([api.listRelations(), api.myBestResults()]).then(
      ([r, best]) => {
        if (cancelled) return
        setRelations(r)
        setResults(best)
      },
      () => !cancelled && setFailed(true),
    )
    return () => {
      cancelled = true
    }
  }, [])

  const rows = useMemo(() => {
    const q = fold(query.trim())
    const sign = desc ? -1 : 1
    return (relations ?? [])
      .filter((r) => r.loai === kind)
      .filter(
        (r) =>
          !q ||
          [r.maMonHoc, r.tenMonHoc, r.maMonYeuCau, r.tenMonYeuCau].some((v) => fold(v).includes(q)),
      )
      .sort(
        (a, b) =>
          sign * (a.maMonHoc.localeCompare(b.maMonHoc) || a.maMonYeuCau.localeCompare(b.maMonYeuCau)),
      )
  }, [relations, kind, query, desc])

  const current = KINDS.find((k) => k.value === kind) ?? KINDS[0]!
  const resultOf = (ma: string) => RESULT[results[ma] ?? 'NONE']

  function exportCsv() {
    downloadCsv(
      [
        ['STT', 'Mã môn đăng ký', 'Tên môn đăng ký', 'Mã môn yêu cầu', 'Tên môn học yêu cầu', 'Kết quả môn yêu cầu'],
        ...rows.map((r, i) => [
          i + 1,
          r.maMonHoc,
          r.tenMonHoc,
          r.maMonYeuCau,
          r.tenMonYeuCau,
          resultOf(r.maMonYeuCau).label,
        ]),
      ],
      `${exportName}-${kind.toLowerCase().replace('_', '-')}.csv`,
    )
  }

  if (failed) return <p className={styles.muted}>Không tải được danh sách môn. Vui lòng thử lại.</p>
  if (!relations) return <p className={styles.muted}>Đang tải danh sách môn…</p>

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Loại điều kiện"
          className={styles.kindSelect}
          value={kind}
          options={KINDS.map(({ value, label }) => ({ value, label }))}
          onChange={setKind}
        />
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã hoặc tên môn…"
            aria-label="Tìm môn"
          />
        </label>
        <button type="button" className={styles.tool} onClick={() => window.print()}>
          <Icon name="printer" size="15px" />
          In
        </button>
        <button type="button" className={styles.tool} onClick={exportCsv} disabled={rows.length === 0}>
          <Icon name="download" size="15px" />
          Xuất Excel
        </button>
      </div>

      <p className={styles.hint}>
        <b>{current.label}:</b> {current.hint}
      </p>
      <p className={styles.printTitle}>Môn {current.label.toLowerCase()}</p>

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">STT</th>
              <th scope="col" aria-sort={desc ? 'descending' : 'ascending'}>
                <button type="button" className={styles.sortBtn} onClick={() => setDesc((d) => !d)}>
                  Mã môn đăng ký {desc ? '▼' : '▲'}
                </button>
              </th>
              <th scope="col" className={styles.left}>
                Tên môn đăng ký
              </th>
              <th scope="col">Mã môn yêu cầu</th>
              <th scope="col" className={styles.left}>
                Tên môn học yêu cầu
              </th>
              <th scope="col">Kết quả môn yêu cầu</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6} className={styles.empty}>
                  Không tìm thấy dữ liệu
                  {kind !== 'TIEN_QUYET' && !query ? (
                    <small>Chương trình đào tạo hiện chỉ quy định điều kiện tiên quyết.</small>
                  ) : null}
                </td>
              </tr>
            ) : (
              rows.map((r, i) => {
                const result = resultOf(r.maMonYeuCau)
                return (
                  <tr key={`${r.maMonHoc}-${r.maMonYeuCau}`}>
                    <td className={styles.center}>{i + 1}</td>
                    <td className={`${styles.center} ${styles.code}`}>{r.maMonHoc}</td>
                    <td>{r.tenMonHoc}</td>
                    <td className={`${styles.center} ${styles.code}`}>{r.maMonYeuCau}</td>
                    <td>{r.tenMonYeuCau}</td>
                    <td className={styles.center}>
                      <span className={`${styles.badge} ${styles[result.tone]}`}>{result.label}</span>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
