import type { ReactNode } from 'react'

import { Dialog, Icon } from '@/shared/ui'

import styles from './AuthDialog.module.scss'

export interface AuthConfirmDialogProps {
  open: boolean
  title: string
  confirmLabel: string
  pending?: boolean
  danger?: boolean
  onClose: () => void
  onConfirm: () => void
  children: ReactNode
}

/**
 * Hộp xác nhận dùng cho các thao tác không đảo ngược được của luồng
 * credential: kích hoạt (mã dùng một lần), đặt lại mật khẩu (thu hồi mọi
 * phiên). Luôn mở khi được render — nơi gọi tháo hẳn khi đóng để sự kiện
 * `close` của `<dialog>` cũ không xoá nhầm hộp thoại vừa mở.
 */
export function AuthConfirmDialog({
  open,
  title,
  confirmLabel,
  pending = false,
  danger = false,
  onClose,
  onConfirm,
  children,
}: AuthConfirmDialogProps) {
  if (!open) return null

  return (
    <Dialog
      open
      onClose={onClose}
      ariaLabel={title}
      header={<p className={styles.title}>{title}</p>}
      footer={
        <>
          <button type="button" className={styles.ghost} onClick={onClose} disabled={pending}>
            Để kiểm tra lại
          </button>
          <button
            type="button"
            className={danger ? styles.danger : styles.primary}
            onClick={onConfirm}
            disabled={pending}
          >
            <Icon name={danger ? 'lock' : 'check'} size="13px" />
            {pending ? 'Đang xử lý…' : confirmLabel}
          </button>
        </>
      }
    >
      <div className={styles.body}>{children}</div>
    </Dialog>
  )
}

export interface AuthInfoDialogProps {
  open: boolean
  title: string
  actionLabel: string
  onClose: () => void
  children: ReactNode
}

/**
 * Hộp thông báo (không có hành động nguy hiểm): báo đã gửi mã, nhắc kiểm tra
 * hộp thư và thư rác. Chỉ một nút đóng để mắt không phải chọn.
 */
export function AuthInfoDialog({ open, title, actionLabel, onClose, children }: AuthInfoDialogProps) {
  if (!open) return null

  return (
    <Dialog
      open
      onClose={onClose}
      ariaLabel={title}
      header={<p className={styles.title}>{title}</p>}
      footer={
        <button type="button" className={styles.primary} onClick={onClose}>
          <Icon name="check" size="13px" />
          {actionLabel}
        </button>
      }
    >
      <div className={styles.body}>{children}</div>
    </Dialog>
  )
}
