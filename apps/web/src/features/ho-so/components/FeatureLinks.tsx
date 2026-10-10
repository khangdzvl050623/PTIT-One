import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

import { Icon, Panel } from '@/shared/ui'

import type { FeatureLink } from '../types'
import styles from './FeatureLinks.module.scss'

export interface FeatureLinksProps {
  links: readonly FeatureLink[]
  /** Nội dung thêm sau nhãn link (ví dụ chấm đỏ chưa đọc của Thông báo). */
  after?: (link: FeatureLink) => ReactNode
}

/** Khung "TÍNH NĂNG" ở cột phải: danh sách lối tắt có mũi tên. */
export function FeatureLinks({ links, after }: FeatureLinksProps) {
  return (
    <Panel title="TÍNH NĂNG" icon="gear">
      <ul className={styles.list}>
        {links.map((link) => {
          const extra = after?.(link)
          return (
            <li key={link.label}>
              <Link className={styles.link} to={link.href}>
                <Icon name="chevronRight" size="12px" />
                <span>{link.label}</span>
                {extra ? <span className={styles.after}>{extra}</span> : null}
              </Link>
            </li>
          )
        })}
      </ul>
    </Panel>
  )
}
