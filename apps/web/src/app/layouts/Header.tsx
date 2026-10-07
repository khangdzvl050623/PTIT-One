import { useEffect, useRef } from 'react'
import { Link, NavLink } from 'react-router-dom'

import { navItemsFor } from '@/app/router/navigation'
import { useAuth } from '@/features/auth'
import { SITE } from '@/shared/config'
import { LABELS, ROUTES } from '@/shared/constants'
import { Logo } from '@/shared/ui'

import styles from './Header.module.scss'

export interface HeaderProps {
  siteName?: string
  homeHref?: string
  homeLabel?: string
}

export function Header({
  siteName = SITE.siteName,
  homeHref = SITE.homeHref,
  homeLabel = LABELS.home,
}: HeaderProps) {
  const { status, user, signOut } = useAuth()
  const ref = useRef<HTMLElement>(null)

  /* Đưa chiều cao thật của header vào `--header-h` để cột dính bên dưới biết
     chừa bao nhiêu — header xuống hai dòng khi màn hẹp hoặc menu dài. */
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const root = document.documentElement
    const observer = new ResizeObserver(() => {
      root.style.setProperty('--header-h', `${el.offsetHeight}px`)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  /* Menu lấy từ cùng danh sách sinh ra tuyến, nên không thể hiện một mục mà
     router không có. Chưa đăng nhập thì rỗng. */
  const items = navItemsFor(user?.role)

  return (
    <header ref={ref} className={styles.topBar} data-print="hide">
      <div className={styles.inner}>
        <Link to={homeHref} className={styles.brand}>
          <Logo size="sm" variant="tile" />
          <span className={styles.siteName}>{siteName}</span>
        </Link>

        <nav className={styles.nav}>
          <NavLink to={homeHref} end className={navLinkClass}>
            {homeLabel}
          </NavLink>

          {status === 'authenticated' ? (
            <NavLink to={ROUTES.userInfo} className={navLinkClass}>
              {LABELS.userInfo}
            </NavLink>
          ) : null}

          {items.map((item) => (
            <NavLink key={item.path} to={item.path} className={navLinkClass}>
              {item.label}
            </NavLink>
          ))}

          {status === 'authenticated' && user ? (
            <span className={styles.userArea}>
              <NavLink to={ROUTES.account} className={navLinkClass}>
                {user.username}
              </NavLink>
              <button className={styles.signOut} type="button" onClick={() => void signOut()}>
                {LABELS.logout}
              </button>
            </span>
          ) : null}

          {status === 'anonymous' ? (
            <NavLink to={ROUTES.login} className={navLinkClass}>
              {LABELS.loginSubmit}
            </NavLink>
          ) : null}
        </nav>
      </div>
    </header>
  )
}

function navLinkClass({ isActive }: { isActive: boolean }): string {
  return isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
}
