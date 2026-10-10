import { useEffect, useState } from 'react'

import { fetchUnreadCount } from '../api/inboxApi'

import styles from './UnreadBadge.module.scss'

/**
 * Số chưa đọc cho chấm đỏ trên link. Chỉ gọi ở chỗ thật sự vẽ chấm (sidebar,
 * navbar) — tile số liệu ở trang Thông tin đã có nguồn riêng, không dùng ké
 * để khỏi tải trùng.
 */
export function useUnreadCount(enabled = true): number {
  const [count, setCount] = useState(0)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    void fetchUnreadCount().then(
      (found) => {
        if (!cancelled) setCount(found.soChuaDoc)
      },
      () => undefined,
    )
    return () => {
      cancelled = true
    }
  }, [enabled])

  return count
}

/** Huy hiệu đếm cho sidebar: vòng đỏ ghi số chưa đọc, hết thì tự ẩn. */
export function UnreadBadge({ count }: { count: number }) {
  if (count <= 0) return null
  return (
    <span className={styles.badge} aria-label={`${count} thông báo chưa đọc`}>
      {count > 99 ? '99+' : count}
    </span>
  )
}
