import type { ReactNode } from 'react'

import { Dialog } from '@/shared/ui'

import styles from './NoticeDetailDialog.module.scss'

export interface DetailRow {
  label: string
  content: ReactNode
}

export interface NoticeDetailDialogProps {
  title: string
  rows: readonly DetailRow[]
  onClose: () => void
  /** Dòng tải/lỗi hiện trên danh sách hàng (ví dụ chi tiết đang nạp lại). */
  status?: ReactNode
}

/** Ngày hiển thị kiểu cổng gốc: dd/mm/yyyy. Dùng chung cho mọi hàng tin. */
export function formatNgay(iso: string): string {
  const time = Date.parse(iso)
  return Number.isNaN(time) ? '—' : new Date(time).toLocaleDateString('vi-VN')
}

/**
 * Dialog chi tiết thông báo dùng chung (hộp thư đến/gửi): hàng nhãn 90px +
 * giá trị. Nội dung cần nhấn (`strong`), giữ dòng (`body`) hay link nội bộ
 * (`innerLink`) thì bọc bằng class của module này — đừng đẻ thêm bản sao ở
 * màn gọi.
 */
export function NoticeDetailDialog({ title, rows, onClose, status }: NoticeDetailDialogProps) {
  return (
    <Dialog
      open
      onClose={onClose}
      ariaLabel={title}
      header={<p className={styles.dialogTitle}>{title}</p>}
    >
      {status}
      <dl className={styles.detail}>
        {rows.map((row) => (
          <div key={row.label} className={styles.line}>
            <dt>{row.label}</dt>
            <dd>{row.content}</dd>
          </div>
        ))}
      </dl>
    </Dialog>
  )
}
