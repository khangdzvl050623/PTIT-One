import { useCallback, useState } from 'react'

import { useTerms } from '@/features/lich-hoc'
import { teachingClasses } from '@/features/nhap-diem'
import type { TeachingClass } from '@/features/nhap-diem'
import { useAsyncData } from '@/shared/lib'
import { Panel, Select } from '@/shared/ui'

import { NoticeComposer } from './NoticeComposer'
import { Inbox } from './Inbox'
import { Pills } from './Pills'
import { SentBox } from './SentBox'

import styles from './GvNoticeHub.module.scss'

type Tab = 'DEN' | 'GUI' | 'DA_GUI'

/**
 * Chỗ thông báo duy nhất của giảng viên: "Hộp thư đến" (tin admin và hệ thống
 * gửi mình), "Gửi thông báo" (soạn cho từng lớp mình dạy) và "Đã gửi" (bản
 * mình đã soạn, kèm số đã đọc).
 *
 * Không tách ba trang riêng — đọc tin lớp xong soạn tiếp, soạn xong kiểm tra
 * ngay là luồng liền mạch, và TÍNH NĂNG chỉ cần một link "Thông báo".
 */
export function GvNoticeHub() {
  const [tab, setTab] = useState<Tab>('DEN')

  return (
    <div className={styles.hub}>
      <Pills
        options={[
          { value: 'DEN' as const, label: 'Hộp thư đến' },
          { value: 'GUI' as const, label: 'Gửi thông báo' },
          { value: 'DA_GUI' as const, label: 'Đã gửi' },
        ]}
        value={tab}
        onChange={setTab}
        ariaLabel="Thông báo giảng viên"
      />

      {tab === 'DEN' ? <Inbox /> : tab === 'GUI' ? <GuiThongBao /> : <SentBox />}
    </div>
  )
}

/** Nhánh gửi: chọn 1 trong các lớp đang dạy rồi soạn ngay dưới. */
function GuiThongBao() {
  /* Học kỳ mặc định của server — tin gửi cho lớp của học kỳ hiện tại. */
  const { defaultTerm, loading: loadingKy, error: errorKy } = useTerms()

  const loadClasses = useCallback(
    () => (defaultTerm ? teachingClasses(defaultTerm) : Promise.resolve([] as TeachingClass[])),
    [defaultTerm],
  )
  const classes = useAsyncData(loadClasses)
  const list = classes.data ?? []
  const [maLopHP, setMaLopHP] = useState('')

  const lop = list.find((c) => c.maLopHP === maLopHP) ?? null

  if (loadingKy || classes.loading) {
    return (
      <Panel title="CHỌN LỚP NHẬN" icon="users">
        <p>Đang tải lớp phụ trách…</p>
      </Panel>
    )
  }

  if (errorKy || classes.error) {
    return (
      <Panel title="CHỌN LỚP NHẬN" icon="users">
        <p role="alert">{errorKy ?? classes.error}</p>
      </Panel>
    )
  }

  if (list.length === 0) {
    return (
      <Panel title="CHỌN LỚP NHẬN" icon="users">
        <p>Học kỳ này chưa có lớp nào để gửi thông báo.</p>
      </Panel>
    )
  }

  return (
    <div className={styles.send}>
      <Panel title="CHỌN LỚP NHẬN" icon="users">
        <label className={styles.field}>
          Lớp nhận thông báo
          {/* Mục rỗng đầu để Select không tự trỏ lớp đầu tiên khi chưa chọn:
              `Select` hiện mục khớp `value`, không khớp thì lấy mục 0. */}
          <Select
            value={maLopHP}
            options={[
              { value: '', label: 'CHỌN LỚP ĐỂ GỬI THÔNG BÁO' },
              ...list.map((c) => ({
                value: c.maLopHP,
                label: `${c.maLopHP} — ${c.tenMonHoc} (${c.soLuongDaDangKy} SV)`,
              })),
            ]}
            onChange={setMaLopHP}
            ariaLabel="Lớp nhận thông báo"
          />
        </label>
        {lop ? (
          <p className={styles.hint}>
            Tin gửi tới <b>sinh viên lớp {lop.maLopHP}</b>.
          </p>
        ) : (
          <p className={styles.hint}>Chọn lớp ở trên để bắt đầu.</p>
        )}
      </Panel>

      {lop ? (
        <NoticeComposer
          key={lop.maLopHP}
          target={{ phamVi: 'LOP_HOC_PHAN', maCoSo: null, maLopHP: lop.maLopHP }}
          tenNguoiNhan={`lớp ${lop.tenMonHoc} (${lop.maLopHP})`}
        />
      ) : null}
    </div>
  )
}
