import { LABELS } from '@/shared/constants'
import { Icon } from '@/shared/ui'

import type { Notice } from '../types'

import { NewBadge } from './NewBadge'

import styles from './NoticeSpotlight.module.scss'

export interface NoticeSpotlightProps {
  notice: Notice
}

/** Thông báo tiêu điểm: bảng gọi chú ý nền trắng viền đỏ + tiêu đề và trích đoạn. */
export function NoticeSpotlight({ notice }: NoticeSpotlightProps) {
  return (
    <article className={styles.spotlight}>
      <div className={styles.callout}>
        <Icon name="bullhorn" size="30px" className={styles.calloutIcon} />
        <span className={styles.calloutTitle}>{LABELS.notices}</span>
      </div>

      <time className={styles.meta}>{notice.publishedAt}</time>

      <a className={styles.title} href={notice.href}>
        {notice.title}
        {notice.isNew ? <NewBadge /> : null}
      </a>

      {notice.excerpt ? <p className={styles.excerpt}>{notice.excerpt}</p> : null}
    </article>
  )
}
