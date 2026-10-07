import { GRADE_BUCKETS } from '../types'
import type { CourseRow, ReportSummary } from '../types'
import styles from './Charts.module.scss'

const pct = (ratio: number | null) => (ratio === null ? '—' : `${Math.round(ratio * 1000) / 10}%`)

/**
 * Tỉ lệ lấp đầy theo môn — thanh ngang, một chuỗi nên một màu, nhãn số ở đầu
 * thanh. Sắp giảm dần để thấy ngay môn sắp/đã kín chỗ.
 */
export function FillChart({ rows }: { rows: readonly CourseRow[] }) {
  const sorted = [...rows].sort((a, b) => (b.tiLeLapDay ?? 0) - (a.tiLeLapDay ?? 0))
  if (sorted.length === 0) return <p className={styles.empty}>Không có lớp nào trong phạm vi này.</p>

  return (
    <ul className={styles.hbars} aria-label="Tỉ lệ lấp đầy theo môn">
      {sorted.map((r, i) => {
        const full = r.tongDaDangKy >= r.tongSucChua
        return (
          <li
            key={r.maMonHoc}
            className={styles.hrow}
            style={{ animationDelay: `${i * 30}ms` }}
            title={`${r.maMonHoc} · ${r.tenMonHoc}\n${r.tongDaDangKy}/${r.tongSucChua} chỗ · ${r.soLop} lớp`}
          >
            <span className={styles.hlabel}>
              <b>{r.maMonHoc}</b>
              <small>{r.tenMonHoc}</small>
            </span>
            <span className={styles.htrack}>
              <span className={styles.hfill} style={{ width: `${(r.tiLeLapDay ?? 0) * 100}%` }} />
            </span>
            <span className={styles.hvalue}>
              {pct(r.tiLeLapDay)}
              {full ? <em className={styles.full}>Đầy</em> : null}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

/** Phân bố điểm tổng kết đã công bố — 5 khoảng cố định của API. */
export function GradeDistribution({ rows }: { rows: readonly CourseRow[] }) {
  const buckets = GRADE_BUCKETS.map((_, i) => rows.reduce((s, r) => s + (r.phanBoDiem[i] ?? 0), 0))
  const total = buckets.reduce((s, n) => s + n, 0)
  const max = Math.max(1, ...buckets)
  const dat = rows.reduce((s, r) => s + r.soDat, 0)
  const truot = rows.reduce((s, r) => s + r.soTruot, 0)
  const chua = rows.reduce((s, r) => s + r.chuaCoKetQua, 0)

  return (
    <div className={styles.dist}>
      <dl className={styles.passStats}>
        <div>
          <dt>Tỉ lệ đạt</dt>
          <dd>{dat + truot ? `${Math.round((dat / (dat + truot)) * 1000) / 10}%` : '—'}</dd>
        </div>
        <div>
          <dt>Đạt</dt>
          <dd>{dat}</dd>
        </div>
        <div>
          <dt>Trượt</dt>
          <dd>{truot}</dd>
        </div>
        <div>
          <dt>Chưa có kết quả</dt>
          <dd>{chua}</dd>
        </div>
      </dl>

      {total === 0 ? (
        <p className={styles.empty}>
          Chưa có điểm tổng kết nào được công bố trong học kỳ này — {chua} lượt đang chờ kết quả.
        </p>
      ) : (
        <div className={styles.vbars} role="img" aria-label="Phân bố điểm tổng kết">
          {buckets.map((n, i) => (
            <div key={GRADE_BUCKETS[i]} className={styles.vcol}>
              <span className={styles.vvalue}>{n}</span>
              <span className={styles.vtrack}>
                <span
                  className={`${styles.vfill} ${i === 0 ? styles.vfail : ''}`}
                  style={{ height: `${(n / max) * 100}%`, animationDelay: `${i * 60}ms` }}
                />
              </span>
              <span className={styles.vlabel}>{GRADE_BUCKETS[i]}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/** Tiến độ bảng điểm theo lớp: thanh xếp chồng + chú giải có số. */
export function GradeProgress({ summary }: { summary: ReportSummary }) {
  const { daKhoa, daCongBo, chuaCongBo } = summary.tienDoDiem
  const total = daKhoa + daCongBo + chuaCongBo
  const parts = [
    { key: 'lock', label: 'Đã khoá', value: daKhoa },
    { key: 'pub', label: 'Đã công bố', value: daCongBo },
    { key: 'draft', label: 'Chưa công bố', value: chuaCongBo },
  ] as const

  return (
    <div className={styles.progress}>
      <span className={styles.stack} aria-hidden="true">
        {parts.map((p) =>
          p.value ? (
            <span
              key={p.key}
              className={styles[p.key]}
              style={{ width: `${(p.value / Math.max(1, total)) * 100}%` }}
            />
          ) : null,
        )}
      </span>
      <ul className={styles.legend}>
        {parts.map((p) => (
          <li key={p.key}>
            <span className={`${styles.swatch} ${styles[p.key]}`} />
            {p.label}
            <b>{p.value}</b>
            <small>{total ? `${Math.round((p.value / total) * 100)}%` : ''}</small>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Admin Master, phạm vi toàn hệ thống: so sánh nhanh các cơ sở. */
export function CampusCompare({
  items,
}: {
  items: readonly { tenCoSo: string; summary: ReportSummary }[]
}) {
  return (
    <table className={styles.campus}>
      <thead>
        <tr>
          <th scope="col">Cơ sở</th>
          <th scope="col">Lớp</th>
          <th scope="col">Lượt ĐK</th>
          <th scope="col">Sinh viên</th>
          <th scope="col">Lấp đầy</th>
          <th scope="col">Lớp đầy</th>
        </tr>
      </thead>
      <tbody>
        {items.map(({ tenCoSo, summary: s }) => (
          <tr key={tenCoSo}>
            <th scope="row">{tenCoSo}</th>
            <td>{s.dangKy.soLop}</td>
            <td>{s.dangKy.luotDangKy}</td>
            <td>{s.dangKy.soSinhVien}</td>
            <td>
              <span className={styles.inline}>
                <span className={styles.itrack}>
                  <span style={{ width: `${(s.sucChua.tiLeLapDay ?? 0) * 100}%` }} />
                </span>
                {pct(s.sucChua.tiLeLapDay)}
              </span>
            </td>
            <td>{s.sucChua.soLopDay}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
