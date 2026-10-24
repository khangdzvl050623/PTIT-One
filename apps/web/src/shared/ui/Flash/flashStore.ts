import type { Flash } from './FlashBanner'

/**
 * Kho một-lần cho flash, dự phòng khi `location.state` không tới được trang
 * đích.
 *
 * Trường hợp điển hình: đổi mật khẩu xong gọi `signOut()` rồi `navigate` kèm
 * flash — nhưng `RequireAuth` vẫn còn mounted lúc đó nên effect `<Navigate>`
 * của nó chạy sau và ghi đè entry bằng state chỉ có `from`. Kho này không qua
 * history nên không bên nào ghi đè được; trang đích đọc một lần rồi thôi.
 */
let pending: Flash | null = null

export function setPendingFlash(flash: Flash): void {
  pending = flash
}

/** Đọc rồi xoá ngay — gọi lần hai trả `null`. */
export function consumePendingFlash(): Flash | null {
  const next = pending
  pending = null
  return next
}

/**
 * Xoá flash khỏi entry hiện tại để F5 hay back không hiện lại.
 *
 * Chỉ hạ trường `usr` của React Router, giữ nguyên `key`/`idx` — xoá cả object
 * state sẽ làm sổ sách history của router lệch.
 */
export function clearLocationFlash(): void {
  const raw = (window.history.state ?? {}) as Record<string, unknown>
  window.history.replaceState({ ...raw, usr: undefined }, '')
}
