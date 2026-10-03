import type { TermReport } from '../types'
import styles from './StaffCards.module.scss'

export interface TermReportCardProps {
  title: string
  report: TermReport
  /** Admin Master: thêm bảng tách theo từng cơ sở. */
  byCampus?: readonly TermReport[]
}

const percent = (part: number, whole: number) => (whole ? Math.round((part / whole) * 1000) / 10 : 0)

/** Thống kê học kỳ (`ReportSummary`): sức chứa và tiến độ công bố điểm. */
export function TermReportCard({ title, report, byCampus }: TermReportCardProps) {
  const lapDay = percent(report.tongDaDangKy, report.tongSucChua)
  const tongLop = report.chuaCongBo + report.daCongBo + report.daKhoa
  const segments = [
    { key: 'daKhoa', label: 'Đã khoá', value: report.daKhoa },
    { key: 'daCongBo', label: 'Đã công bố', value: report.daCongBo },
    { key: 'chuaCongBo', label: 'Chưa công bố', value: report.chuaCongBo },
  ] as const

  return (
    <section className={styles.card}>
      <header className={styles.head}>
        <h3>{title}</h3>
        <span className={styles.count}>{report.tenPhamVi}</span>
      </header>

      <div className={styles.block}>
        <p className={styles.blockTitle}>
          Sức chứa <b>{lapDay}%</b>
          <small>
            {report.tongDaDangKy}/{report.tongSucChua} chỗ · {report.soLopDay} lớp đầy
          </small>
        </p>
        <span
          className={styles.meterLg}
          role="progressbar"
          aria-label="Tỉ lệ lấp đầy"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={lapDay}
        >
          <span style={{ width: `${lapDay}%` }} />
        </span>
      </div>

      <div className={styles.block}>
        <p className={styles.blockTitle}>
          Tiến độ bảng điểm
          <small>{tongLop} lớp</small>
        </p>
        <span className={styles.stack} aria-hidden="true">
          {segments.map((s) =>
            s.value ? (
              <span
                key={s.key}
                className={styles[s.key]}
                style={{ width: `${percent(s.value, tongLop)}%` }}
              />
            ) : null,
          )}
        </span>
        <ul className={styles.legend}>
          {segments.map((s) => (
            <li key={s.key}>
              <span className={`${styles.dot} ${styles[s.key]}`} />
              {s.label} <b>{s.value}</b>
            </li>
          ))}
        </ul>
      </div>

      {byCampus ? (
        <table className={styles.campusTable}>
          <thead>
            <tr>
              <th scope="col">Cơ sở</th>
              <th scope="col">Lớp</th>
              <th scope="col">Lượt ĐK</th>
              <th scope="col">Lấp đầy</th>
            </tr>
          </thead>
          <tbody>
            {byCampus.map((r) => (
              <tr key={r.maCoSo ?? 'all'}>
                <th scope="row">{r.tenPhamVi}</th>
                <td>{r.soLop}</td>
                <td>{r.luotDangKy}</td>
                <td>{percent(r.tongDaDangKy, r.tongSucChua)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </section>
  )
}
