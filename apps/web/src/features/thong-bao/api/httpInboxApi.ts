import { apiFetch } from '@/shared/api'

import type { InboxPageData, UnreadCount } from './inboxTypes'

/**
 * Hộp thư của chính mình — danh tính lấy từ JWT nên không có tham số người
 * dùng nào. Một lần tải một trang (`kichThuoc` tối đa 50 theo hợp đồng).
 */
export function fetchInbox(trang = 0, kichThuoc = 50): Promise<InboxPageData> {
  const query = new URLSearchParams({ trang: String(trang), kichThuoc: String(kichThuoc) })
  return apiFetch<InboxPageData>(`/api/me/notifications?${query}`)
}

/** Cho chuông: rẻ hơn tải cả trang. */
export function fetchUnreadCount(): Promise<UnreadCount> {
  return apiFetch<UnreadCount>('/api/me/notifications/unread-count')
}

/** Đánh dấu một bản đã đọc — đọc lại là no-op, trả số chưa đọc mới. */
export function markRead(maThongBao: string): Promise<UnreadCount> {
  return apiFetch<UnreadCount>(`/api/me/notifications/${encodeURIComponent(maThongBao)}/read`, {
    method: 'POST',
  })
}

/** Đánh dấu tất cả đã đọc. */
export function markAllRead(): Promise<UnreadCount> {
  return apiFetch<UnreadCount>('/api/me/notifications/read-all', { method: 'POST' })
}
