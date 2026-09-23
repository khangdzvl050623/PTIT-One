import { LABELS } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import { ACCESS_STATS } from '../data/accessStats'

import styles from './AccessStats.module.scss'

/** Khối thống kê truy cập ở góc phải dưới trang chủ. */
export function AccessStats() {
  return (
    <section className={styles.wrap}>
      <h2 className={styles.title}>{LABELS.accessStats}</h2>
      <ul className={styles.list}>
        {ACCESS_STATS.map((stat) => (
          <li key={stat.id} className={styles.row}>
            <Icon name={stat.icon} size="12px" className={styles.icon} />
            <span className={styles.label}>{stat.label}:</span>
            <strong className={styles.value}>{stat.value}</strong>
          </li>
        ))}
      </ul>
    </section>
  )
}
