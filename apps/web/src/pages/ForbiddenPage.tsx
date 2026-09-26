import { Link } from 'react-router-dom'

import { LABELS, ROUTES } from '@/shared/constants'

import styles from './NotFoundPage.module.scss'

/**
 * Hiện khi đã đăng nhập nhưng sai vai trò. Khác 404: ở đây trang có tồn tại,
 * chỉ là tài khoản này không được mở.
 */
export function ForbiddenPage() {
  return (
    <section className={styles.wrap}>
      <p className={styles.code}>403</p>
      <h1 className={styles.title}>{LABELS.forbiddenTitle}</h1>
      <p className={styles.text}>{LABELS.forbiddenText}</p>
      <Link className={styles.link} to={ROUTES.home}>
        {LABELS.home}
      </Link>
    </section>
  )
}
