import { useState } from 'react'
import type { CSSProperties } from 'react'

import { Select } from '@/shared/ui'

import type { TermResults } from '../types'
import styles from './GradeChart.module.scss'

const MAX_SCORE = 10
const TICKS = [10, 8, 6, 4, 2, 0] as const
/** Mốc đạt của thang 10 — trùng `GradePolicy` ở backend. */
const PASS_SCORE = 4

export interface GradeChartProps {
  /** Mới nhất trước; mặc định chọn phần tử đầu. */
  terms: readonly TermResults[]
}

/**
 * Điểm tổng kết từng môn của một học kỳ. Một chuỗi nên một màu, không cần
 * chú giải. Môn chưa công bố điểm vẽ khung nét đứt — **không** vẽ cột 0.
 */
export function GradeChart({ terms }: GradeChartProps) {
  const [maHocKy, setMaHocKy] = useState(terms[0]?.maHocKy ?? '')
  const term = terms.find((t) => t.maHocKy === maHocKy) ?? terms[0]

  return (
    <section className={styles.card}>
      <header className={styles.header}>
        <h3 className={styles.title}>Kết quả học tập</h3>
        <Select
          ariaLabel="Học kỳ của biểu đồ kết quả học tập"
          className={styles.select}
          value={term?.maHocKy ?? ''}
          options={terms.map((t) => ({ value: t.maHocKy, label: t.tenHocKy }))}
          onChange={setMaHocKy}
        />
      </header>

      {!term || term.monHoc.length === 0 ? (
        <p className={styles.empty}>Chưa có môn học nào trong học kỳ này.</p>
      ) : (
        <>
          <div className={styles.plot} aria-hidden="true">
            <div className={styles.axis}>
              {TICKS.map((tick) => (
                <span key={tick}>{tick}</span>
              ))}
            </div>

            <div className={styles.area}>
              {TICKS.map((tick) => (
                <span
                  key={tick}
                  className={styles.grid}
                  style={{ bottom: `${(tick / MAX_SCORE) * 100}%` }}
                />
              ))}
              <span
                className={styles.passLine}
                style={{ bottom: `${(PASS_SCORE / MAX_SCORE) * 100}%` }}
              />

              {/* key theo học kỳ: đổi kỳ thì cột dựng lại và chạy lại hiệu ứng mọc. */}
              <div key={term.maHocKy} className={styles.bars}>
                {term.monHoc.map((mon, index) => (
                  <div
                    key={mon.maMonHoc}
                    className={styles.slot}
                    style={
                      {
                        '--bar-top': `${barTop(mon.diemTongKet)}%`,
                        '--i': index,
                      } as CSSProperties
                    }
                  >
                    {mon.diemTongKet === null ? (
                      <span className={styles.pending} />
                    ) : (
                      <span
                        className={styles.bar}
                        style={{ height: `${(mon.diemTongKet / MAX_SCORE) * 100}%` }}
                      />
                    )}
                    <span className={styles.tooltip}>
                      <strong>{mon.tenMonHoc}</strong>
                      <span>
                        {mon.diemTongKet === null
                          ? 'Chưa có điểm'
                          : `${mon.diemTongKet.toFixed(1)} · ${mon.diemTongKet >= PASS_SCORE ? 'Đạt' : 'Không đạt'}`}
                      </span>
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className={styles.labels}>
              {term.monHoc.map((mon) => (
                <span key={mon.maMonHoc}>{mon.maMonHoc}</span>
              ))}
            </div>
          </div>

          <p className={styles.note}>
            <span className={styles.noteLine} /> Mốc đạt {PASS_SCORE.toFixed(1)}
            <span className={styles.notePending} /> Chưa có điểm
          </p>

          {/* Bản bảng cho trình đọc màn hình — biểu đồ ở trên là aria-hidden. */}
          <table className={styles.srOnly}>
            <caption>Điểm tổng kết {term.tenHocKy}</caption>
            <thead>
              <tr>
                <th scope="col">Môn học</th>
                <th scope="col">Điểm</th>
              </tr>
            </thead>
            <tbody>
              {term.monHoc.map((mon) => (
                <tr key={mon.maMonHoc}>
                  <th scope="row">
                    {mon.maMonHoc} {mon.tenMonHoc}
                  </th>
                  <td>{mon.diemTongKet === null ? 'Chưa có điểm' : mon.diemTongKet.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}

/** Đỉnh cột theo % chiều cao — tooltip neo ở đây. Chưa có điểm thì neo giữa khung. */
function barTop(diem: number | null): number {
  return diem === null ? 50 : (diem / MAX_SCORE) * 100
}
