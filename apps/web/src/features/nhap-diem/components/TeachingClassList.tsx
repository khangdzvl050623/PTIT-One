import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '@/shared/api'
import { downloadCsv } from '@/shared/lib'
import { ROUTES } from '@/shared/constants'
import { Dialog, Icon, Select, Skeleton, SkeletonRows } from '@/shared/ui'

import * as api from '../api/mockTeachingApi'
import { TERM_NAMES } from '../data/demo'
import { formatLich, vnDateTime } from '../lib/format'
import type { ClassRoster, TeachingClass, TeachingScheduleEntry } from '../types'
import styles from './TeachingClassList.module.scss'

const CLASS_STATUS: Record<string, string> = {
  DU_KIEN: 'Dự kiến',
  MO: 'Đang mở',
  DA_KHOA: 'Đã khoá điểm',
  DA_HUY: 'Đã huỷ',
}

const HINH_THUC: Record<string, string> = {
  TRUC_TIEP: 'Trực tiếp',
  TRUC_TUYEN: 'Trực tuyến',
  KET_HOP: 'Kết hợp',
}

export interface TeachingClassListProps {
  maHocKy: string
  onChangeTerm: (maHocKy: string) => void
  /** Buổi dạy của học kỳ đang xem — dùng để hiện lịch từng lớp. */
  entries: readonly TeachingScheduleEntry[]
}

/**
 * Lớp giảng viên phụ trách (F05) — `GET /api/me/teaching-classes`.
 *
 * Lịch từng lớp lấy từ lịch dạy, vì `teaching-classes` chỉ trả `ClassSection`
 * và **không** kèm lịch. Danh sách sinh viên mở riêng:
 * `GET /api/classes/{maLopHP}/students`, quyền kiểm theo **lớp** nên đổi mã
 * lớp trên URL sang lớp của giảng viên khác là `403`.
 */
export function TeachingClassList({ maHocKy, onChangeTerm, entries }: TeachingClassListProps) {
  const [classes, setClasses] = useState<TeachingClass[] | null>(null)
  const [roster, setRoster] = useState<ClassRoster | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setClasses(null)
    void api.teachingClasses(maHocKy).then(
      (list) => {
        if (!cancelled) setClasses(list)
      },
      () => {
        if (!cancelled) setError('Không tải được danh sách lớp phụ trách.')
      },
    )
    return () => {
      cancelled = true
    }
  }, [maHocKy])

  /** Lịch theo lớp — một lớp có thể nhiều buổi trong tuần. */
  const lichTheoLop = useMemo(() => {
    const map = new Map<string, TeachingScheduleEntry[]>()
    for (const e of entries) {
      const list = map.get(e.maLopHP) ?? []
      list.push(e)
      map.set(e.maLopHP, list)
    }
    return map
  }, [entries])

  async function openRoster(lop: TeachingClass) {
    setBusy(true)
    setError(null)
    try {
      setRoster(await api.classRoster(lop.maLopHP))
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không tải được danh sách sinh viên.')
    } finally {
      setBusy(false)
    }
  }

  /* Giữ nguyên bộ khung lúc chờ: ô chọn học kỳ dùng được ngay, chỗ bảng là
     khung xương — đổi học kỳ không làm cả khối nhảy một nhịp. */
  const dangTai = classes === null
  const tongSinhVien = classes?.reduce((s, c) => s + c.soLuongDaDangKy, 0) ?? 0
  const tongTinChi = classes?.reduce((s, c) => s + c.soTinChi, 0) ?? 0

  return (
    <div className={styles.block}>
      <div className={styles.toolbar} data-print="hide">
        <Select
          ariaLabel="Học kỳ"
          className={styles.termSelect}
          value={maHocKy}
          options={Object.entries(TERM_NAMES).map(([value, label]) => ({ value, label }))}
          onChange={onChangeTerm}
        />
        <span className={styles.summary}>
          {dangTai ? (
            <Skeleton width="260px" height="15px" />
          ) : (
            <>
              {classes.length} lớp · {tongSinhVien} sinh viên · {tongTinChi} tín chỉ giảng dạy
            </>
          )}
        </span>
      </div>

      {error ? (
        <p className={styles.error} role="alert">
          {error}
        </p>
      ) : null}

      <div className={styles.scroll} aria-busy={dangTai}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">Mã lớp</th>
              <th scope="col" className={styles.left}>
                Môn học
              </th>
              <th scope="col" className={styles.left}>
                Lịch dạy
              </th>
              <th scope="col">Sĩ số</th>
              <th scope="col">Hình thức</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {dangTai ? (
              <SkeletonRows cols={7} rows={3} />
            ) : classes.length === 0 ? (
              <tr>
                <td colSpan={7} className={styles.empty}>
                  Học kỳ này bạn chưa được phân công lớp nào.
                </td>
              </tr>
            ) : (
              classes.map((c) => {
                const lich = lichTheoLop.get(c.maLopHP) ?? []
                const khoa = c.trangThai === 'DA_KHOA'
                return (
                  <tr key={c.maLopHP}>
                    <td className={`${styles.center} ${styles.code}`}>{c.maLopHP}</td>
                    <td>
                      {c.tenMonHoc}
                      <small className={styles.sub}>
                        {c.maMonHoc} · {c.soTinChi} TC
                      </small>
                    </td>
                    <td>
                      {lich.length === 0 ? (
                        <span className={styles.muted}>Chưa xếp lịch</span>
                      ) : (
                        lich.map((e) => (
                          <span key={`${e.thu}-${e.tietBatDau}`} className={styles.lich}>
                            {formatLich(e)}
                          </span>
                        ))
                      )}
                    </td>
                    <td className={styles.center}>
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
                    </td>
                    <td className={styles.center}>{HINH_THUC[c.hinhThucHoc] ?? c.hinhThucHoc}</td>
                    <td className={styles.center}>
                      <span className={`${styles.badge} ${styles[`lop_${c.trangThai}`]}`}>
                        {CLASS_STATUS[c.trangThai] ?? c.trangThai}
                      </span>
                    </td>
                    <td className={styles.actions}>
                      <button
                        type="button"
                        className={styles.ghost}
                        onClick={() => void openRoster(c)}
                        disabled={busy}
                      >
                        Danh sách SV
                      </button>
                      {/* Lớp đã khoá vẫn mở được bảng điểm, chỉ là chỉ đọc. */}
                      <Link className={styles.ghost} to={ROUTES.gvNhapDiem}>
                        {khoa ? 'Xem điểm' : 'Nhập điểm'}
                      </Link>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <Dialog
        open={roster !== null}
        onClose={() => setRoster(null)}
        ariaLabel={roster ? `Danh sách sinh viên lớp ${roster.lop.maLopHP}` : 'Danh sách sinh viên'}
        header={roster ? <RosterHeader roster={roster} /> : null}
        footer={
          roster ? (
            <>
              <button
                type="button"
                className={styles.ghost}
                onClick={() =>
                  downloadCsv(
                    [
                      ['STT', 'Mã SV', 'Họ tên', 'Cơ sở nhà', 'Ngày đăng ký', 'Trạng thái'],
                      ...roster.sinhVien.map((sv, i) => [
                        i + 1,
                        sv.maSinhVien,
                        sv.hoTen,
                        sv.maCoSoNha,
                        vnDateTime(sv.ngayDangKy),
                        sv.trangThai,
                      ]),
                    ],
                    `danh-sach-${roster.lop.maLopHP}.csv`,
                  )
                }
              >
                <Icon name="download" size="13px" /> Xuất Excel
              </button>
              <button type="button" className={styles.primary} onClick={() => setRoster(null)}>
                Đóng
              </button>
            </>
          ) : undefined
        }
      >
        {roster ? (
          roster.sinhVien.length === 0 ? (
            <p className={styles.muted}>Lớp chưa có sinh viên nào.</p>
          ) : (
            <table className={styles.rosterTable}>
              <thead>
                <tr>
                  <th scope="col">STT</th>
                  <th scope="col">Mã SV</th>
                  <th scope="col">Họ tên</th>
                  <th scope="col">Cơ sở nhà</th>
                  <th scope="col">Ngày đăng ký</th>
                </tr>
              </thead>
              <tbody>
                {roster.sinhVien.map((sv, i) => (
                  <tr key={sv.maSinhVien}>
                    <td>{i + 1}</td>
                    <td className={styles.code}>{sv.maSinhVien}</td>
                    <td>{sv.hoTen}</td>
                    <td>{sv.maCoSoNha}</td>
                    <td>{vnDateTime(sv.ngayDangKy)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        ) : null}
      </Dialog>
    </div>
  )
}

/** Đầu popup: thông tin lớp + đối soát bộ đếm sĩ số với số dòng ghi danh. */
function RosterHeader({ roster }: { roster: ClassRoster }) {
  const boDem = roster.lop.soLuongDaDangKy
  const soDong = roster.sinhVien.length
  return (
    <div className={styles.dialogTitle}>
      {roster.lop.tenMonHoc}
      <small>
        {roster.lop.maLopHP} · {roster.lop.soTinChi} TC
      </small>
      <span className={boDem === soDong ? styles.match : styles.mismatch}>
        Bộ đếm sĩ số {boDem} · Dòng ghi danh {soDong} —{' '}
        {boDem === soDong ? 'khớp ✓' : 'LỆCH, cần kiểm tra'}
      </span>
    </div>
  )
}
