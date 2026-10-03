import { useState } from 'react'
import type { FormEvent } from 'react'

import { LABELS } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import styles from './LoginForm.module.scss'

export interface LoginCredentials {
  username: string
  password: string
}

export interface LoginFormProps {
  /**
   * Nơi xử lý đăng nhập. Chưa nối API thì bỏ trống — form hiện ghi chú
   * thay vì giả báo đăng nhập thành công.
   */
  onSubmit?: (credentials: LoginCredentials) => void
  /** Khoá nút trong lúc chờ server, tránh bấm lặp. */
  pending?: boolean
  /** Lý do đăng nhập hỏng, lấy từ `message` của API. */
  errorMessage?: string | null
}

export function LoginForm({ onSubmit, pending = false, errorMessage = null }: LoginFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!onSubmit) {
      setMessage(LABELS.loginPending)
      return
    }

    setMessage('')
    onSubmit({ username, password })
  }

  /* Lỗi từ server được ưu tiên hơn ghi chú nội bộ của form. */
  const notice = errorMessage ?? message

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <label className={styles.field}>
        <span className={styles.fieldIcon}>
          <Icon name="user" size="18px" />
        </span>
        <input
          className={styles.input}
          type="text"
          name="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder={LABELS.username}
          autoComplete="username"
          disabled={pending}
          required
        />
      </label>

      <label className={styles.field}>
        <span className={styles.fieldIcon}>
          <Icon name="lock" size="18px" />
        </span>
        <input
          className={styles.input}
          type={showPassword ? 'text' : 'password'}
          name="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder={LABELS.password}
          autoComplete="current-password"
          disabled={pending}
          required
        />
        {/* Icon theo trạng thái hiện tại như cổng gốc: đang ẩn thì mắt gạch chéo. */}
        <button
          className={styles.reveal}
          type="button"
          onClick={() => setShowPassword((shown) => !shown)}
          aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          aria-pressed={showPassword}
          disabled={pending}
        >
          <Icon name={showPassword ? 'eye' : 'eyeOff'} size="18px" />
        </button>
      </label>

      <button className={styles.submit} type="submit" disabled={pending}>
        <Icon name="signIn" size="15px" />
        <span>{pending ? LABELS.loginSubmitting : LABELS.loginSubmit}</span>
      </button>

      {/* aria-live để trình đọc màn hình đọc lỗi mà không cần chuyển focus. */}
      {notice ? (
        <p
          className={errorMessage ? `${styles.message} ${styles.error}` : styles.message}
          role={errorMessage ? 'alert' : undefined}
          aria-live="polite"
        >
          {notice}
        </p>
      ) : null}
    </form>
  )
}
