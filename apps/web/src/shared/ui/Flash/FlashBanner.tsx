import { useEffect } from 'react'
import type { ReactNode } from 'react'

import { Icon } from '../Icon'

import styles from './FlashBanner.module.scss'

/** Lời nhắn mang theo một lần điều hướng — trang đích hiện rồi thu hồi. */
export interface Flash {
  kind: 'success' | 'info'
  text: ReactNode
}

/** Hình dạng `location.state` ở những trang đọc hoặc giữ flash. */
export interface FlashRouteState {
  from?: string
  flash?: Flash
}

export interface FlashBannerProps {
  flash: Flash
  onClose: () => void
}

/** Thời gian banner tồn tại — thanh chạy và hẹn tắt dùng chung hằng này. */
const FLASH_MS = 3000

/**
 * Banner một lần sau redirect (đổi mật khẩu xong, kích hoạt xong…). Cùng ngôn
 * ngữ với box success nội tuyến: vòng tích xanh, viền trái đậm. Hiện 3 giây
 * với thanh chạy đếm ngược rồi tự ẩn; bấm × để tắt ngay; `role="status"` để
 * trình đọc màn hình đọc khi hiện.
 */
export function FlashBanner({ flash, onClose }: FlashBannerProps) {
  useEffect(() => {
    const timer = window.setTimeout(onClose, FLASH_MS)
    return () => window.clearTimeout(timer)
  }, [onClose])

  const info = flash.kind === 'info'

  return (
    <p className={`${styles.banner} ${info ? styles.info : styles.success}`} role="status">
      <span className={styles.icon} aria-hidden="true">
        <Icon name={info ? 'bell' : 'check'} size="15px" />
      </span>
      <span className={styles.text}>{flash.text}</span>
      <button type="button" className={styles.x} onClick={onClose} aria-label="Đóng thông báo">
        <Icon name="close" size="14px" />
      </button>
      <span
        className={styles.progress}
        aria-hidden="true"
        style={{ animationDuration: `${FLASH_MS}ms` }}
      />
    </p>
  )
}
