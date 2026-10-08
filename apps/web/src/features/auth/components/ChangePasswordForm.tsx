import { useState } from 'react'
import type { FormEvent } from 'react'

import { ApiError } from '@/shared/api'
import { Icon } from '@/shared/ui'

import { changePassword } from '../api/credentialApi'
import { useAuth } from '../model/AuthContext'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/**
 * Đổi mật khẩu của chính mình.
 *
 * Server thu hồi **mọi phiên kể cả phiên này** rồi xoá cookie, nên sau khi
 * thành công không còn gì để hiển thị: gọi `signOut` để trạng thái phía client
 * khớp với thực tế, rồi để người dùng đăng nhập lại. Giữ nguyên màn "đã đăng
 * nhập" lúc này là nói dối — request kế tiếp sẽ nhận `401`.
 */
export function ChangePasswordForm() {
  const { signOut } = useAuth()
  const [hienTai, setHienTai] = useState('')
  const [moi, setMoi] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (moi !== nhapLai) {
      setError('Hai lần nhập mật khẩu mới không giống nhau.')
      return
    }
    setPending(true)
    setError(null)
    try {
      await changePassword(hienTai, moi)
      await signOut()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
      setPending(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
      <label className={styles.field}>
        Mật khẩu hiện tại
        <input
          className={styles.input}
          type="password"
          value={hienTai}
          onChange={(event) => setHienTai(event.target.value)}
          autoComplete="current-password"
          disabled={pending}
          required
        />
      </label>

      <label className={styles.field}>
        Mật khẩu mới
        <input
          className={styles.input}
          type="password"
          value={moi}
          onChange={(event) => setMoi(event.target.value)}
          placeholder="Ít nhất 8 ký tự"
          autoComplete="new-password"
          minLength={8}
          maxLength={128}
          disabled={pending}
          required
        />
      </label>

      <label className={styles.field}>
        Nhập lại mật khẩu mới
        <input
          className={styles.input}
          type="password"
          value={nhapLai}
          onChange={(event) => setNhapLai(event.target.value)}
          autoComplete="new-password"
          disabled={pending}
          required
        />
      </label>

      <p className={styles.hint}>
        Đổi xong, <b>mọi thiết bị đang đăng nhập đều bị đăng xuất</b>, kể cả máy này.
        Bạn sẽ quay về trang đăng nhập.
      </p>

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={pending}>
          <Icon name="lock" size="15px" />
          {pending ? 'Đang đổi…' : 'Đổi mật khẩu'}
        </button>
      </div>

      {error ? (
        <p className={`${styles.message} ${styles.error}`} role="alert">
          {error}
        </p>
      ) : null}
    </form>
  )
}
