import { useEffect, useState } from 'react'

import { ApiError } from '@/shared/api'
import { Icon, Panel, Select } from '@/shared/ui'
import type { IconName } from '@/shared/ui'

import { useAsyncData } from '@/shared/lib'

import * as api from '../api/reportApi'

import type { CourseRow, ReportSummary } from '../types'
import { CampusCompare, FillChart, GradeDistribution, GradeProgress } from './Charts'
import { CourseTable } from './CourseTable'
import styles from './Dashboard.module.scss'

/** Giá trị "toàn hệ thống" của ô chọn cơ sở (API dùng `maCoSo = null`). */
const ALL = ''

interface Loaded {
  summary: ReportSummary
  courses: readonly CourseRow[]
  /** Chỉ khi Admin Master xem toàn hệ thống. */
  campuses: readonly { tenCoSo: string; summary: ReportSummary }[] | null
}

export interface DashboardProps {
  /**
   * Cơ sở của admin cơ sở — khoá phạm vi (server trả 403 nếu xin cơ sở khác).
   * `null` = Admin Master, được chọn cơ sở hoặc toàn hệ thống.
   */
  campusLock: string | null
  /** Tên tệp khi xuất bảng, không gồm đuôi. */
  exportName: string
}

/** Hai danh mục chỉ cần tải một lần; `useAsyncData` đòi hàm ổn định. */
async function loadLookups() {
  const [terms, campuses] = await Promise.all([api.listTerms(), api.listCampuses()])
  return { terms, campuses }
}

/** Dashboard quản trị — `GET /api/reports/summary` + `/courses` theo học kỳ và cơ sở. */
export function Dashboard({ campusLock, exportName }: DashboardProps) {
  /* Học kỳ và cơ sở lấy từ API (`/api/terms`, `/api/campuses`), không viết cứng:
     danh sách hằng số sẽ lệch ngay khi nhà trường mở học kỳ mới. */
  const lookups = useAsyncData(loadLookups)
  const terms = lookups.data?.terms ?? []
  const campuses = lookups.data?.campuses ?? []

  const [maHocKy, setMaHocKy] = useState('')
  const [maCoSo, setMaCoSo] = useState<string>(campusLock ?? ALL)
  const [data, setData] = useState<Loaded | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  // Chọn kỳ mới nhất ngay khi có danh sách; người dùng đổi sau thì giữ lựa chọn của họ.
  useEffect(() => {
    if (!maHocKy && terms.length > 0) setMaHocKy(terms[0]!.maHocKy)
  }, [maHocKy, terms])

  useEffect(() => {
    if (!maHocKy) return
    let cancelled = false
    const scope = maCoSo === ALL ? null : maCoSo
    setLoading(true)
    setError(null)
    Promise.all([
      api.getSummary(maHocKy, scope, campusLock),
      api.getCourseReport(maHocKy, scope, campusLock),
      scope === null
        ? Promise.all(
            campuses.map(async (c) => ({
              tenCoSo: c.tenCoSo,
              summary: await api.getSummary(maHocKy, c.maCoSo, campusLock),
            })),
          )
        : Promise.resolve(null),
    ])
      .then(([summary, courses, campuses]) => {
        if (!cancelled) setData({ summary, courses: courses.monHoc, campuses })
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof ApiError ? cause.message : 'Không tải được thống kê.')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [maHocKy, maCoSo, campusLock])

  const tenPhamVi =
    maCoSo === ALL ? 'Toàn hệ thống' : (campuses.find((c) => c.maCoSo === maCoSo)?.tenCoSo ?? maCoSo)
  const tenHocKy = terms.find((t) => t.maHocKy === maHocKy)?.tenHocKy ?? maHocKy

  return (
    <div className={styles.dashboard}>
      <div className={styles.filters}>
        <Select
          ariaLabel="Học kỳ"
          className={styles.filter}
          value={maHocKy}
          options={terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
          onChange={setMaHocKy}
        />
        {campusLock === null ? (
          <Select
            ariaLabel="Cơ sở"
            className={styles.filter}
            value={maCoSo}
            options={[
              { value: ALL, label: 'Toàn hệ thống' },
              ...campuses.map((c) => ({ value: c.maCoSo, label: c.tenCoSo })),
            ]}
            onChange={setMaCoSo}
          />
        ) : (
          /* Admin cơ sở: phạm vi cố định theo JWT — hiện rõ, không cho chọn. */
          <span className={styles.lock}>
            <Icon name="lock" size="13px" /> {tenPhamVi}
          </span>
        )}
        <span className={styles.updated}>
          {loading
            ? 'Đang tải…'
            : data
              ? `Cập nhật lúc ${new Date(data.summary.capNhatLuc).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}`
              : ''}
        </span>
      </div>

      {error ? (
        <p className={styles.error} role="alert">
          {error}
        </p>
      ) : null}

      {data ? (
        <div className={`${styles.content} ${loading ? styles.stale : ''}`}>
          <h2 className={styles.scope}>
            {tenPhamVi} <span>· {tenHocKy}</span>
          </h2>

          <div className={styles.kpis}>
            <Kpi icon="chalkboard" label="Lớp học phần" value={data.summary.dangKy.soLop} />
            <Kpi
              icon="users"
              label="Lượt đăng ký"
              value={data.summary.dangKy.luotDangKy}
              hint="Một SV học 6 môn là 6 lượt"
            />
            <Kpi icon="graduate" label="Sinh viên" value={data.summary.dangKy.soSinhVien} />
            <Kpi
              icon="book"
              label="Tỉ lệ lấp đầy"
              value={
                data.summary.sucChua.tiLeLapDay === null
                  ? '—'
                  : `${Math.round(data.summary.sucChua.tiLeLapDay * 1000) / 10}%`
              }
              hint={`${data.summary.sucChua.tongDaDangKy}/${data.summary.sucChua.tongSucChua} chỗ`}
            />
            <Kpi icon="lock" label="Lớp đầy" value={data.summary.sucChua.soLopDay} tone="warm" />
            <Kpi icon="check" label="Lớp còn chỗ" value={data.summary.sucChua.soLopConCho} />
          </div>

          <div className={styles.grid}>
            <Panel title="TỈ LỆ LẤP ĐẦY THEO MÔN" icon="chalkboard" className={styles.wide}>
              <FillChart rows={data.courses} />
            </Panel>
            <div className={styles.side}>
              <Panel title="PHÂN BỐ ĐIỂM TỔNG KẾT" icon="graduate">
                <GradeDistribution rows={data.courses} />
              </Panel>
              <Panel title="TIẾN ĐỘ BẢNG ĐIỂM" icon="book">
                <GradeProgress summary={data.summary} />
              </Panel>
            </div>
          </div>

          {data.campuses ? (
            <Panel title="SO SÁNH CƠ SỞ" icon="users">
              <CampusCompare items={data.campuses} />
            </Panel>
          ) : null}

          <Panel title="CHI TIẾT THEO MÔN" icon="list">
            <CourseTable
              rows={data.courses}
              exportName={`${exportName}-${maHocKy}-${maCoSo || 'toan-he-thong'}`}
            />
          </Panel>
        </div>
      ) : loading ? (
        <p className={styles.muted}>Đang tải thống kê…</p>
      ) : null}
    </div>
  )
}

function Kpi(props: {
  icon: IconName
  label: string
  value: number | string
  hint?: string
  tone?: 'warm'
}) {
  return (
    <div className={`${styles.kpi} ${props.tone === 'warm' ? styles.kpiWarm : ''}`}>
      <span className={styles.kpiIcon}>
        <Icon name={props.icon} size="18px" />
      </span>
      <span className={styles.kpiBody}>
        <small>{props.label}</small>
        <strong>{props.value}</strong>
        {props.hint ? <em>{props.hint}</em> : null}
      </span>
    </div>
  )
}
