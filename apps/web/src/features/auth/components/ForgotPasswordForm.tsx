import { useState } from 'react'
import type { FormEvent } from 'react'

import { API_MODE, ApiError } from '@/shared/api'
import { Icon } from '@/shared/ui'

import { forgotPassword, resetPassword } from '../api/credentialApi'
import { MA_DEMO } from '../api/mockCredentialStore'
import { AuthConfirmDialog, AuthInfoDialog } from './AuthDialog'
import dialogStyles from './AuthDialog.module.scss'
import { PasswordField } from './PasswordField'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/** Kết quả lần đặt lại gần nhất — một box duy nhất, thành công hoặc thất bại. */
type Result = { ok: true; username: string } | { ok: false; message: string }

/**
 * Quên mật khẩu, hai bước trên cùng một màn — và vẫn một màn sau khi xong.
 *
 * Bước 1 **luôn báo thành công**, kể cả khi tài khoản không có, email gõ sai
 * hay chưa xác minh — nếu phân biệt được ba trường hợp đó thì form này thành
 * công cụ dò xem tài khoản nào có thật. Vì vậy lời nhắn phải viết theo kiểu
 * "nếu thông tin đúng thì…", đừng hứa chắc là đã gửi.
 *
 * Email gõ ở bước 1 chỉ để **đối chiếu**; mã bay tới địa chỉ đã xác minh của
 * tài khoản, không tới địa chỉ vừa gõ.
 *
 * Đặt lại xong, box xanh hiện ngay dưới nút bấm, form tự khoá; đường về đăng
 * nhập chỉ có một — nút viên thuốc ở chân trang.
 */
export function ForgotPasswordForm() {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [email, setEmail] = useState('')
  const [ma, setMa] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [pending, setPending] = useState(false)
  const [result, setResult] = useState<Result | null>(null)
  const [daGui, setDaGui] = useState(false)
  const [thongBaoMa, setThongBaoMa] = useState(false)
  const [xacNhanDatLai, setXacNhanDatLai] = useState(false)

  const succeeded = result?.ok === true
  const locked = pending || succeeded

  async function run(action: () => Promise<void>) {
    setPending(true)
    setResult(null)
    try {
      await action()
    } catch (cause) {
      setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
    } finally {
      setPending(false)
    }
  }

  function handleRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void run(async () => {
      await forgotPassword(tenDangNhap.trim(), email.trim())
      setDaGui(true)
      /* Bước 1 luôn báo thành công để không lộ tài khoản nào có thật — hộp
         thông báo nhắc kiểm tra thư thay vì hứa chắc "đã gửi tới bạn". */
      setThongBaoMa(true)
    })
  }

  function handleReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (matKhau !== nhapLai) {
      setResult({ ok: false, message: 'Hai lần nhập mật khẩu mới không giống nhau.' })
      return
    }
    /* Đặt lại sẽ thu hồi mọi phiên đang mở — cho xác nhận lại tên đăng nhập
       và hệ quả trước khi gửi mã 6 số dùng một lần đi. */
    setResult(null)
    setXacNhanDatLai(true)
  }

  async function doReset() {
    setPending(true)
    setResult(null)
    try {
      await resetPassword(tenDangNhap.trim(), ma.trim(), matKhau)
      setXacNhanDatLai(false)
      setResult({ ok: true, username: tenDangNhap.trim() })
    } catch (cause) {
      setXacNhanDatLai(false)
      setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
    } finally {
      setPending(false)
    }
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
            disabled={locked}
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
            disabled={locked}
            required
          />
        </label>

        <p className={styles.hint}>
          Email ở đây chỉ để <b>đối chiếu</b>. Mã được gửi tới địa chỉ đã xác minh của
          tài khoản, không gửi tới địa chỉ vừa gõ.
        </p>

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={locked}>
            <Icon name="bell" size="15px" />
            {pending ? 'Đang gửi…' : 'Gửi mã khôi phục'}
          </button>
        </div>

        {daGui && !succeeded ? (
          <p className={styles.hintBox} role="status">
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
            disabled={locked}
            required
          />
        </label>

        <PasswordField
          label="Mật khẩu mới"
          value={matKhau}
          onChange={setMatKhau}
          placeholder="Ít nhất 8 ký tự"
          autoComplete="new-password"
          minLength={8}
          maxLength={128}
          disabled={locked}
        />

        <PasswordField
          label="Nhập lại mật khẩu mới"
          value={nhapLai}
          onChange={setNhapLai}
          autoComplete="new-password"
          disabled={locked}
        />

        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={locked}>
            <Icon name="lock" size="15px" />
            {pending ? 'Đang đặt lại…' : succeeded ? 'Đã đặt lại' : 'Đặt lại mật khẩu'}
          </button>
        </div>

        {API_MODE === 'mock' ? (
          <p className={styles.hint}>Chế độ demo: mã khôi phục luôn là {MA_DEMO}.</p>
        ) : null}
      </form>

      {result?.ok ? (
        <p className={`${styles.message} ${styles.success}`} role="status">
          <span className={styles.successIcon}>
            <Icon name="check" size="15px" />
          </span>
          <span>
            Đã đặt lại mật khẩu cho <b>{result.username}</b>. Mọi phiên đang mở đã bị
            thu hồi — đăng nhập lại bằng mật khẩu mới ở nút dưới chân trang.
          </span>
        </p>
      ) : null}

      {result && !result.ok ? (
        <p key={result.message} className={`${styles.message} ${styles.error}`} role="alert">
          {result.message}
        </p>
      ) : null}

      <AuthInfoDialog
        open={thongBaoMa}
        title="Kiểm tra hộp thư của bạn"
        actionLabel="Đã hiểu, nhập mã ở bước 2"
        onClose={() => setThongBaoMa(false)}
      >
        <p>
          Nếu thông tin khớp một tài khoản có email đã xác minh, mã 6 số đã được gửi
          tới địa chỉ đó và có hạn <b>10 phút</b>.
        </p>
        <p>
          Không thấy thư? Kiểm tra cả <b>thư rác (spam)</b>. Chưa từng xác minh email
          thì liên hệ Phòng Đào tạo để được hỗ trợ.
        </p>
      </AuthInfoDialog>

      <AuthConfirmDialog
        open={xacNhanDatLai}
        title={`Đặt lại mật khẩu cho ${tenDangNhap.trim() || '…'}`}
        confirmLabel="Xác nhận đặt lại"
        danger
        pending={pending}
        onClose={() => {
          if (!pending) setXacNhanDatLai(false)
        }}
        onConfirm={() => void doReset()}
      >
        <p className={dialogStyles.account}>
          Tên đăng nhập: <b>{tenDangNhap.trim() || '—'}</b>
        </p>
        <p>
          Mã khôi phục <b>chỉ dùng một lần, hạn 10 phút</b>. Đặt lại xong, mọi phiên
          đang mở trên mọi thiết bị sẽ <b>bị đăng xuất</b> và phải vào lại bằng mật
          khẩu mới.
        </p>
        <p className={dialogStyles.warn}>
          Kiểm tra kỹ mã 6 số và hai ô mật khẩu mới khớp nhau trước khi xác nhận.
        </p>
      </AuthConfirmDialog>
    </>
  )
}
