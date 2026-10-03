import { Link } from 'react-router-dom'

import { Icon, Panel } from '@/shared/ui'

import type { FeatureLink } from '../types'
import styles from './FeatureLinks.module.scss'

export interface FeatureLinksProps {
  links: readonly FeatureLink[]
}

/** Khung "TÍNH NĂNG" ở cột phải: danh sách lối tắt có mũi tên. */
export function FeatureLinks({ links }: FeatureLinksProps) {
  return (
    <Panel title="TÍNH NĂNG" icon="gear">
      <ul className={styles.list}>
        {links.map((link) => (
          <li key={link.label}>
            <Link className={styles.link} to={link.href}>
              <Icon name="chevronRight" size="12px" />
              <span>{link.label}</span>
            </Link>
          </li>
        ))}
      </ul>
    </Panel>
  )
}
