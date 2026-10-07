import { Fragment, useEffect, useMemo, useState } from 'react'

import { downloadCsv } from '@/shared/lib'
import { Dialog, Icon, Select } from '@/shared/ui'

import * as api from '../api/enrollmentApi'
import { PLAN_TERMS, TERM_NAMES, facultyOf } from '../data/demo'
import type { CourseRelation, ProgramCourse, StudentProgram, StudyRecord } from '../types'
import styles from './ProgramView.module.scss'

type Mode = 'KE_HOACH' | 'THUC_HIEN'
type Status = 'DAT' | 'DANG_HOC' | 'KHONG_DAT' | 'CHUA_HOC'

const MODES: readonly { value: Mode; label: string }[] = [
  { value: 'KE_HOACH', label: 'CTĐT kế hoạch' },
  { value: 'THUC_HIEN', label: 'CTĐT thực hiện' },
]

const STATUS: Record<Status, string> = {
  DAT: 'Đã đạt',
  DANG_HOC: 'Đang học',
  KHONG_DAT: 'Chưa đạt',
  CHUA_HOC: 'Chưa học',
}

/** Một khối trong bảng: tiêu đề học kỳ + các dòng môn. */
interface Group<T> {
  key: string
  title: string
  subtitle: string | null
  rows: T[]
}

export interface ProgramViewProps {
  /** Lịch sử học — rút từ `GET /api/me/grades`, mỗi lần học một dòng. */
  history: readonly StudyRecord[]
  /** Tên tệp khi xuất, không gồm đuôi. */
  exportName: string
}

/**
 * Chương trình đào tạo: **kế hoạch** (môn của CTĐT theo học kỳ gợi ý) và
 * **thực hiện** (môn đã thực sự học, theo học kỳ đã học — kể cả học lại và
 * môn ngoài chương trình).
 */
export function ProgramView({ history, exportName }: ProgramViewProps) {
  const [program, setProgram] = useState<StudentProgram | null>(null)
  const [relations, setRelations] = useState<CourseRelation[]>([])
  const [failed, setFailed] = useState(false)
  const [mode, setMode] = useState<Mode>('KE_HOACH')
  const [desc, setDesc] = useState(false)
  const [detail, setDetail] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([api.myProgram(), api.listRelations()]).then(
      ([p, r]) => {
        if (cancelled) return
        setProgram(p)
        setRelations(r)
      },
      () => !cancelled && setFailed(true),
    )
    return () => {
      cancelled = true
    }
  }, [])

  /** Trạng thái tốt nhất mỗi môn: đạt > đang học > chưa đạt > chưa học. */
  const status = useMemo(() => {
    const rank: Record<Status, number> = { DAT: 3, DANG_HOC: 2, KHONG_DAT: 1, CHUA_HOC: 0 }
    const map = new Map<string, Status>()
    for (const r of history) {
      const s: Status = r.ketQua === null ? 'DANG_HOC' : r.ketQua
      const prev = map.get(r.maMonHoc) ?? 'CHUA_HOC'
      if (rank[s] > rank[prev]) map.set(r.maMonHoc, s)
    }
    return map
  }, [history])
  const statusOf = (ma: string): Status => status.get(ma) ?? 'CHUA_HOC'

  const courseMap = useMemo(
    () => new Map((program?.monHoc ?? []).map((m) => [m.maMonHoc, m])),
    [program],
  )

  const planGroups = useMemo<Group<ProgramCourse>[]>(() => {
    const byKy = new Map<number, ProgramCourse[]>()
    for (const m of program?.monHoc ?? []) {
      const ky = m.hocKyGoiY ?? 0
      byKy.set(ky, [...(byKy.get(ky) ?? []), m])
    }
    return [...byKy.keys()]
      .sort((a, b) => (a || 99) - (b || 99))
      .map((ky) => {
        const rows = byCode(byKy.get(ky) ?? [], desc)
        const maHocKy = PLAN_TERMS[ky - 1]
        return {
          key: `ky-${ky}`,
          title: ky ? `Học kỳ ${ky}` : 'Chưa xếp học kỳ',
          subtitle: maHocKy ? (TERM_NAMES[maHocKy] ?? maHocKy) : null,
          rows,
        }
      })
  }, [program, desc])

  const doneGroups = useMemo<Group<StudyRecord>[]>(() => {
    const byTerm = new Map<string, StudyRecord[]>()
    for (const r of history) byTerm.set(r.maHocKy, [...(byTerm.get(r.maHocKy) ?? []), r])
    // Mã `yyyy-yyyy-HKn` nên so chuỗi là đúng thứ tự thời gian.
    return [...byTerm.keys()].sort().map((ma) => ({
      key: ma,
      title: byTerm.get(ma)?.[0]?.tenHocKy ?? ma,
      subtitle: null,
      rows: byCode(byTerm.get(ma) ?? [], desc),
    }))
  }, [history, desc])

  if (failed) return <p className={styles.muted}>Không tải được chương trình đào tạo.</p>
  if (!program) return <p className={styles.muted}>Đang tải chương trình đào tạo…</p>

  /** Lần học trước đó của cùng môn (để gắn nhãn "Học lại"). */
  const isRetake = (r: StudyRecord) =>
    history.some((h) => h.maMonHoc === r.maMonHoc && h.maHocKy < r.maHocKy)

  function exportCsv() {
    const rows =
      mode === 'KE_HOACH'
        ? [
            ['Học kỳ', 'Mã MH', 'Tên môn học', 'Số tín chỉ', 'Bắt buộc', 'Trạng thái'],
            ...planGroups.flatMap((g) =>
              g.rows.map((m) => [
                g.title,
                m.maMonHoc,
                m.tenMonHoc,
                m.soTinChi,
                m.batBuoc ? 'x' : '',
                STATUS[statusOf(m.maMonHoc)],
              ]),
            ),
          ]
        : [
            ['Học kỳ', 'Mã MH', 'Tên môn học', 'Số tín chỉ', 'Điểm TK', 'Kết quả', 'Ghi chú'],
            ...doneGroups.flatMap((g) =>
              g.rows.map((r) => [
                g.title,
                r.maMonHoc,
                r.tenMonHoc,
                r.soTinChi,
                r.diemTongKet,
                r.ketQua === null ? 'Đang học' : r.ketQua === 'DAT' ? 'Đạt' : 'Không đạt',
                [isRetake(r) ? 'Học lại' : '', courseMap.has(r.maMonHoc) ? '' : 'Ngoài CTĐT']
                  .filter(Boolean)
                  .join('; '),
              ]),
            ),
          ]
    downloadCsv(rows, `${exportName}-${mode === 'KE_HOACH' ? 'ke-hoach' : 'thuc-hien'}.csv`)
  }

  const detailCourse = detail ? courseMap.get(detail) : undefined
  const detailName =
    detailCourse?.tenMonHoc ?? history.find((h) => h.maMonHoc === detail)?.tenMonHoc ?? detail

  return (
    <div className={styles.wrap}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Loại chương trình"
          className={styles.modeSelect}
          value={mode}
          options={[...MODES]}
          onChange={setMode}
        />
        <span className={styles.spacer} />
        <button type="button" className={styles.tool} onClick={() => window.print()}>
          <Icon name="printer" size="15px" />
          In
        </button>
        <button type="button" className={styles.tool} onClick={exportCsv}>
          <Icon name="download" size="15px" />
          Xuất Excel
        </button>
      </div>

      <p className={styles.printTitle}>{MODES.find((m) => m.value === mode)?.label}</p>

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">STT</th>
              <th scope="col" aria-sort={desc ? 'descending' : 'ascending'}>
                <button type="button" className={styles.sortBtn} onClick={() => setDesc((d) => !d)}>
                  Mã MH {desc ? '▼' : '▲'}
                </button>
              </th>
              <th scope="col" className={styles.left}>
                Tên môn học
              </th>
              <th scope="col">Số tín chỉ</th>
              {mode === 'KE_HOACH' ? (
                <>
                  <th scope="col">Môn bắt buộc</th>
                  <th scope="col">Trạng thái</th>
                </>
              ) : (
                <>
                  <th scope="col">Điểm TK</th>
                  <th scope="col">Kết quả</th>
                  <th scope="col">Ghi chú</th>
                </>
              )}
              <th scope="col" data-print="hide">
                Chi tiết
              </th>
            </tr>
          </thead>
          <tbody>
            {mode === 'KE_HOACH'
              ? planGroups.map((g) => (
                  <Fragment key={g.key}>
                    <GroupRow
                      title={g.title}
                      subtitle={g.subtitle}
                      credits={g.rows.reduce((s, m) => s + m.soTinChi, 0)}
                      cols={7}
                    />
                    {g.rows.map((m, i) => (
                      <tr key={m.maMonHoc}>
                        <td className={styles.center}>{i + 1}</td>
                        <td className={`${styles.center} ${styles.code}`}>{m.maMonHoc}</td>
                        <td>{m.tenMonHoc}</td>
                        <td className={styles.center}>{m.soTinChi}</td>
                        <td className={styles.center}>
                          {m.batBuoc ? (
                            <Icon name="check" size="14px" className={styles.mandatory} />
                          ) : (
                            <span className={styles.elective}>Tự chọn</span>
                          )}
                        </td>
                        <td className={styles.center}>
                          <StatusBadge status={statusOf(m.maMonHoc)} />
                        </td>
                        <td className={styles.center} data-print="hide">
                          <DetailButton name={m.tenMonHoc} onClick={() => setDetail(m.maMonHoc)} />
                        </td>
                      </tr>
                    ))}
                  </Fragment>
                ))
              : doneGroups.map((g) => (
                  <Fragment key={g.key}>
                    <GroupRow
                      title={g.title}
                      subtitle={null}
                      credits={g.rows.reduce((s, r) => s + r.soTinChi, 0)}
                      cols={8}
                    />
                    {g.rows.map((r, i) => (
                      <tr key={`${r.maHocKy}-${r.maMonHoc}`}>
                        <td className={styles.center}>{i + 1}</td>
                        <td className={`${styles.center} ${styles.code}`}>{r.maMonHoc}</td>
                        <td>{r.tenMonHoc}</td>
                        <td className={styles.center}>{r.soTinChi}</td>
                        <td className={`${styles.center} ${styles.code}`}>
                          {r.diemTongKet === null ? '—' : r.diemTongKet.toFixed(1)}
                        </td>
                        <td className={styles.center}>
                          <StatusBadge status={r.ketQua ?? 'DANG_HOC'} short />
                        </td>
                        <td className={styles.center}>
                          {isRetake(r) ? <span className={styles.tag}>Học lại</span> : null}
                          {courseMap.has(r.maMonHoc) ? null : (
                            <span className={styles.tag}>Ngoài CTĐT</span>
                          )}
                        </td>
                        <td className={styles.center} data-print="hide">
                          <DetailButton name={r.tenMonHoc} onClick={() => setDetail(r.maMonHoc)} />
                        </td>
                      </tr>
                    ))}
                  </Fragment>
                ))}
          </tbody>
        </table>
      </div>

      <Dialog
        open={detail !== null}
        onClose={() => setDetail(null)}
        ariaLabel={`Chi tiết môn ${detailName ?? ''}`}
        header={
          detail ? (
            <dl className={styles.dialogHead}>
              <dt>Mã môn học:</dt>
              <dd>{detail}</dd>
              <dt>Tên môn học:</dt>
              <dd>{detailName}</dd>
            </dl>
          ) : null
        }
      >
        {detail ? (
          <CourseDetail
            maMonHoc={detail}
            course={detailCourse}
            relations={relations.filter((r) => r.maMonHoc === detail)}
            records={history.filter((h) => h.maMonHoc === detail)}
            statusOf={statusOf}
          />
        ) : null}
      </Dialog>
    </div>
  )
}

function GroupRow(props: { title: string; subtitle: string | null; credits: number; cols: number }) {
  return (
    <tr className={styles.group}>
      <th scope="rowgroup" colSpan={props.cols}>
        {props.title}
        {props.subtitle ? <span className={styles.groupSub}> · {props.subtitle}</span> : null}
        <span className={styles.groupCredits}>{props.credits} tín chỉ</span>
      </th>
    </tr>
  )
}

function StatusBadge({ status, short = false }: { status: Status; short?: boolean }) {
  const label = short && status === 'DAT' ? 'Đạt' : short && status === 'KHONG_DAT' ? 'Không đạt' : STATUS[status]
  return <span className={`${styles.badge} ${styles[status]}`}>{label}</span>
}

function DetailButton({ name, onClick }: { name: string; onClick: () => void }) {
  return (
    <button
      type="button"
      className={styles.listBtn}
      onClick={onClick}
      aria-label={`Chi tiết môn ${name}`}
      aria-haspopup="dialog"
    >
      <Icon name="list" size="18px" />
    </button>
  )
}

interface CourseDetailProps {
  maMonHoc: string
  course: ProgramCourse | undefined
  relations: readonly CourseRelation[]
  records: readonly StudyRecord[]
  statusOf: (ma: string) => Status
}

/** Nội dung popup ☰ — thay cho "Tiết thành phần" (hệ thống không lưu số tiết). */
function CourseDetail({ maMonHoc, course, relations, records, statusOf }: CourseDetailProps) {
  const ky = course?.hocKyGoiY ?? null
  const maHocKy = ky ? PLAN_TERMS[ky - 1] : undefined
  return (
    <div className={styles.detail}>
      <dl className={styles.facts}>
        <dt>Số tín chỉ</dt>
        <dd>{course?.soTinChi ?? records[0]?.soTinChi ?? '—'}</dd>
        <dt>Loại môn</dt>
        <dd>{course ? (course.batBuoc ? 'Bắt buộc' : 'Tự chọn') : 'Ngoài chương trình đào tạo'}</dd>
        <dt>Khoa quản lý</dt>
        <dd>{facultyOf(maMonHoc).tenKhoa}</dd>
        <dt>Học kỳ gợi ý</dt>
        <dd>
          {ky ? `Học kỳ ${ky}` : '—'}
          {maHocKy ? ` · ${TERM_NAMES[maHocKy] ?? maHocKy}` : ''}
        </dd>
        <dt>Trạng thái của bạn</dt>
        <dd>
          <StatusBadge status={statusOf(maMonHoc)} />
        </dd>
      </dl>

      <h3 className={styles.detailTitle}>Môn tiên quyết</h3>
      {relations.length === 0 ? (
        <p className={styles.muted}>Không có.</p>
      ) : (
        <ul className={styles.list}>
          {relations.map((r) => (
            <li key={r.maMonYeuCau}>
              <span>
                <b>{r.maMonYeuCau}</b> {r.tenMonYeuCau}
              </span>
              <StatusBadge status={statusOf(r.maMonYeuCau)} />
            </li>
          ))}
        </ul>
      )}

      <h3 className={styles.detailTitle}>Lịch sử học</h3>
      {records.length === 0 ? (
        <p className={styles.muted}>Chưa học môn này.</p>
      ) : (
        <ul className={styles.list}>
          {[...records]
            .sort((a, b) => a.maHocKy.localeCompare(b.maHocKy))
            .map((r) => (
              <li key={r.maHocKy}>
                <span>{r.tenHocKy}</span>
                <span>
                  {r.diemTongKet === null ? '' : <b>{r.diemTongKet.toFixed(1)} </b>}
                  <StatusBadge status={r.ketQua ?? 'DANG_HOC'} short />
                </span>
              </li>
            ))}
        </ul>
      )}
    </div>
  )
}

/** Sắp theo mã môn trong từng khối học kỳ. */
function byCode<T extends { maMonHoc: string }>(rows: T[], desc: boolean): T[] {
  return rows.sort((a, b) => (desc ? -1 : 1) * a.maMonHoc.localeCompare(b.maMonHoc))
}
