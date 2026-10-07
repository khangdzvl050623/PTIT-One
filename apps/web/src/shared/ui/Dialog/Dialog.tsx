import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'

import { Icon } from '../Icon'

import styles from './Dialog.module.scss'

export interface DialogProps {
  open: boolean
  onClose: () => void
  /** Tên hộp thoại cho trình đọc màn hình. */
  ariaLabel: string
  /** Phần đầu (thông tin chính) — có nút × ở góc. */
  header: ReactNode
  children: ReactNode
  /** Mặc định là nút "Đóng". */
  footer?: ReactNode
}

/**
 * Hộp thoại dùng `<dialog>` gốc của trình duyệt: `showModal()` tự lo nền mờ,
 * khoá focus bên trong, Esc để đóng và trả focus về nút đã mở — không phải
 * tự viết bẫy focus.
 */
export function Dialog({ open, onClose, ariaLabel, header, children, footer }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      aria-label={ariaLabel}
      // Esc hoặc dialog.close() đều đi qua đây — đồng bộ lại state của cha.
      onClose={onClose}
      // Bấm vào nền mờ (chính phần tử dialog, ngoài khung nội dung) thì đóng.
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className={styles.box}>
        <header className={styles.header}>
          <div className={styles.headerBody}>{header}</div>
          <button type="button" className={styles.x} onClick={onClose} aria-label="Đóng">
            <Icon name="close" size="18px" />
          </button>
        </header>
        <div className={styles.body}>{children}</div>
        <footer className={styles.footer}>
          {footer ?? (
            <button type="button" className={styles.closeBtn} onClick={onClose}>
              <Icon name="close" size="13px" />
              Đóng
            </button>
          )}
        </footer>
      </div>
    </dialog>
  )
}
