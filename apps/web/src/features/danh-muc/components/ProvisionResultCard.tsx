import { useState } from 'react'

import { Dialog, Icon } from '@/shared/ui'

import type { ProvisionResult } from '../types'
import styles from './AccountDirectory.module.scss'

export interface ProvisionResultCardProps {
  result: ProvisionResult
  title: string
  onClose: () => void
}

/**
 * Kết quả cấp hồ sơ. Ba nhánh khác hẳn nhau:
 *
 * - **Mã qua email**: `maKichHoat = null`. Admin không thấy mã — đó chính là
 *   điều khiến việc kích hoạt chứng minh người dùng sở hữu hòm thư, nên ở đây
 *   không có gì để sao chép.
 * - **Mã trao tay**: `maKichHoat` có giá trị và đây là **lần duy nhất** mã gốc
 *   rời khỏi server; DB chỉ giữ hash nên đóng hộp thoại là mất.
 * - **Mật khẩu đặt sẵn** (⚠️ chỉ demo): không có mã kích hoạt, Admin trao thẳng
 *   mật khẩu. Nhánh này không có ở API thật — xem `DemoInitialPassword`.
 */
export function ProvisionResultCard({ result, title, onClose }: ProvisionResultCardProps) {
  const [copied, setCopied] = useState(false)
  const { kichHoat, matKhauBanDau } = result
  /** Thứ Admin phải trao tay: mật khẩu đặt sẵn, hoặc mã kích hoạt. */
  const canChep = matKhauBanDau ?? kichHoat?.maKichHoat ?? null

  async function copy() {
    if (!canChep) return
    try {
      await navigator.clipboard.writeText(canChep)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      /* Trình duyệt chặn clipboard (không phải HTTPS, hoặc người dùng từ chối)
         — giá trị vẫn đang hiện trên màn hình để chép tay. */
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      ariaLabel={title}
      header={<p className={styles.dialogTitle}>{title}</p>}
      footer={
        <>
          {canChep ? (
            <button type="button" className={styles.ghost} onClick={() => void copy()}>
              <Icon name={copied ? 'check' : 'download'} size="13px" />
              {copied ? 'Đã chép' : matKhauBanDau ? 'Chép mật khẩu' : 'Chép mã'}
            </button>
          ) : null}
          <button type="button" className={styles.primary} onClick={onClose}>
            {canChep ? 'Tôi đã lưu' : 'Đóng'}
          </button>
        </>
      }
    >
      <div className={styles.form}>
        {matKhauBanDau ? (
          <>
            <p>
              Tài khoản <b>{result.ma}</b> ({result.hoTen}) đã có mật khẩu, đăng nhập được ngay.
              Không có mã kích hoạt cho tài khoản này.
            </p>
            <p className={styles.codeBox}>
              <code>{matKhauBanDau}</code>
            </p>
            <p className={styles.warn}>
              Trao mật khẩu tận tay và <b>nhắc đổi ngay sau lần đăng nhập đầu</b>. Ai biết mật khẩu
              mặc định cũng vào được tài khoản vừa cấp mà chưa ai dùng tới.
            </p>
            {/* Ghi ngay trên màn để người demo không tưởng đây là luồng thật. */}
            <p className={styles.hintBox}>
              Đây là lối tắt <b>chỉ có khi chạy dữ liệu demo</b>. Trên hệ thống thật, tài khoản mới
              không có mật khẩu — người dùng tự đặt bằng mã kích hoạt.
            </p>
          </>
        ) : kichHoat?.maKichHoat ? (
          <>
            <p className={styles.warn}>
              Đây là <b>lần duy nhất</b> mã hiện ra. Hệ thống chỉ lưu bản băm nên không xem lại
              được — đóng cửa sổ này mà chưa lưu thì phải cấp mã mới.
            </p>
            <p className={styles.codeBox}>
              <code>{kichHoat.maKichHoat}</code>
            </p>
            <p>
              Trao mã cho <b>{kichHoat.tenDangNhap}</b>. Người dùng vào trang kích hoạt, nhập tên
              đăng nhập, mã này và mật khẩu mới. Gõ thường hay thiếu dấu gạch vẫn khớp.
            </p>
          </>
        ) : (
          <>
            <p>
              Mã kích hoạt đã được gửi tới <b>{kichHoat?.guiToiEmail}</b>. Bạn không nhìn thấy mã —
              người nhận tự mở thư.
            </p>
            <p className={styles.hintBox}>
              Kích hoạt thành công thì email này được coi là <b>đã xác minh</b>. Nếu thư không tới,
              dùng <b>Cấp lại mã</b> ở danh bạ và chọn trao tay.
            </p>
          </>
        )}

        {kichHoat ? (
          <p className={styles.sub}>
            Mã hết hạn lúc {hanDung(kichHoat.hetHan)}. Sai 5 lần thì mã bị thu hồi và phải cấp lại.
          </p>
        ) : null}
      </div>
    </Dialog>
  )
}

function hanDung(iso: string): string {
  const value = Date.parse(iso)
  return Number.isNaN(value) ? '—' : new Date(value).toLocaleString('vi-VN')
}
