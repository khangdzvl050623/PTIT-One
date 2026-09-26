import { fetchCurrentUser, refreshSession } from '../api/authApi'

import type { SessionUser } from './types'

/**
 * Bảo đảm **chỉ một** lượt làm mới phiên chạy tại một thời điểm.
 *
 * Backend rotate refresh token: token cũ bị đánh dấu đã dùng. Hai lượt refresh
 * song song cùng một token sẽ bị coi là **replay** và backend thu hồi cả phiên —
 * người dùng bị đá ra. Nên cần khoá ở hai mức:
 *
 * - Trong một tab: gộp mọi lời gọi vào cùng một promise.
 * - Giữa các tab: Web Locks, vì các tab dùng chung cookie.
 */

const LOCK_NAME = 'ptitone-auth-refresh'
const LAST_REFRESH_KEY = 'ptitone:auth:last-refresh'
/** Trong khoảng này coi như tab khác vừa rotate xong, cookie đã mới. */
const RECENT_WINDOW_MS = 5_000

interface LockManagerLike {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>
}

let inFlight: Promise<SessionUser | null> | null = null

/** Không bao giờ ném lỗi: `null` nghĩa là phiên đã mất, hãy đăng nhập lại. */
export function coordinatedRefresh(): Promise<SessionUser | null> {
  if (!inFlight) {
    inFlight = withCrossTabLock(refreshOrReuse).finally(() => {
      inFlight = null
    })
  }
  return inFlight
}

export function markSessionEnded(): void {
  writeStamp(null)
}

async function refreshOrReuse(): Promise<SessionUser | null> {
  if (refreshedRecently()) {
    try {
      /* Tab khác vừa rotate; cookie trong trình duyệt đã là token mới. Đọc lại
         danh tính thay vì rotate thêm một lần nữa — lượt rotate thừa đó biến
         token vừa cấp thành "đã dùng" và làm cả hai tab mất phiên. */
      return await fetchCurrentUser()
    } catch {
      /* Lần refresh kia có thể đã hỏng — rơi xuống refresh thật bên dưới. */
    }
  }

  try {
    const user = await refreshSession()
    writeStamp(Date.now())
    return user
  } catch {
    writeStamp(null)
    return null
  }
}

async function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  const locks = lockManager()
  return locks ? locks.request(LOCK_NAME, task) : task()
}

/* Truy cập theo kiểu cấu trúc để không phụ thuộc phiên bản lib DOM, và vẫn
   chạy được trên trình duyệt chưa có Web Locks (lúc đó chỉ còn khoá trong tab). */
function lockManager(): LockManagerLike | null {
  if (typeof navigator === 'undefined') return null
  return (navigator as unknown as { locks?: LockManagerLike }).locks ?? null
}

function refreshedRecently(): boolean {
  const stamp = readStamp()
  return stamp !== null && Date.now() - stamp < RECENT_WINDOW_MS
}

/* localStorage ném lỗi ở chế độ riêng tư hoặc khi trình duyệt chặn lưu trữ.
   Mất dấu thời gian chỉ làm mất bước tối ưu, không làm sai tính đúng đắn. */
function readStamp(): number | null {
  try {
    const raw = window.localStorage.getItem(LAST_REFRESH_KEY)
    if (!raw) return null
    const value = Number(raw)
    return Number.isFinite(value) ? value : null
  } catch {
    return null
  }
}

function writeStamp(value: number | null): void {
  try {
    if (value === null) window.localStorage.removeItem(LAST_REFRESH_KEY)
    else window.localStorage.setItem(LAST_REFRESH_KEY, String(value))
  } catch {
    /* Bỏ qua: xem ghi chú ở readStamp. */
  }
}
