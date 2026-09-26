import { SITE } from '@/shared/config'
import { LABELS } from '@/shared/constants'
import { Logo } from '@/shared/ui'

import styles from './PlaceholderPage.module.scss'

export interface PlaceholderPageProps {
  /** Tên màn hình sẽ thay thế chỗ này. Bỏ trống thì hiện tên sản phẩm. */
  title?: string
  /** Mã gói chức năng trong kế hoạch, ví dụ `F08`. */
  feature?: string
}

/**
 * Trang giữ chỗ cho tuyến chưa dựng xong.
 *
 * Ghi rõ màn hình nào sẽ thay thế và thuộc gói chức năng nào, để người mở
 * trúng không tưởng đây là tính năng đã hỏng.
 */
export function PlaceholderPage({ title, feature }: PlaceholderPageProps) {
  return (
    <section className={styles.wrap}>
      <Logo size="lg" variant="tile" />
      <h1 className={styles.title}>{title ?? SITE.appName}</h1>
      <p className={styles.text}>{LABELS.placeholderText}</p>
      {feature ? <p className={styles.feature}>Gói chức năng {feature}</p> : null}
    </section>
  )
}
