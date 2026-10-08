import { Panel } from '@/shared/ui'

import { ActivateForm, ChangePasswordForm, EmailPanel, ForgotPasswordForm } from '@/features/auth'

import styles from './CredentialPages.module.scss'

/**
 * Bốn màn A1 — thông tin đăng nhập của chính mình.
 *
 * Gom một file vì cả bốn chỉ là một `Panel` bọc một biểu mẫu của feature
 * `auth`: tách thành bốn file trang gần như trống thì khó theo dõi hơn là dễ.
 * Có màn nào cần bố cục riêng thì tách màn đó ra.
 *
 * `/kich-hoat` và `/quen-mat-khau` **không** cần đăng nhập — người dùng chưa
 * có mật khẩu hoặc đã quên thì không thể đăng nhập trước.
 */

/** `/kich-hoat` — đổi mã Admin cấp lấy mật khẩu do mình đặt. */
export function ActivatePage() {
  return (
    <div className={styles.page}>
      <Panel title="KÍCH HOẠT TÀI KHOẢN" icon="lock">
        <p className={styles.lead}>
          Tài khoản vừa được cấp <b>chưa có mật khẩu</b>. Nhập mã kích hoạt Phòng Đào
          tạo đưa cho bạn để tự đặt mật khẩu lần đầu.
        </p>
        <ActivateForm />
      </Panel>
    </div>
  )
}

/** `/quen-mat-khau` — xin mã khôi phục rồi đặt lại mật khẩu. */
export function ForgotPasswordPage() {
  return (
    <div className={styles.page}>
      <Panel title="QUÊN MẬT KHẨU" icon="lock">
        <ForgotPasswordForm />
      </Panel>
    </div>
  )
}

/** `/tai-khoan/email` — thêm email cá nhân và xác minh. */
export function EmailPage() {
  return (
    <div className={styles.page}>
      <Panel title="EMAIL VÀ XÁC MINH" icon="bell">
        <EmailPanel />
      </Panel>
    </div>
  )
}

/** `/tai-khoan/doi-mat-khau`. */
export function ChangePasswordPage() {
  return (
    <div className={styles.page}>
      <Panel title="ĐỔI MẬT KHẨU" icon="lock">
        <ChangePasswordForm />
      </Panel>
    </div>
  )
}
