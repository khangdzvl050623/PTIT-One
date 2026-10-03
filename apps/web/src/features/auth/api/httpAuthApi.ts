import { apiFetch } from '@/shared/api'

import type { SessionUser } from '../model/types'

/**
 * Bốn endpoint auth. Không nhận và không trả token — mọi thứ đi qua cookie.
 *
 * Các đường dẫn này đều nằm dưới `/api/auth/` nên lớp `apiFetch` **không** tự
 * làm mới phiên khi gặp 401: ở đây 401 là kết luận thật (sai mật khẩu, phiên
 * đã mất), không phải access hết hạn.
 */

export function login(username: string, password: string): Promise<SessionUser> {
  return apiFetch<SessionUser>('/api/auth/login', {
    method: 'POST',
    json: { username, password },
  })
}

export function fetchCurrentUser(): Promise<SessionUser> {
  return apiFetch<SessionUser>('/api/auth/me')
}

/** Rotate refresh token và phát access mới. Hạn tuyệt đối của phiên không đổi. */
export function refreshSession(): Promise<SessionUser> {
  return apiFetch<SessionUser>('/api/auth/refresh', { method: 'POST' })
}

/** Chỉ thu hồi phiên hiện tại. Thiết bị khác không bị ảnh hưởng. */
export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}

/** Thu hồi mọi phiên của tài khoản và tăng phiên bản tài khoản. */
export function logoutAll(): Promise<void> {
  return apiFetch<void>('/api/auth/logout-all', { method: 'POST' })
}
