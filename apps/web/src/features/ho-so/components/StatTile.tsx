import { Link } from 'react-router-dom'

import { Icon } from '@/shared/ui'
import type { IconName } from '@/shared/ui'

import styles from './StatTile.module.scss'

export interface StatTileProps {
  label: string
  value: string | number
  icon: IconName
  href: string
  /** `brand` nền đỏ nhạt, `warm` nền cam nhạt, `plain` nền trắng. */
  tone?: 'brand' | 'warm' | 'plain'
}

/** Ô số liệu nhanh: nhãn, con số lớn, icon tròn và liên kết "Xem chi tiết". */
export function StatTile({ label, value, icon, href, tone = 'plain' }: StatTileProps) {
  return (
    <section className={`${styles.tile} ${styles[tone]}`}>
      <div className={styles.text}>
        <h3 className={styles.label}>{label}</h3>
        <p className={styles.value}>{value}</p>
        <Link className={styles.link} to={href}>
          Xem chi tiết
        </Link>
      </div>
      <span className={styles.icon}>
        <Icon name={icon} size="21px" />
      </span>
    </section>
  )
}
