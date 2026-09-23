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
}

export function LoginForm({ onSubmit }: LoginFormProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!onSubmit) {
      setMessage(LABELS.loginPending)
      return
    }

    setMessage('')
    onSubmit({ username, password })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <label className={styles.field}>
        <span className={styles.fieldIcon}>
          <Icon name="user" />
        </span>
        <input
          className={styles.input}
          type="text"
          name="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder={LABELS.username}
          autoComplete="username"
          required
        />
      </label>

      <label className={styles.field}>
        <span className={styles.fieldIcon}>
          <Icon name="lock" />
        </span>
        <input
          className={styles.input}
          type="password"
          name="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder={LABELS.password}
          autoComplete="current-password"
          required
        />
      </label>

      <button className={styles.submit} type="submit">
        <Icon name="signIn" size="12px" />
        <span>{LABELS.loginSubmit}</span>
      </button>

      {message ? <p className={styles.message}>{message}</p> : null}
    </form>
  )
}
