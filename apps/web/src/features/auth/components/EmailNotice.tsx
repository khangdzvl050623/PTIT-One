import { Link, useLocation } from 'react-router-dom'

import { ROUTES } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import { useAuth } from '../model/AuthContext'
import styles from './EmailNotice.module.scss'

/**
 * Dải nhắc xác minh email, hiện trên mọi trang khi đã đăng nhập mà
 * `emailDaXacMinh` còn `false`.
 *
 * Chỉ **nhắc**, không chặn (chốt 08/10/2026): sinh viên vẫn xem lịch và điểm
 * được. Nhưng cái mất khi bỏ qua là có thật nên phải nói ra — không có email
 * đã xác minh thì quên mật khẩu phải nhờ Phòng Đào tạo cấp lại mã, và chưa sửa
 * được phần lý lịch trong hồ sơ.
 *
 * Tự ẩn ở chính màn email để không nhắc người đang làm đúng việc được nhắc.
 */
export function EmailNotice() {
  const { user } = useAuth()
  const { pathname } = useLocation()

  if (!user || user.emailDaXacMinh || pathname === ROUTES.email) return null

  return (
    <div className={styles.notice} role="status">
      <Icon name="bell" size="16px" className={styles.icon} />
      <p className={styles.text}>
        {user.email
          ? 'Email của bạn chưa được xác minh.'
          : 'Tài khoản chưa có email cá nhân.'}{' '}
        Chưa xác minh thì không tự khôi phục được mật khẩu khi quên, và chưa sửa được
        hồ sơ.
      </p>
      <Link className={styles.action} to={ROUTES.email}>
        {user.email ? 'Xác minh ngay' : 'Thêm email'}
      </Link>
    </div>
  )
}
