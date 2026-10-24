import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError } from '@/shared/api'
import { LABELS, ROUTES } from '@/shared/constants'
import { Icon, Panel, type Flash } from '@/shared/ui'

import { API_MODE } from '@/shared/api'
import { MOCK_ACCOUNTS } from '../api/mockAuthApi'
import { useAuth } from '../model/AuthContext'
import { ROLE_LABELS } from '../model/types'
import type { Role } from '../model/types'
import { LoginForm } from './LoginForm'
import type { LoginCredentials } from './LoginForm'
import styles from './LoginPanel.module.scss'

const NETWORK_MESSAGE = 'Không kết nối được máy chủ. Vui lòng thử lại.'

export interface LoginPanelProps {
  /**
   * Nơi chuyển tới sau khi đăng nhập. Bỏ trống thì ở nguyên trang hiện tại.
   *
   * Là **hàm nhận vai trò**, không phải chuỗi: đích đến phụ thuộc vai trò (một
   * tuyến của vai trò khác sẽ rơi vào `/khong-du-quyen`), mà vai trò chỉ biết
   * sau khi đăng nhập xong. Việc chọn đích thuộc tầng tuyến đường — để component
   * này tự tính thì nó phải import `app/router`, tạo vòng
   * `navigation → features/auth → LoginPanel → navigation`.
   */
  redirectTo?: (role: Role) => string
  /** Tin một lần mang tiếp sang trang đích (trang nào đọc thì hiện). */
  flash?: Flash | null
}

/**
 * Ô đăng nhập của cổng thông tin. Đã đăng nhập thì đổi thành thẻ danh tính —
 * giữ nguyên biểu mẫu lúc đó sẽ khiến người dùng tưởng mình chưa vào được.
 */
export function LoginPanel({ redirectTo, flash }: LoginPanelProps) {
  const { status, user, signIn, signOut } = useAuth()
  const navigate = useNavigate()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit({ username, password }: LoginCredentials) {
    setPending(true)
    setError(null)
    try {
      const signedIn = await signIn(username, password)
      // Tính đích SAU khi biết vai trò — xem ghi chú ở `redirectTo`.
      if (redirectTo) {
        const to = redirectTo(signedIn.role)
        navigate(to, flash ? { replace: true, state: { flash } } : { replace: true })
      }
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
        <p className={styles.loading}>Đang kiểm tra phiên đăng nhập…</p>
      </Panel>
    )
  }

  if (user) {
    return (
      /* Đã vào thì khung đổi tên thành TÀI KHOẢN — giữ chữ "Đăng nhập" lúc
         này dễ khiến người dùng tưởng mình chưa vào được. */
      <Panel title={LABELS.account.toLocaleUpperCase('vi')} icon="user">
        <dl className={styles.identity}>
          <div className={styles.row}>
            <dt className={styles.term}>{LABELS.account}</dt>
            <dd className={styles.value}>{user.username}</dd>
          </div>
          <div className={styles.row}>
            <dt className={styles.term}>Họ tên</dt>
            {/* `/api/auth/me` chưa trả họ tên — thiếu thì hiện vai trò, không để trống. */}
            <dd className={styles.value}>{user.hoTen ?? ROLE_LABELS[user.role]}</dd>
          </div>
        </dl>
        <button
          className={styles.signOut}
          type="button"
          disabled={pending}
          onClick={() => {
            setPending(true)
            void signOut().finally(() => setPending(false))
          }}
        >
          <Icon name="signOut" size="15px" />
          <span>{LABELS.logout}</span>
        </button>
        <Link className={styles.changePassword} to={ROUTES.doiMatKhau}>
          Đổi mật khẩu
        </Link>
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
      {/* Tài khoản vừa được cấp chưa có mật khẩu nên KHÔNG đăng nhập được —
          không có lối này thì màn kích hoạt coi như không tồn tại. */}
      <p className={styles.helpLinks}>
        <Link to={ROUTES.kichHoat}>Kích hoạt tài khoản lần đầu</Link>
        <Link to={ROUTES.quenMatKhau}>Quên mật khẩu?</Link>
      </p>

      {API_MODE === 'mock' ? (
        <p className={styles.mockHint}>
          Chế độ demo, chưa nối API. Mật khẩu bất kỳ, tài khoản:{' '}
          {Object.entries(MOCK_ACCOUNTS)
            .map(([username, account]) => `${username} (${ROLE_LABELS[account.role]})`)
            .join(' · ')}
        </p>
      ) : null}
    </Panel>
  )
}
