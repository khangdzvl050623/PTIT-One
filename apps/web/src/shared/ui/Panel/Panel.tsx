import type { ReactNode } from 'react'

import { Icon } from '../Icon'
import type { IconName } from '../Icon'

import styles from './Panel.module.scss'

export interface PanelProps {
  /** Tiêu đề trên thanh đỏ của khung. */
  title: string
  /** Icon tuỳ chọn đặt trước tiêu đề. */
  icon?: IconName
  className?: string
  children: ReactNode
}

/** Khung nội dung kiểu cổng thông tin: thanh tiêu đề đỏ + vùng thân trắng. */
export function Panel({ title, icon, className, children }: PanelProps) {
  const classes = [styles.panel, className].filter(Boolean).join(' ')

  return (
    <section className={classes}>
      <h2 className={styles.header}>
        {icon ? <Icon name={icon} size="12px" /> : null}
        <span>{title}</span>
      </h2>
      <div className={styles.body}>{children}</div>
    </section>
  )
}
