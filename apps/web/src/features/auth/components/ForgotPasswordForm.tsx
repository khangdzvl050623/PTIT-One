import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { API_MODE, ApiError } from '@/shared/api'
import { ROUTES } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import { forgotPassword, resetPassword } from '../api/credentialApi'
import { MA_DEMO } from '../api/mockCredentialStore'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/**
 * Quên mật khẩu, hai bước trên cùng một màn.
 *
 * Bước 1 **luôn báo thành công**, kể cả khi tài khoản không có, email gõ sai
 * hay chưa xác minh — nếu phân biệt được ba trường hợp đó thì form này thành
 * công cụ dò xem tài khoản nào có thật. Vì vậy lời nhắn phải viết theo kiểu
 * "nếu thông tin đúng thì…", đừng hứa chắc là đã gửi.
 *
 * Email gõ ở bước 1 chỉ để **đối chiếu**; mã bay tới địa chỉ đã xác minh của
 * tài khoản, không tới địa chỉ vừa gõ.
 */
export function ForgotPasswordForm() {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [email, setEmail] = useState('')
  const [ma, setMa] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [daGui, setDaGui] = useState(false)
  const [xong, setXong] = useState(false)

  async function run(action: () => Promise<void>) {
    setPending(true)
    setError(null)
    try {
      await action()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
    } finally {
      setPending(false)
    }
  }

  function handleRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run(async () => {
      await forgotPassword(tenDangNhap.trim(), email.trim())
      setDaGui(true)
    })
  }

  function handleReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (matKhau !== nhapLai) {
      setError('Hai lần nhập mật khẩu mới không giống nhau.')
      return
    }
    void run(async () => {
      await resetPassword(tenDangNhap.trim(), ma.trim(), matKhau)
      setXong(true)
    })
  }

  if (xong) {
    return (
      <div className={styles.form}>
        <p className={`${styles.message} ${styles.success}`} role="status">
          Đã đặt lại mật khẩu cho <b>{tenDangNhap.trim()}</b>.
        </p>
        <p className={styles.hint}>
          Mọi phiên đang mở đã bị thu hồi. Đăng nhập lại bằng mật khẩu mới.
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
    <>
      <form className={styles.form} onSubmit={handleRequest}>
        <p className={styles.stepTitle}>Bước 1 — xin mã khôi phục</p>

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
          Email đã đăng ký
          <input
            className={styles.input}
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="vidu@gmail.com"
            autoComplete="email"
            disabled={pending}
            required
          />
        </label>

        <p className={styles.hint}>
          Email ở đây chỉ để <b>đối chiếu</b>. Mã được gửi tới địa chỉ đã xác minh của
          tài khoản, không gửi tới địa chỉ vừa gõ.
        </p>

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={pending}>
            <Icon name="bell" size="15px" />
            {pending ? 'Đang gửi…' : 'Gửi mã khôi phục'}
          </button>
        </div>

        {daGui ? (
          <p className={styles.hint} role="status">
            Nếu thông tin khớp một tài khoản có email đã xác minh, mã 6 số đã được gửi
            tới địa chỉ đó. Mã có hạn 10 phút. Chưa từng xác minh email thì liên hệ
            Phòng Đào tạo để được cấp lại.
          </p>
        ) : null}
      </form>

      <form className={`${styles.form} ${styles.step}`} onSubmit={handleReset}>
        <p className={styles.stepTitle}>Bước 2 — đặt mật khẩu mới</p>

        <label className={styles.field}>
          Mã khôi phục
          <input
            className={`${styles.input} ${styles.code}`}
            value={ma}
            onChange={(event) => setMa(event.target.value)}
            inputMode="numeric"
            pattern="[0-9]{6}"
            maxLength={6}
            autoComplete="one-time-code"
            placeholder="000000"
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

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={pending}>
            <Icon name="lock" size="15px" />
            {pending ? 'Đang đặt lại…' : 'Đặt lại mật khẩu'}
          </button>
        </div>

        {API_MODE === 'mock' ? (
          <p className={styles.hint}>Chế độ demo: mã khôi phục luôn là {MA_DEMO}.</p>
        ) : null}
      </form>

      {error ? (
        <p className={`${styles.message} ${styles.error}`} role="alert">
          {error}
        </p>
      ) : null}
    </>
  )
}
