import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { API_MODE, ApiError } from '@/shared/api'
import { ROUTES } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import { activate, resendActivation } from '../api/credentialApi'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/**
 * Kích hoạt lần đầu: đổi mã Admin Master cấp lấy mật khẩu **do mình đặt**.
 *
 * Admin không bao giờ biết mật khẩu này — đó là lý do hệ thống phát mã kích
 * hoạt chứ không phát mật khẩu. Mã đi tới sinh viên qua kênh ngoài (số điện
 * thoại, trao tay) khi cấp hồ sơ không kèm email, hoặc qua thư khi có email.
 *
 * Kích hoạt xong **không** tự đăng nhập: người dùng gõ lại mật khẩu vừa đặt ở
 * màn đăng nhập, nên mật khẩu được xác nhận ngay lúc còn nhớ.
 */
export function ActivateForm() {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [maKichHoat, setMaKichHoat] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [sent, setSent] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (matKhau !== nhapLai) {
      // Kiểm ở client vì server không nhận ô "nhập lại" — nó chỉ có một mật khẩu.
      setError('Hai lần nhập mật khẩu không giống nhau.')
      return
    }
    setPending(true)
    setError(null)
    try {
      await activate(tenDangNhap.trim(), maKichHoat.trim(), matKhau)
      setDone(true)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
    } finally {
      setPending(false)
    }
  }

  async function handleResend() {
    setPending(true)
    setError(null)
    try {
      await resendActivation(tenDangNhap.trim())
      setSent(true)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
    } finally {
      setPending(false)
    }
  }

  if (done) {
    return (
      <div className={styles.form}>
        <p className={`${styles.message} ${styles.success}`} role="status">
          Đã đặt mật khẩu cho tài khoản <b>{tenDangNhap.trim()}</b>.
        </p>
        <p className={styles.hint}>
          Đăng nhập bằng mật khẩu vừa đặt. Sau khi vào, hãy thêm email cá nhân và xác
          minh để tự khôi phục được mật khẩu khi quên.
        </p>
        <div className={styles.actions}>
          <Link className={styles.primary} to={ROUTES.login}>
            <Icon name="signIn" size="15px" />
            Tới trang đăng nhập
          </Link>
        </div>
      </div>
    )
  }

  return (
    <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
      <label className={styles.field}>
        Tên đăng nhập
        <input
          className={styles.input}
          value={tenDangNhap}
          onChange={(event) => setTenDangNhap(event.target.value)}
          placeholder="Mã sinh viên hoặc mã giảng viên"
          autoComplete="username"
          disabled={pending}
          required
        />
      </label>

      <label className={styles.field}>
        Mã kích hoạt
        <input
          className={styles.input}
          value={maKichHoat}
          onChange={(event) => setMaKichHoat(event.target.value)}
          placeholder="XXXX-XXXX-XXXX-XXXX"
          autoComplete="one-time-code"
          disabled={pending}
          required
        />
      </label>

      <label className={styles.field}>
        Mật khẩu mới
        <input
          className={styles.input}
          type="password"
          value={matKhau}
          onChange={(event) => setMatKhau(event.target.value)}
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
        Mật khẩu <b>không được chứa tên đăng nhập</b>. Quản trị không biết mật khẩu bạn
        đặt ở đây.
      </p>

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={pending}>
          <Icon name="lock" size="15px" />
          {pending ? 'Đang kích hoạt…' : 'Kích hoạt tài khoản'}
        </button>
        {/* Chỉ có tác dụng khi Admin đã lưu email lúc cấp hồ sơ; không có email
            thì server im lặng bỏ qua, nên lời nhắn dưới nói đúng cả hai trường hợp. */}
        <button
          className={styles.ghost}
          type="button"
          onClick={() => void handleResend()}
          disabled={pending || tenDangNhap.trim().length === 0}
        >
          Gửi lại mã qua email
        </button>
      </div>

      {sent ? (
        <p className={styles.hint} role="status">
          Nếu tài khoản có email đã lưu và chưa kích hoạt, mã mới đã được gửi tới đó.
          Mã cũ hết hiệu lực. Không nhận được thư thì liên hệ Phòng Đào tạo để lấy mã
          trao tay.
        </p>
      ) : null}

      {error ? (
        <p className={`${styles.message} ${styles.error}`} role="alert">
          {error}
        </p>
      ) : null}

      {API_MODE === 'mock' ? (
        <p className={styles.hint}>
          Chế độ demo: nhập tên đăng nhập có thật trong danh sách demo và mã bất kỳ từ
          4 ký tự.
        </p>
      ) : null}
    </form>
  )
}
