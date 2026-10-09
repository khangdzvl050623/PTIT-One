import { Link } from 'react-router-dom'

import type { TeachingClass } from '../types'
import styles from './StaffCards.module.scss'

const TRANG_THAI_LOP: Record<string, string> = {
  DU_KIEN: 'Dự kiến',
  MO: 'Đang mở',
  DA_KHOA: 'Đã khoá',
  DA_HUY: 'Đã huỷ',
}

const HINH_THUC: Record<string, string> = {
  TRUC_TIEP: 'Trực tiếp',
  TRUC_TUYEN: 'Trực tuyến',
  KET_HOP: 'Kết hợp',
}

export interface TeachingClassesProps {
  title: string
  classes: readonly TeachingClass[]
  /** Đích "Nhập điểm" cho từng lớp. */
  gradeHref: string
}

/**
 * Lớp GV phụ trách: sĩ số, hình thức học và trạng thái lớp.
 *
 * Hiện **trạng thái lớp**, không phải trạng thái bảng điểm:
 * `GET /api/me/teaching-classes` trả `ClassSection`, không kèm bảng điểm cũng
 * không kèm lịch. Muốn hai thứ đó thì phải gọi thêm `/grades` và
 * `/teaching-schedule` cho từng lớp — màn "Lớp phụ trách" làm việc đó, còn ô
 * tóm tắt này thì không đáng.
 */
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
                <span className={`${styles.badge} ${styles[c.trangThai]}`}>
                  {TRANG_THAI_LOP[c.trangThai] ?? c.trangThai}
                </span>
              </div>
              <div className={styles.classMeta}>
                <span>{HINH_THUC[c.hinhThucHoc] ?? c.hinhThucHoc}</span>
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
