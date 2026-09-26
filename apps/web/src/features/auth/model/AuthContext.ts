import { createContext, useContext } from 'react'

import type { AuthStatus, Role, SessionUser } from './types'

export interface AuthContextValue {
  status: AuthStatus
  user: SessionUser | null
  /** Ném `ApiError` khi sai thông tin đăng nhập, để form hiển thị đúng lý do. */
  signIn: (username: string, password: string) => Promise<SessionUser>
  signOut: () => Promise<void>
  /** Đăng xuất khỏi mọi thiết bị. */
  signOutEverywhere: () => Promise<void>
  hasRole: (...roles: Role[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth phải nằm trong <AuthProvider>.')
  }
  return value
}
