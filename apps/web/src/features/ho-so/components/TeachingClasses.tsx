import { Link } from 'react-router-dom'

import type { TeachingClass } from '../types'
import styles from './StaffCards.module.scss'

const TRANG_THAI_DIEM: Record<TeachingClass['trangThaiDiem'], string> = {
  NHAP: 'Chưa công bố',
  DA_CONG_BO: 'Đã công bố',
  DA_KHOA: 'Đã khoá',
}

export interface TeachingClassesProps {
  title: string
  classes: readonly TeachingClass[]
  /** Đích "Nhập điểm" cho từng lớp. */
  gradeHref: string
}

/** Lớp GV phụ trách: sĩ số, lịch và trạng thái bảng điểm. */
export function TeachingClasses({ title, classes, gradeHref }: TeachingClassesProps) {
  return (
    <section className={styles.card}>
      <header className={styles.head}>
        <h3>{title}</h3>
        <span className={styles.count}>{classes.length} lớp</span>
      </header>
      {classes.length === 0 ? (
        <p className={styles.empty}>Chưa được phân công lớp nào.</p>
      ) : (
        <ul className={styles.classes}>
          {classes.map((c) => (
            <li key={c.maLopHP}>
              <div className={styles.classTop}>
                <span>
                  <b>{c.tenMonHoc}</b>
                  <small title={c.maLopHP}>
                    {c.maLopHP} · {c.soTinChi} TC
                  </small>
                </span>
                <span className={`${styles.badge} ${styles[c.trangThaiDiem]}`}>
                  {TRANG_THAI_DIEM[c.trangThaiDiem]}
                </span>
              </div>
              <div className={styles.classMeta}>
                <span>{c.lich}</span>
                <span className={styles.seats}>
                  <span
                    className={styles.meter}
                    role="progressbar"
                    aria-label={`Sĩ số ${c.maLopHP}`}
                    aria-valuemin={0}
                    aria-valuemax={c.soLuongToiDa}
                    aria-valuenow={c.soLuongDaDangKy}
                  >
                    <span style={{ width: `${(c.soLuongDaDangKy / c.soLuongToiDa) * 100}%` }} />
                  </span>
                  {c.soLuongDaDangKy}/{c.soLuongToiDa}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
      <footer className={styles.foot}>
        <Link to={gradeHref}>Nhập điểm</Link>
      </footer>
    </section>
  )
}
