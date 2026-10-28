import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Panel, Icon, type Flash } from '@/shared/ui'
import { ROUTES } from '@/shared/constants'

import { ActivateForm, ChangePasswordForm, EmailPanel, ForgotPasswordForm } from '@/features/auth'

import styles from './CredentialPages.module.scss'

/** Tin mang sang login khi đi từ chân trang kích hoạt — cố ý không tên riêng. */
const ACTIVATED_FLASH: Flash = {
  kind: 'success',
  text: 'Tài khoản vừa kích hoạt — đăng nhập bằng mật khẩu vừa đặt.',
}

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
  /* Chỉ mang flash sang login khi đã kích hoạt thật — chưa nhập gì mà báo
     "vừa kích hoạt" là nói dối (bug 10/2026). */
  const [completed, setCompleted] = useState(false)

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Panel title="KÍCH HOẠT TÀI KHOẢN" icon="lock">
          <p className={styles.lead}>
            Tài khoản vừa được cấp <b>chưa có mật khẩu</b>. Nhập mã kích hoạt Phòng Đào
            tạo đưa cho bạn để tự đặt mật khẩu lần đầu.
          </p>
          <ActivateForm onCompleted={() => setCompleted(true)} />
          <p className={styles.backRow}>
            <span className={styles.backText}>Đã kích hoạt xong?</span>
            {/* Tin chung chung, không tên tài khoản: form không lộ state lên
                trang cha chỉ để cá nhân hoá một dòng chữ. */}
            <Link
              className={styles.backLink}
              to={ROUTES.login}
              state={completed ? { flash: ACTIVATED_FLASH } : undefined}
            >
              <Icon name="signIn" size="14px" />
              Về trang đăng nhập
            </Link>
          </p>
        </Panel>
      </div>
    </div>
  )
}

/** `/quen-mat-khau` — xin mã khôi phục rồi đặt lại mật khẩu. */
export function ForgotPasswordPage() {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Panel title="QUÊN MẬT KHẨU" icon="lock">
          <p className={styles.steps}>
            <span className={styles.stepDot}>1</span> Xin mã
            <span aria-hidden="true">→</span>
            <span className={styles.stepDot}>2</span> Đặt mật khẩu mới
          </p>
          <ForgotPasswordForm />
          <p className={styles.backRow}>
            <span className={styles.backText}>Nhớ lại mật khẩu?</span>
            <Link className={styles.backLink} to={ROUTES.login}>
              <Icon name="signIn" size="14px" />
              Về trang đăng nhập
            </Link>
          </p>
        </Panel>
      </div>
    </div>
  )
}

/** `/tai-khoan/email` — thêm email cá nhân và xác minh. */
export function EmailPage() {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Panel title="EMAIL VÀ XÁC MINH" icon="bell">
          <EmailPanel />
        </Panel>
      </div>
    </div>
  )
}

/** `/tai-khoan/doi-mat-khau`. */
export function ChangePasswordPage() {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Panel title="ĐỔI MẬT KHẨU" icon="lock">
          <ChangePasswordForm />
        </Panel>
      </div>
    </div>
  )
}
