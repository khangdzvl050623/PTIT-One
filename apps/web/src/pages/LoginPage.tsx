import { Navigate, useLocation } from 'react-router-dom'

import { LoginPanel, useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/constants'

import styles from './LoginPage.module.scss'

/** Nơi người dùng định tới trước khi bị chặn, do `RequireAuth` gắn vào state. */
interface RedirectState {
  from?: string
}

export function LoginPage() {
  const { status } = useAuth()
  const location = useLocation()

  const target = (location.state as RedirectState | null)?.from ?? ROUTES.home

  // Đã đăng nhập thì không mở lại màn đăng nhập; quay về nơi định tới.
  if (status === 'authenticated') {
    return <Navigate to={target} replace />
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <LoginPanel redirectTo={target} />
      </div>
    </div>
  )
}
