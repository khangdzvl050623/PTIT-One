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

const NETWORK = 'Không kết nối được máy chủ. Thử lại sau.'

/**
 * Thêm email cá nhân và xác minh bằng mã 6 số.
 *
 * Màn này có **ba trạng thái**, và việc chính của mỗi trạng thái khác nhau:
 *
 * | Trạng thái | Việc chính | Việc phụ |
 * |---|---|---|
 * | chưa có email | thêm email | — |
 * | có email, chưa xác minh | **nhập mã** | dùng email khác |
 * | đã xác minh | đổi email | — |
 *
 * Bảng này là lý do component dài hơn một biểu mẫu thường. Bản đầu tiên đặt
 * "Đổi email" lên trên trong mọi trạng thái, nên người đang có email chờ xác
 * minh — ví dụ vừa được Admin đặt hộ — nhìn thấy dòng "Email hiện tại: X" rồi
 * ngay dưới là một ô email trống đòi nhập lại. Không ai đoán được rằng việc
 * cần làm là bấm "Gửi lại mã" ở tận cuối trang.
 */
export function EmailPanel() {
  const { reloadUser } = useAuth()
  const email = useAsyncData<AccountEmail>(useCallback(() => fetchEmail(), []))

  const [diaChi, setDiaChi] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [ma, setMa] = useState('')
  const [doiEmail, setDoiEmail] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const hienTai = email.data
  const choXacMinh = Boolean(hienTai?.email) && !hienTai?.daXacMinh

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
      const moi = diaChi.trim()
      await changeEmail(moi, matKhau)
      setMatKhau('')
      setDiaChi('')
      setDoiEmail(false)
      setNotice(`Đã gửi mã 6 số tới ${moi}. Mã có hạn 10 phút.`)
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
      setNotice(`Đã gửi lại mã tới ${hienTai?.email}. Mã trước đó hết hiệu lực.`)
    })
  }

  if (email.loading) return <p>Đang tải thông tin email…</p>
  if (email.error) return <p role="alert">{email.error}</p>

  /** Ô nhập email + mật khẩu hiện tại. Dùng ở cả "thêm" lẫn "đổi". */
  const bieuMauEmail = (
    <form className={styles.form} onSubmit={handleChangeEmail}>
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

      <p className={styles.hint}>
        Cần mật khẩu hiện tại để người khác mượn máy không đổi được email khôi phục.
        {hienTai?.daXacMinh ? ' Đổi email thì phải xác minh lại từ đầu.' : ''}
      </p>

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={pending}>
          <Icon name="bell" size="15px" />
          {pending ? 'Đang gửi…' : 'Lưu và gửi mã'}
        </button>
        {choXacMinh ? (
          <button
            className={styles.ghost}
            type="button"
            onClick={() => setDoiEmail(false)}
            disabled={pending}
          >
            Thôi, quay lại
          </button>
        ) : null}
      </div>
    </form>
  )

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

      {/* Chờ xác minh: việc chính là NHẬP MÃ, không phải đổi email. */}
      {choXacMinh && !doiEmail ? (
        <>
          <form className={styles.form} onSubmit={handleVerify}>
            <p className={styles.stepTitle}>Xác minh {hienTai?.email}</p>
            <p className={styles.hint}>
              Mã 6 số đã được gửi tới địa chỉ này. Không thấy thư thì xem hộp spam, hoặc
              bấm <b>Gửi lại mã</b>.
            </p>

            <label className={styles.field}>
              Mã xác minh
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

          {/* Lối phụ: địa chỉ đang chờ là sai (gõ nhầm, hoặc Admin đặt hộ nhầm). */}
          <div className={styles.step}>
            <p className={styles.hint}>Địa chỉ trên không phải của bạn?</p>
            <div className={styles.actions}>
              <button
                className={styles.ghost}
                type="button"
                onClick={() => setDoiEmail(true)}
                disabled={pending}
              >
                Dùng email khác
              </button>
            </div>
          </div>
        </>
      ) : (
        <>
          <p className={styles.stepTitle}>
            {hienTai?.email ? 'Đổi email' : 'Thêm email cá nhân'}
          </p>
          {bieuMauEmail}
        </>
      )}

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
