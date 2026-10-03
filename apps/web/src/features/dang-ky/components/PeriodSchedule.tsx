import { useEffect, useMemo, useState } from 'react'

import { Select } from '@/shared/ui'

import * as api from '../api/mockEnrollmentApi'
import { TRAN_TIN_CHI } from '../data/demo'
import { PHASE_LABEL, phaseOf, vnDateTime } from '../lib/period'
import type { PeriodPhase } from '../lib/period'
import type { EnrollmentPeriod } from '../types'
import styles from './PeriodSchedule.module.scss'

/** Trạng thái tính theo giờ thực — làm mới mỗi 30 giây để đợt tự chuyển Đang mở/Hết giờ. */
const TICK_MS = 30_000

export interface PeriodScheduleProps {
  /** Tên học kỳ theo mã — từ `GET /api/terms`. */
  termNames: Readonly<Record<string, string>>
}

/** Lịch đợt đăng ký của cơ sở sinh viên: bảng mọi đợt + lưu ý khi đăng ký. */
export function PeriodSchedule({ termNames }: PeriodScheduleProps) {
  const [periods, setPeriods] = useState<EnrollmentPeriod[] | null>(null)
  const [failed, setFailed] = useState(false)
  const [maHocKy, setMaHocKy] = useState('')
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    let cancelled = false
    api.listPeriods().then(
      (list) => !cancelled && setPeriods(list),
      () => !cancelled && setFailed(true),
    )
    const timer = window.setInterval(() => setNow(Date.now()), TICK_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [])

  const rows = useMemo(
    () => (periods ?? []).filter((p) => !maHocKy || p.maHocKy === maHocKy),
    [periods, maHocKy],
  )

  if (failed) return <p className={styles.muted}>Không tải được lịch đợt đăng ký. Vui lòng thử lại.</p>
  if (!periods) return <p className={styles.muted}>Đang tải lịch đợt đăng ký…</p>

  const termOf = (p: EnrollmentPeriod) => termNames[p.maHocKy] ?? p.maHocKy

  return (
    <div className={styles.wrap}>
      <section className={styles.section}>
        <header className={styles.sectionHead}>
          <h3>Danh sách đợt đăng ký</h3>
          <Select
            ariaLabel="Lọc theo học kỳ"
            className={styles.termSelect}
            value={maHocKy}
            options={[
              { value: '', label: 'Tất cả học kỳ' },
              ...[...new Set(periods.map((p) => p.maHocKy))].map((ma) => ({
                value: ma,
                label: termNames[ma] ?? ma,
              })),
            ]}
            onChange={setMaHocKy}
          />
        </header>

        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">STT</th>
                <th scope="col">Mã đợt</th>
                <th scope="col" className={styles.left}>
                  Học kỳ
                </th>
                <th scope="col">Cơ sở</th>
                <th scope="col">Thời gian mở</th>
                <th scope="col">Thời gian đóng</th>
                <th scope="col">Thời lượng</th>
                <th scope="col">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={8} className={styles.empty}>
                    Không có đợt đăng ký nào.
                  </td>
                </tr>
              ) : (
                rows.map((p, i) => {
                  const phase = phaseOf(p, now)
                  return (
                    <tr
                      key={p.maDot}
                      className={phase === 'DANG_MO' ? styles.rowOpen : undefined}
                    >
                      <td className={styles.center}>{i + 1}</td>
                      <td className={`${styles.center} ${styles.code}`}>{p.maDot}</td>
                      <td>{termOf(p)}</td>
                      <td className={styles.center}>{p.maCoSo}</td>
                      <td className={styles.center}>{vnDateTime(p.thoiGianMo)}</td>
                      <td className={styles.center}>{vnDateTime(p.thoiGianDong)}</td>
                      <td className={styles.center}>
                        {Math.round(
                          (Date.parse(p.thoiGianDong) - Date.parse(p.thoiGianMo)) / 86_400_000,
                        )}{' '}
                        ngày
                      </td>
                      <td className={styles.center}>
                        <PhaseBadge phase={phase} />
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className={styles.section}>
        <header className={styles.sectionHead}>
          <h3>Lưu ý khi đăng ký học phần</h3>
        </header>
        <ul className={styles.rules}>
          <li>
            Chỉ đăng ký và huỷ được <b>trong thời gian đợt đang mở</b>; hết giờ đóng thì hệ
            thống tự khoá dù đợt chưa được đóng thủ công.
          </li>
          <li>
            Tổng tín chỉ mỗi học kỳ không vượt <b>{TRAN_TIN_CHI} tín chỉ</b>.
          </li>
          <li>
            Phải <b>đạt mọi môn tiên quyết</b> (chỉ tính điểm đã công bố) và môn phải thuộc{' '}
            <b>chương trình đào tạo</b> của bạn.
          </li>
          <li>
            Mỗi môn chỉ giữ <b>một lớp</b> trong học kỳ; lớp mới <b>không được trùng lịch</b> với
            lớp đã đăng ký.
          </li>
          <li>
            Lớp đủ sĩ số thì từ chối ngay — <b>không có hàng chờ</b>. Huỷ lớp là trả chỗ, người
            khác có thể đăng ký mất.
          </li>
          <li>
            Chỉ huỷ được khi <b>chưa có điểm</b>. Đăng ký lại môn đã học được tính là học lại
            hoặc cải thiện; <b>điểm cao nhất</b> được giữ.
          </li>
          <li>Mọi thời gian trên trang tính theo giờ Việt Nam (UTC+7).</li>
        </ul>
      </section>
    </div>
  )
}

function PhaseBadge({ phase }: { phase: PeriodPhase }) {
  return <span className={`${styles.badge} ${styles[phase]}`}>{PHASE_LABEL[phase]}</span>
}
