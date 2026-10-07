import { Icon, Logo } from '@/shared/ui'

import styles from './IdPhoto.module.scss'

/** Ảnh thẻ mặc định 3×4 kèm logo Học viện — hệ thống chưa lưu ảnh thẻ. */
export function IdPhoto() {
  return (
    <div className={styles.photo} role="img" aria-label="Chưa có ảnh thẻ">
      <span className={styles.logo}>
        <Logo size="md" variant="tile" alt="" />
      </span>
      <Icon name="user" className={styles.figure} />
    </div>
  )
}
