import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError } from '@/shared/api'
import { LABELS, ROUTES } from '@/shared/constants'
import { Panel } from '@/shared/ui'

import { useAuth } from '../model/AuthContext'
import { ROLE_LABELS } from '../model/types'
import { LoginForm } from './LoginForm'
import type { LoginCredentials } from './LoginForm'
import styles from './LoginPanel.module.scss'

const NETWORK_MESSAGE = 'Không kết nối được máy chủ. Vui lòng thử lại.'

export interface LoginPanelProps {
  /** Nơi chuyển tới sau khi đăng nhập. Bỏ trống thì ở nguyên trang hiện tại. */
  redirectTo?: string
}

/**
 * Ô đăng nhập của cổng thông tin. Đã đăng nhập thì đổi thành thẻ danh tính —
 * giữ nguyên biểu mẫu lúc đó sẽ khiến người dùng tưởng mình chưa vào được.
 */
export function LoginPanel({ redirectTo }: LoginPanelProps) {
  const { status, user, signIn, signOut } = useAuth()
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit({ username, password }: LoginCredentials) {
    setPending(true)
    setError(null)
    try {
      await signIn(username, password)
      if (redirectTo) navigate(redirectTo, { replace: true })
    } catch (cause) {
      /* Backend cố tình trả cùng một thông báo cho sai mật khẩu, tài khoản
         chưa kích hoạt và tài khoản bị ngừng — để form này không trở thành
         công cụ dò xem tài khoản nào có thật. Hiển thị đúng thông báo đó. */
      setError(cause instanceof ApiError ? cause.message : NETWORK_MESSAGE)
    } finally {
      setPending(false)
    }
  }

  /* Chưa biết còn phiên hay không. Hiện biểu mẫu lúc này rồi đổi sang thẻ
     danh tính ngay sau đó sẽ nhấp nháy khó chịu mỗi lần tải trang. */
  if (status === 'loading') {
    return (
      <Panel title={LABELS.login} icon="user">
        <p className={styles.role}>Đang kiểm tra phiên đăng nhập…</p>
      </Panel>
    )
  }

  if (user) {
    return (
      <Panel title={LABELS.account} icon="user">
        <div className={styles.identity}>
          <p className={styles.name}>{user.username}</p>
          <p className={styles.role}>{ROLE_LABELS[user.role]}</p>
          <Link className={styles.link} to={ROUTES.account}>
            {LABELS.account}
          </Link>
          <button
            className={styles.signOut}
            type="button"
            disabled={pending}
            onClick={() => {
              setPending(true)
              void signOut().finally(() => setPending(false))
            }}
          >
            {LABELS.logout}
          </button>
        </div>
      </Panel>
    )
  }

  return (
    <Panel title={LABELS.login} icon="user">
      <LoginForm
        onSubmit={(credentials) => void handleSubmit(credentials)}
        pending={pending}
        errorMessage={error}
      />
    </Panel>
  )
}
