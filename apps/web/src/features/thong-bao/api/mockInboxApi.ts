import type { InboxItem, InboxPageData, UnreadCount } from './inboxTypes'
import { INBOX_MOCK } from '../data/inbox'

function page(items: readonly InboxItem[]): InboxPageData {
  return {
    soChuaDoc: items.filter((n) => !n.daDoc).length,
    trang: 0,
    kichThuoc: items.length,
    thongBao: [...items],
  }
}

/** Dữ liệu mẫu cho hộp thư — lẫn đã/chưa đọc, cả soạn tay lẫn tự sinh. */
export function fetchInbox(): Promise<InboxPageData> {
  return Promise.resolve(page(INBOX_MOCK))
}

export function fetchUnreadCount(): Promise<UnreadCount> {
  return Promise.resolve({ soChuaDoc: INBOX_MOCK.filter((n) => !n.daDoc).length })
}

export function markRead(maThongBao: string): Promise<UnreadCount> {
  const item = INBOX_MOCK.find((n) => n.maThongBao === maThongBao)
  if (item) item.daDoc = true
  return fetchUnreadCount()
}

export function markAllRead(): Promise<UnreadCount> {
  for (const item of INBOX_MOCK) item.daDoc = true
  return Promise.resolve({ soChuaDoc: 0 })
}
