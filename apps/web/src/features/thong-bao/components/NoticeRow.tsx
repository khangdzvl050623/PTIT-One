import type { Notice } from '../types'

import { NewBadge } from './NewBadge'

import styles from './NoticeRow.module.scss'

export interface NoticeRowProps {
  notice: Notice
}

/** Một dòng thông báo: dấu chevron >>, tiêu đề, nhãn New và thời điểm đăng. */
export function NoticeRow({ notice }: NoticeRowProps) {
  return (
    <li className={styles.row}>
      <span className={styles.text}>
        <span className={styles.chevron} aria-hidden="true">
          &gt;&gt;
        </span>
        <a className={styles.link} href={notice.href}>
          {notice.title}
        </a>
        {notice.isNew ? <NewBadge /> : null}
      </span>
      <time className={styles.meta}>{notice.publishedAt}</time>
    </li>
  )
}
