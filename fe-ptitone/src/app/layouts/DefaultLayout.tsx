import { Outlet } from 'react-router-dom'

import { Footer } from './Footer'
import { Header } from './Header'

import styles from './DefaultLayout.module.scss'

/**
 * Khung trang mặc định: header + vùng nội dung (outlet) + footer.
 * Dùng làm layout route trong `app/router`.
 */
export function DefaultLayout() {
  return (
    <div className={styles.shell}>
      <Header />
      <main className={styles.main}>
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
