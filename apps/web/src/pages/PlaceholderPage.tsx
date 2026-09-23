import { SITE } from '@/shared/config'
import { LABELS } from '@/shared/constants'
import { Logo } from '@/shared/ui'

import styles from './PlaceholderPage.module.scss'

/**
 * Trang mặc định cho tuyến chưa dựng xong: chỉ hiển thị logo PTIT.
 * Khi tính năng của tuyến đó hoàn thiện thì thay bằng trang thật.
 */
export function PlaceholderPage() {
  return (
    <section className={styles.wrap}>
      <Logo size="lg" variant="tile" />
      <h1 className={styles.title}>{SITE.appName}</h1>
      <p className={styles.text}>{LABELS.placeholderText}</p>
    </section>
  )
}
