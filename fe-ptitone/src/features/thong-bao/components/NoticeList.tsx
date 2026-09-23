import type { Notice } from '../types'

import { NoticeRow } from './NoticeRow'

import styles from './NoticeList.module.scss'

export interface NoticeListProps {
  notices: readonly Notice[]
}

export function NoticeList({ notices }: NoticeListProps) {
  return (
    <ul className={styles.list}>
      {notices.map((notice) => (
        <NoticeRow key={notice.id} notice={notice} />
      ))}
    </ul>
  )
}
