import { useCallback, useEffect, useMemo, useState } from 'react'

import { ApiError } from '@/shared/api'
import { downloadCsv } from '@/shared/lib'
import { Dialog, Icon } from '@/shared/ui'

import * as api from '../api/mockEnrollmentApi'
import { DEMO_MA_HOC_KY } from '../data/demo'
import { vnDateTime } from '../lib/period'
import type { ClassOffer, ClassRoster } from '../types'
import styles from './Admin.module.scss'

const CLASS_STATUS: Record<string, string> = {
  DU_KIEN: 'Dự kiến',
  MO: 'Đang mở',
  DA_KHOA: 'Đã khoá',
  DA_HUY: 'Đã huỷ',
}

function fold(s: string): string {
  return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/gi, 'd').toLowerCase()
}

/**
 * Ghi danh theo lớp của cơ sở: xem danh sách sinh viên (đối soát bộ đếm với
 * số dòng ghi danh) và huỷ lớp. Huỷ lớp là MỘT giao dịch ở server: huỷ ghi
 * danh, trả tín chỉ, sĩ số về 0, báo sinh viên và giảng viên.
 */
export function ClassEnrollments() {
  const [classes, setClasses] = useState<ClassOffer[] | null>(null)
  const [query, setQuery] = useState('')
  const [roster, setRoster] = useState<ClassRoster | null>(null)
  const [cancelling, setCancelling] = useState<ClassOffer | null>(null)
  const [lyDo, setLyDo] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)

  const reload = useCallback(async () => setClasses(await api.adminClasses(DEMO_MA_HOC_KY)), [])

  useEffect(() => {
    void reload()
  }, [reload])

  const visible = useMemo(() => {
    const q = fold(query.trim())
    return (classes ?? []).filter(
      (c) => !q || fold(`${c.maLopHP} ${c.tenMonHoc} ${c.tenGiangVien ?? ''}`).includes(q),
    )
  }, [classes, query])

  async function openRoster(lop: ClassOffer) {
    setBusy(true)
    try {
      setRoster(await api.classRoster(lop.maLopHP))
    } finally {
      setBusy(false)
    }
  }

  async function confirmCancel() {
    if (!cancelling) return
    setBusy(true)
    try {
      const result = await api.cancelClass(cancelling.maLopHP)
      setNotice({
        tone: 'success',
        text:
          result.soDangKyDaHuy > 0
            ? `Đã huỷ lớp ${cancelling.maLopHP} — ${result.soDangKyDaHuy} sinh viên được trả chỗ và tín chỉ, đã gửi thông báo${lyDo.trim() ? ' kèm lý do' : ''}.`
            : `Lớp ${cancelling.maLopHP} đã huỷ từ trước.`,
      })
      setCancelling(null)
      setLyDo('')
      await reload()
    } catch (cause) {
      setNotice({ tone: 'error', text: cause instanceof ApiError ? cause.message : 'Không huỷ được lớp.' })
      setCancelling(null)
    } finally {
      setBusy(false)
    }
  }

  if (!classes) return <p className={styles.muted}>Đang tải danh sách lớp…</p>

  const tongDaDangKy = classes.reduce((s, c) => s + c.soLuongDaDangKy, 0)
  const tongSucChua = classes.filter((c) => c.trangThai !== 'DA_HUY').reduce((s, c) => s + c.soLuongToiDa, 0)

  return (
    <div className={styles.block}>
      <div className={styles.toolbar}>
        <label className={styles.search}>
          <Icon name="search" size="14px" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo mã lớp, môn hoặc giảng viên…"
            aria-label="Tìm lớp"
          />
        </label>
        <span className={styles.summary}>
          {classes.length} lớp · {tongDaDangKy}/{tongSucChua} chỗ đã đăng ký
        </span>
      </div>

      {notice ? (
        <p className={`${styles.notice} ${styles[notice.tone]}`} role={notice.tone === 'error' ? 'alert' : 'status'}>
          {notice.text}
          <button type="button" onClick={() => setNotice(null)} aria-label="Đóng thông báo">
            ×
          </button>
        </p>
      ) : null}

      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">Mã lớp</th>
              <th scope="col" className={styles.left}>
                Môn học
              </th>
              <th scope="col" className={styles.left}>
                Giảng viên
              </th>
              <th scope="col">Sĩ số</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 ? (
              <tr>
                <td colSpan={6} className={styles.empty}>
                  Không tìm thấy dữ liệu
                </td>
              </tr>
            ) : (
              visible.map((c) => {
                const huy = c.trangThai === 'DA_HUY'
                return (
                  <tr key={c.maLopHP} className={huy ? styles.rowCancelled : undefined}>
                    <td className={`${styles.center} ${styles.code}`}>{c.maLopHP}</td>
                    <td>
                      {c.tenMonHoc}
                      <small className={styles.sub}>
                        {c.maMonHoc} · {c.soTinChi} TC
                      </small>
                    </td>
                    <td>{c.tenGiangVien ?? 'Chưa phân công'}</td>
                    <td className={styles.center}>
                      <span className={styles.seats}>
                        <span className={styles.meter}>
                          <span style={{ width: `${(c.soLuongDaDangKy / c.soLuongToiDa) * 100}%` }} />
                        </span>
                        {c.soLuongDaDangKy}/{c.soLuongToiDa}
                      </span>
                    </td>
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
                      <button
                        type="button"
                        className={styles.danger}
                        onClick={() => setCancelling(c)}
                        disabled={busy || huy}
                      >
                        Huỷ lớp
                      </button>
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
                        vnDateTime(sv.ngayDangKy, true),
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
                    <td>{vnDateTime(sv.ngayDangKy, true)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        ) : null}
      </Dialog>

      <Dialog
        open={cancelling !== null}
        onClose={() => setCancelling(null)}
        ariaLabel={cancelling ? `Huỷ lớp ${cancelling.maLopHP}` : 'Huỷ lớp'}
        header={
          <p className={styles.dialogTitle}>
            Huỷ lớp {cancelling?.maLopHP}
            <small>{cancelling?.tenMonHoc}</small>
          </p>
        }
        footer={
          <>
            <button type="button" className={styles.ghost} onClick={() => setCancelling(null)} disabled={busy}>
              Giữ lớp
            </button>
            <button type="button" className={styles.dangerSolid} onClick={() => void confirmCancel()} disabled={busy}>
              {busy ? 'Đang huỷ…' : 'Huỷ lớp'}
            </button>
          </>
        }
      >
        {cancelling ? (
          <div className={styles.form}>
            <p className={styles.warn}>
              <b>{cancelling.soLuongDaDangKy} sinh viên</b> sẽ bị huỷ ghi danh, được trả chỗ và trả tín
              chỉ; sinh viên và giảng viên nhận thông báo. Không huỷ được nếu lớp đã khoá điểm hoặc đã
              có điểm. Thao tác này không hoàn tác được.
            </p>
            <label>
              <span>Lý do (gửi kèm thông báo, tuỳ chọn)</span>
              <textarea
                value={lyDo}
                maxLength={500}
                rows={3}
                onChange={(e) => setLyDo(e.target.value)}
                placeholder="Ví dụ: Không đủ sĩ số tối thiểu để mở lớp."
              />
              <small className={styles.sub}>{lyDo.length}/500</small>
            </label>
          </div>
        ) : null}
      </Dialog>
    </div>
  )
}

/** Đầu popup danh sách: thông tin lớp + đối soát bộ đếm với số dòng ghi danh. */
function RosterHeader({ roster }: { roster: ClassRoster }) {
  const boDem = roster.lop.soLuongDaDangKy
  const soDong = roster.sinhVien.length
  return (
    <div className={styles.dialogTitle}>
      {roster.lop.tenMonHoc}
      <small>
        {roster.lop.maLopHP} · {roster.lop.tenGiangVien ?? 'Chưa phân công'}
      </small>
      <span className={boDem === soDong ? styles.match : styles.mismatch}>
        Bộ đếm sĩ số {boDem} · Dòng ghi danh {soDong} — {boDem === soDong ? 'khớp ✓' : 'LỆCH, cần kiểm tra'}
      </span>
    </div>
  )
}
