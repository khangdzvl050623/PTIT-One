import { Outlet } from 'react-router-dom'

import { EmailNotice } from '@/features/auth'

import { Footer } from './Footer'
import { Header } from './Header'

import styles from './DefaultLayout.module.scss'

/**
 * Khung trang mặc định: header + vùng nội dung (outlet) + footer.
 * Dùng làm layout route trong `app/router`.
 *
 * `EmailNotice` đặt ở đây để lời nhắc xác minh email theo người dùng qua mọi
 * trang; nó tự ẩn khi đã xác minh hoặc chưa đăng nhập.
 */
export function DefaultLayout() {
  return (
    <div className={styles.shell}>
      <Header />
      <main className={styles.main}>
        <EmailNotice />
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
