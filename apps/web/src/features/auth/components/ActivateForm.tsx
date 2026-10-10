import { useState } from 'react'
import type { FormEvent } from 'react'

import { API_MODE, ApiError } from '@/shared/api'
import { Icon } from '@/shared/ui'

import { activate, resendActivation } from '../api/credentialApi'
import { AuthConfirmDialog } from './AuthDialog'
import dialogStyles from './AuthDialog.module.scss'
import { PasswordField } from './PasswordField'
import styles from './AuthForms.module.scss'

const NETWORK = 'Không kết nối được máy chủ. Vui lòng thử lại.'

/** Kết quả lần kích hoạt gần nhất — một box duy nhất, thành công hoặc thất bại. */
type Result = { ok: true; username: string } | { ok: false; message: string }

/**
 * Kích hoạt lần đầu: đổi mã Admin Master cấp lấy mật khẩu **do mình đặt**.
 *
 * Luồng giữ nguyên một giao diện: nhập → xác nhận trong dialog → box kết quả
 * hiện ngay dưới nút bấm (xanh khi thành công, đỏ khi thất bại). Form vẫn ở đó;
 * đường về đăng nhập chỉ có một — nút viên thuốc ở chân trang.
 *
 * Admin không bao giờ biết mật khẩu này — đó là lý do hệ thống phát mã kích
 * hoạt chứ không phát mật khẩu. Mã đi tới sinh viên qua kênh ngoài (số điện
 * thoại, trao tay) khi cấp hồ sơ không kèm email, hoặc qua thư khi có email.
 *
 * Kích hoạt xong **không** tự đăng nhập: người dùng gõ lại mật khẩu vừa đặt ở
 * màn đăng nhập, nên mật khẩu được xác nhận ngay lúc còn nhớ.
 */
export function ActivateForm({ onCompleted }: { onCompleted?: () => void }) {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [maKichHoat, setMaKichHoat] = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [pending, setPending] = useState(false)
  const [result, setResult] = useState<Result | null>(null)
  const [sent, setSent] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const succeeded = result?.ok === true
  const locked = pending || succeeded

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (matKhau !== nhapLai) {
      // Kiểm ở client vì server không nhận ô "nhập lại" — nó chỉ có một mật khẩu.
      setResult({ ok: false, message: 'Hai lần nhập mật khẩu không giống nhau.' })
      return
    }
    /* Mã kích hoạt dùng một lần và sai 5 lần thì bị thu hồi — cho xem lại
       thông tin trước khi gửi để tránh gõ nhầm tên đăng nhập rồi mất lượt. */
    setResult(null)
    setConfirmOpen(true)
  }

  async function doActivate() {
    setPending(true)
    setResult(null)
    try {
      await activate(tenDangNhap.trim(), maKichHoat.trim(), matKhau)
      setConfirmOpen(false)
      setResult({ ok: true, username: tenDangNhap.trim() })
      onCompleted?.()
    } catch (cause) {
      setConfirmOpen(false)
      setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
    } finally {
      setPending(false)
    }
  }

  async function handleResend() {
    setPending(true)
    setResult(null)
    try {
      await resendActivation(tenDangNhap.trim())
      setSent(true)
    } catch (cause) {
      setResult({ ok: false, message: cause instanceof ApiError ? cause.message : NETWORK })
    } finally {
      setPending(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
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
        Mã kích hoạt
        <input
          className={styles.input}
          value={maKichHoat}
          onChange={(event) => setMaKichHoat(event.target.value)}
          placeholder="XXXX-XXXX-XXXX-XXXX"
          autoComplete="one-time-code"
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

      <p className={styles.hint}>
        Mật khẩu <b>không được chứa tên đăng nhập</b>. Quản trị không biết mật khẩu bạn
        đặt ở đây.
      </p>

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={locked}>
          <Icon name="lock" size="15px" />
          {pending ? 'Đang kích hoạt…' : succeeded ? 'Đã kích hoạt' : 'Kích hoạt tài khoản'}
        </button>
        {/* Chỉ có tác dụng khi Admin đã lưu email lúc cấp hồ sơ; không có email
            thì server im lặng bỏ qua, nên lời nhắn dưới nói đúng cả hai trường hợp. */}
        <button
          className={styles.ghost}
          type="button"
          onClick={() => void handleResend()}
          disabled={locked || tenDangNhap.trim().length === 0}
        >
          Gửi lại mã qua email
        </button>
      </div>

      {sent && !succeeded ? (
        <p className={styles.hintBox} role="status">
          Nếu tài khoản có email đã lưu và chưa kích hoạt, mã mới đã được gửi tới đó.
          Mã cũ hết hiệu lực. Không nhận được thư thì liên hệ Phòng Đào tạo để lấy mã
          trao tay.
        </p>
      ) : null}

      {result?.ok ? (
        <p className={`${styles.message} ${styles.success}`} role="status">
          <span className={styles.successIcon}>
            <Icon name="check" size="15px" />
          </span>
          <span>
            Đã đặt mật khẩu cho tài khoản <b>{result.username}</b>. Đăng nhập bằng mật
            khẩu vừa đặt ở nút dưới chân trang.
          </span>
        </p>
      ) : null}

      {result && !result.ok ? (
        <p key={result.message} className={`${styles.message} ${styles.error}`} role="alert">
          {result.message}
        </p>
      ) : null}

      {API_MODE === 'mock' ? (
        <p className={styles.hint}>
          Chế độ demo: nhập tên đăng nhập có thật trong danh sách demo và mã bất kỳ từ
          4 ký tự.
        </p>
      ) : null}

      <AuthConfirmDialog
        open={confirmOpen}
        title={`Kích hoạt tài khoản ${tenDangNhap.trim() || '…'}`}
        confirmLabel="Xác nhận kích hoạt"
        pending={pending}
        onClose={() => {
          if (!pending) setConfirmOpen(false)
        }}
        onConfirm={() => void doActivate()}
      >
        <p className={dialogStyles.account}>
          Tên đăng nhập: <b>{tenDangNhap.trim() || '—'}</b>
        </p>
        <p>
          Mã kích hoạt <b>chỉ dùng một lần</b> — kích hoạt xong là hết hiệu lực. Nhập sai
          5 lần thì mã bị thu hồi và phải nhờ Phòng Đào tạo cấp lại.
        </p>
        <p className={dialogStyles.warn}>
          Kiểm tra kỹ tên đăng nhập và mã trước khi xác nhận. Kích hoạt xong, hãy đăng
          nhập bằng mật khẩu vừa đặt rồi thêm email để tự khôi phục được khi quên.
        </p>
      </AuthConfirmDialog>
    </form>
  )
}
