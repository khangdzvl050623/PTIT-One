import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import { ApiError } from '@/shared/api'
import { ROUTES } from '@/shared/constants'
import { Icon, setPendingFlash } from '@/shared/ui'
import type { Flash } from '@/shared/ui'

import { changePassword } from '../api/credentialApi'
import { useAuth } from '../model/AuthContext'
import { PasswordField } from './PasswordField'
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
  const navigate = useNavigate()
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
      /* Server đã thu hồi mọi phiên nên không còn gì để hiển thị ở đây: sang
         login kèm tin một lần, trang đó hiện banner xanh. Gửi qua cả state lẫn
         kho dự phòng — người gác tuyến có thể ghi đè entry giữa đường. */
      const flash: Flash = {
        kind: 'success',
        text: 'Đã đổi mật khẩu. Mọi thiết bị đã bị đăng xuất — đăng nhập lại bằng mật khẩu mới.',
      }
      setPendingFlash(flash)
      navigate(ROUTES.login, { replace: true, state: { flash } })
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
      setPending(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
      <PasswordField
        label="Mật khẩu hiện tại"
        value={hienTai}
        onChange={setHienTai}
        autoComplete="current-password"
        disabled={pending}
      />

      <PasswordField
        label="Mật khẩu mới"
        value={moi}
        onChange={setMoi}
        placeholder="Ít nhất 8 ký tự"
        autoComplete="new-password"
        minLength={8}
        maxLength={128}
        disabled={pending}
      />

      <PasswordField
        label="Nhập lại mật khẩu mới"
        value={nhapLai}
        onChange={setNhapLai}
        autoComplete="new-password"
        disabled={pending}
      />

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
        <p key={error} className={`${styles.message} ${styles.error}`} role="alert">
          {error}
        </p>
      ) : null}
    </form>
  )
}
