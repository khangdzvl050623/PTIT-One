import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'

import { setReauthenticator } from '@/shared/api'

import * as authApi from '../api/authApi'
import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './AuthContext'
import { coordinatedRefresh, markSessionEnded } from './refreshCoordinator'
import type { AuthStatus, Role, SessionUser } from './types'

/** Làm mới trước khi access hết hạn để người dùng không gặp 401 giữa thao tác. */
const REFRESH_LEAD_MS = 60_000
const MIN_REFRESH_DELAY_MS = 5_000

export interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<SessionUser | null>(null)
  /* Giữ user mới nhất cho các callback chạy ngoài vòng render (timer, retry
     của lớp API) mà không phải khai lại dependency. */
  const userRef = useRef<SessionUser | null>(null)

  const apply = useCallback((next: SessionUser | null) => {
    userRef.current = next
    setUser(next)
    setStatus(next ? 'authenticated' : 'anonymous')
  }, [])

  /* Lớp `shared/api` gọi hàm này khi một request nghiệp vụ trả 401. Đăng ký
     ngược chiều để `shared` không phải import module auth. */
  useEffect(() => {
    setReauthenticator(async () => {
      const renewed = await coordinatedRefresh()
      apply(renewed)
      return renewed !== null
    })
    return () => setReauthenticator(null)
  }, [apply])

  /* Khôi phục phiên khi mở lại trang hoặc F5: cookie vẫn còn, chỉ cần hỏi
     server xem còn hiệu lực không. Access hết hạn thì thử làm mới một lần. */
  useEffect(() => {
    let cancelled = false

    void (async () => {
      /* Không gán giá trị khởi tạo: cả hai nhánh đều ghi trước khi đọc, nên
         `null` ban đầu là thừa (eslint no-useless-assignment). */
      let restored: SessionUser | null
      try {
        restored = await authApi.fetchCurrentUser()
      } catch {
        restored = await coordinatedRefresh()
      }
      if (!cancelled) apply(restored)
    })()

    return () => {
      cancelled = true
    }
  }, [apply])

  // Hẹn giờ làm mới trước hạn access. Đặt lại mỗi lần phiên đổi.
  useEffect(() => {
    if (!user) return

    const dueAt = Date.parse(user.accessExpiresAt)
    if (Number.isNaN(dueAt)) return

    const delay = Math.max(dueAt - Date.now() - REFRESH_LEAD_MS, MIN_REFRESH_DELAY_MS)
    const timer = window.setTimeout(() => {
      void coordinatedRefresh().then(apply)
    }, delay)

    return () => window.clearTimeout(timer)
  }, [user, apply])

  const signIn = useCallback(
    async (username: string, password: string) => {
      const signedIn = await authApi.login(username, password)
      apply(signedIn)
      return signedIn
    },
    [apply],
  )

  const endSession = useCallback(
    async (call: () => Promise<void>) => {
      try {
        await call()
      } finally {
        /* Dù server báo lỗi vẫn xoá trạng thái phía client: giữ lại màn hình
           "đã đăng nhập" trong khi phiên có thể đã bị thu hồi là sai lệch. */
        markSessionEnded()
        apply(null)
      }
    },
    [apply],
  )

  const signOut = useCallback(() => endSession(authApi.logout), [endSession])
  const signOutEverywhere = useCallback(() => endSession(authApi.logoutAll), [endSession])

  const hasRole = useCallback(
    (...roles: Role[]) => {
      const current = userRef.current
      return current !== null && roles.includes(current.role)
    },
    [],
  )

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, signIn, signOut, signOutEverywhere, hasRole }),
    [status, user, signIn, signOut, signOutEverywhere, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
