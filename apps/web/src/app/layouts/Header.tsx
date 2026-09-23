import { Link } from 'react-router-dom'

import { SITE } from '@/shared/config'
import { LABELS } from '@/shared/constants'
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
  return (
    <header className={styles.topBar}>
      <div className={styles.inner}>
        <Link to={homeHref} className={styles.brand}>
          <Logo size="sm" variant="tile" />
          <span className={styles.siteName}>{siteName}</span>
        </Link>
        <nav className={styles.nav}>
          <Link to={homeHref} className={styles.navLink}>
            {homeLabel}
          </Link>
        </nav>
      </div>
    </header>
  )
}
