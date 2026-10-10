import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { useAsyncData } from '@/shared/lib'
import { Panel } from '@/shared/ui'

import { fetchAuthored, fetchAuthoredDetail } from '../api/authoredApi'
import { daGui, tongNguoiNhan } from '../api/authoredTypes'
import type { AuthoredNotice } from '../api/authoredTypes'

import { formatNgay, NoticeDetailDialog } from './NoticeDetailDialog'
import dialogStyles from './NoticeDetailDialog.module.scss'
import { Pills } from './Pills'

import styles from './SentBox.module.scss'

type Filter = 'DA_GUI' | 'NHAP'

/**
 * Hộp thư gửi — các bản do chính mình soạn. Đặt cạnh chỗ soạn để gửi xong
 * kiểm tra ngay được (ai đọc, gửi lúc nào) thay vì soạn xong là mù.
 *
 * Nháp và đã gửi nằm chung, phân biệt bằng badge và pill lọc; sửa/xoá/gửi
 * nháp không thuộc màn này.
 */
export function SentBox() {
  const load = useCallback(() => fetchAuthored(), [])
  const { data, loading, error } = useAsyncData(load)
  const [filter, setFilter] = useState<Filter>('DA_GUI')
  const [selected, setSelected] = useState<string | null>(null)

  const items = data ?? []
  const sent = items.filter((n) => daGui(n))
  const drafts = items.filter((n) => !daGui(n))
  const visible = filter === 'DA_GUI' ? sent : drafts

  return (
    <Panel title="HỘP THƯ GỬI" icon="bullhorn">
      <div className={styles.pillWrap}>
        <Pills
          options={[
            { value: 'DA_GUI' as const, label: 'Đã gửi', count: sent.length },
            { value: 'NHAP' as const, label: 'Nháp', count: drafts.length },
          ]}
          value={filter}
          onChange={setFilter}
          ariaLabel="Lọc theo trạng thái"
        />
      </div>

      {loading ? <p className={styles.state}>Đang tải hộp thư gửi…</p> : null}
      {error ? (
        <p className={styles.state} role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error && visible.length === 0 ? (
        <p className={styles.state}>
          {items.length === 0 ? 'Chưa soạn thông báo nào.' : 'Không có bản nào ở trạng thái này.'}
        </p>
      ) : null}

      {visible.length > 0 ? (
        <ul className={styles.list}>
          {visible.map((n) => (
            <li key={n.maThongBao} className={styles.row}>
              <button type="button" className={styles.main} onClick={() => setSelected(n.maThongBao)}>
                <span className={styles.titleRow}>
                  <span className={styles.title}>{n.tieuDe}</span>
                  {n.mucDo === 'QUAN_TRONG' ? <span className={styles.urgent}>Quan trọng</span> : null}
                </span>
                <span className={styles.sub}>
                  {phamVi(n)} · {ngay(n.ngayGui ?? n.ngayTao)}
                </span>
              </button>
              <span className={styles.meta}>
                {daGui(n) ? `${n.soDaDoc}/${tongNguoiNhan(n)} đã đọc` : `dự kiến ${tongNguoiNhan(n)}`}
              </span>
            </li>
          ))}
        </ul>
      ) : null}

      {selected ? <SentDialog maThongBao={selected} onClose={() => setSelected(null)} /> : null}
    </Panel>
  )
}

/** Dialog chi tiết bản đã soạn: nội dung + phạm vi + số đã đọc. Không phản hồi. */
function SentDialog({ maThongBao, onClose }: { maThongBao: string; onClose: () => void }) {
  const [detail, setDetail] = useState<AuthoredNotice | null>(null)
  const [pending, setPending] = useState(true)
  const [failed, setFailed] = useState<string | null>(null)

  /* Nạp lại khi mở để số người nhận/đã đọc mới nhất (nháp tính lại mỗi lần). */
  useEffect(() => {
    let cancelled = false
    setPending(true)
    setFailed(null)
    void fetchAuthoredDetail(maThongBao).then(
      (found) => {
        if (!cancelled) {
          setDetail(found)
          setPending(false)
        }
      },
      () => {
        if (!cancelled) {
          setFailed('Không tải được chi tiết. Đóng và thử lại.')
          setPending(false)
        }
      },
    )
    return () => {
      cancelled = true
    }
  }, [maThongBao])

  const n = detail

  return (
    <NoticeDetailDialog
      title="NỘI DUNG THÔNG BÁO"
      onClose={onClose}
      status={
        <>
          {pending ? <p className={styles.state}>Đang tải chi tiết…</p> : null}
          {failed ? (
            <p className={styles.state} role="alert">
              {failed}
            </p>
          ) : null}
        </>
      }
      rows={[
        ...(pending || failed || !n
          ? []
          : [
              {
                label: daGui(n) ? 'Ngày gửi' : 'Tạo lúc',
                content: n.ngayGui ? formatNgay(n.ngayGui) : formatNgay(n.ngayTao),
              },
              {
                label: 'Tiêu đề',
                content: <span className={dialogStyles.strong}>{n.tieuDe}</span>,
              },
              {
                label: 'Nội dung',
                content: <span className={dialogStyles.body}>{n.noiDung}</span>,
              },
              {
                label: 'Phạm vi',
                content: (
                  <span>
                    {phamVi(n)} → {doiTuong(n.doiTuong)}
                  </span>
                ),
              },
              {
                label: 'Người nhận',
                content: (
                  <span>
                    {n.nguoiNhan.soSinhVien} sinh viên · {n.nguoiNhan.soGiangVien} giảng viên
                    {daGui(n) ? ` — ${n.soDaDoc} đã đọc` : ' (dự kiến, chốt lúc gửi)'}
                  </span>
                ),
              },
              ...(n.lienKet
                ? [
                    {
                      label: 'Liên kết',
                      content: (
                        <Link className={dialogStyles.innerLink} to={n.lienKet}>
                          Mở trang liên quan
                        </Link>
                      ),
                    },
                  ]
                : []),
            ]),
      ]}
    />
  )
}

/** Ngày hiển thị kiểu cổng gốc: dd/mm/yyyy. */
function ngay(iso: string): string {
  const time = Date.parse(iso)
  return Number.isNaN(time) ? '—' : new Date(time).toLocaleDateString('vi-VN')
}

function phamVi(n: AuthoredNotice): string {
  if (n.phamVi === 'TOAN_TRUONG') return 'Toàn trường'
  if (n.phamVi === 'CO_SO') return `Cơ sở ${n.maCoSo ?? ''}`.trim()
  if (n.phamVi === 'LOP_HOC_PHAN') return `Lớp ${n.maLopHP ?? ''}`.trim()
  return n.phamVi
}

function doiTuong(value: string): string {
  if (value === 'SINH_VIEN') return 'Sinh viên'
  if (value === 'GIANG_VIEN') return 'Giảng viên'
  if (value === 'TAT_CA') return 'Tất cả'
  return value
}
