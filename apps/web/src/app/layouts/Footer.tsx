import { SITE } from '@/shared/config'
import { Logo } from '@/shared/ui'

import styles from './Footer.module.scss'

export interface FooterProps {
  orgName?: string
  campusName?: string
  copyrightYear?: string
  version?: string
}

export function Footer({
  orgName = SITE.orgName,
  campusName = SITE.campusName,
  copyrightYear = SITE.copyrightYear,
  version = SITE.version,
}: FooterProps) {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <Logo size="sm" variant="tile" />
        <div className={styles.meta}>
          <p className={styles.line}>
            Copyright © {copyrightYear} {orgName} – {campusName}
          </p>
          {version ? <p className={styles.version}>{version}</p> : null}
        </div>
      </div>
    </footer>
  )
}
