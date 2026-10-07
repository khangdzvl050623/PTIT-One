import { Navigate, useLocation } from 'react-router-dom'

import { safeTarget } from '@/app/router/navigation'
import { LoginPanel, useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/constants'

import styles from './LoginPage.module.scss'

/** Nơi người dùng định tới trước khi bị chặn, do `RequireAuth` gắn vào state. */
interface RedirectState {
  from?: string
}

export function LoginPage() {
  const { status, user } = useAuth()
  const location = useLocation()

  const from = (location.state as RedirectState | null)?.from

  /* Đã đăng nhập thì không mở lại màn đăng nhập. Nơi đến phải hợp vai trò:
     quay lại một tuyến của vai trò khác sẽ rơi vào /khong-du-quyen. */
  if (status === 'authenticated' && user) {
    return <Navigate to={safeTarget(from, user.role)} replace />
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {/* Chưa biết vai trò nên chưa kiểm được; LoginPanel kiểm sau khi đăng nhập. */}
        <LoginPanel redirectTo={from ?? ROUTES.userInfo} />
      </div>
    </div>
  )
}
