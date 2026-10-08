import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'

import { API_MODE, ApiError } from '@/shared/api'
import { useAsyncData } from '@/shared/lib'
import { Icon } from '@/shared/ui'

import { changeEmail, fetchEmail, resendEmailCode, verifyEmail } from '../api/credentialApi'
import { MA_DEMO } from '../api/mockCredentialStore'
import { useAuth } from '../model/AuthContext'
import type { AccountEmail } from '../model/types'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/**
 * Thêm email cá nhân và xác minh bằng mã 6 số.
 *
 * Đây là màn **tự phục vụ** thay cho việc Admin gõ hộ email lúc cấp hồ sơ: chỉ
 * người đang đăng nhập mới đặt được email của mình, và mã chỉ tới đúng địa chỉ
 * vừa nhập — nên xác minh được là đã chứng minh sở hữu hòm thư đó.
 *
 * Xác minh xong mới có hai thứ: tự khôi phục mật khẩu khi quên, và sửa được
 * phần lý lịch trong hồ sơ.
 */
export function EmailPanel() {
  const { reloadUser } = useAuth()
  const email = useAsyncData<AccountEmail>(useCallback(() => fetchEmail(), []))

  const [diaChi, setDiaChi] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [ma, setMa] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const hienTai = email.data

  async function run(action: () => Promise<void>) {
    setPending(true)
    setError(null)
    setNotice(null)
    try {
      await action()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : NETWORK)
    } finally {
      setPending(false)
    }
  }

  function handleChangeEmail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run(async () => {
      await changeEmail(diaChi.trim(), matKhau)
      setMatKhau('')
      setDiaChi('')
      setNotice(`Đã gửi mã 6 số tới ${diaChi.trim()}. Mã có hạn 10 phút.`)
      email.reload()
      /* Đổi email là MẤT trạng thái đã xác minh — phiên phải biết ngay, nếu
         không nút sửa hồ sơ vẫn mở trong khi server đã từ chối. */
      await reloadUser()
    })
  }

  function handleVerify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run(async () => {
      await verifyEmail(ma.trim())
      setMa('')
      setNotice('Đã xác minh email. Giờ bạn tự khôi phục mật khẩu và sửa hồ sơ được.')
      email.reload()
      await reloadUser()
    })
  }

  function handleResend() {
    void run(async () => {
      await resendEmailCode()
      setNotice('Đã gửi lại mã. Mã trước đó hết hiệu lực.')
    })
  }

  if (email.loading) return <p>Đang tải thông tin email…</p>
  if (email.error) return <p role="alert">{email.error}</p>

  return (
    <>
      <p className={styles.status}>
        <span>Email hiện tại:</span>
        <b>{hienTai?.email ?? 'chưa có'}</b>
        {hienTai?.email ? (
          <span className={`${styles.badge} ${hienTai.daXacMinh ? styles.on : styles.off}`}>
            {hienTai.daXacMinh ? 'Đã xác minh' : 'Chờ xác minh'}
          </span>
        ) : null}
      </p>

      {hienTai?.daXacMinh ? null : (
        <p className={styles.hint}>
          Chưa xác minh thì <b>không tự khôi phục mật khẩu được</b> — mã khôi phục chỉ
          gửi tới địa chỉ đã xác minh. Bạn cũng chưa sửa được phần lý lịch trong hồ sơ.
        </p>
      )}

      <form className={styles.form} onSubmit={handleChangeEmail}>
        <p className={styles.stepTitle}>
          {hienTai?.email ? 'Đổi email' : 'Thêm email cá nhân'}
        </p>

        <label className={styles.field}>
          Email
          <input
            className={styles.input}
            type="email"
            value={diaChi}
            onChange={(event) => setDiaChi(event.target.value)}
            placeholder="vidu@gmail.com"
            autoComplete="email"
            maxLength={254}
            disabled={pending}
            required
          />
        </label>

        <label className={styles.field}>
          Mật khẩu hiện tại
          <input
            className={styles.input}
            type="password"
            value={matKhau}
            onChange={(event) => setMatKhau(event.target.value)}
            autoComplete="current-password"
            disabled={pending}
            required
          />
        </label>

        {/* Nói trước hệ quả: người dùng đang có email đã xác minh mà đổi sẽ mất
            trạng thái đó, và đó là lúc dễ tự khoá mình ra ngoài nhất. */}
        <p className={styles.hint}>
          Cần mật khẩu hiện tại để người khác mượn máy không đổi được email khôi phục.
          {hienTai?.daXacMinh ? ' Đổi email thì phải xác minh lại từ đầu.' : ''}
        </p>

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={pending}>
            <Icon name="bell" size="15px" />
            {pending ? 'Đang gửi…' : 'Lưu và gửi mã'}
          </button>
        </div>
      </form>

      {hienTai?.email && !hienTai.daXacMinh ? (
        <form className={`${styles.form} ${styles.step}`} onSubmit={handleVerify}>
          <p className={styles.stepTitle}>Nhập mã xác minh</p>

          <label className={styles.field}>
            Mã 6 số trong thư
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

          <div className={styles.actions}>
            <button className={styles.primary} type="submit" disabled={pending}>
              <Icon name="check" size="15px" />
              {pending ? 'Đang xác minh…' : 'Xác minh'}
            </button>
            <button
              className={styles.ghost}
              type="button"
              onClick={handleResend}
              disabled={pending}
            >
              Gửi lại mã
            </button>
          </div>

          {API_MODE === 'mock' ? (
            <p className={styles.hint}>Chế độ demo: mã xác minh luôn là {MA_DEMO}.</p>
          ) : null}
        </form>
      ) : null}

      {notice ? (
        <p className={`${styles.message} ${styles.success}`} role="status">
          {notice}
        </p>
      ) : null}
      {error ? (
        <p className={`${styles.message} ${styles.error}`} role="alert">
          {error}
        </p>
      ) : null}
    </>
  )
}
