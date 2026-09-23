import { Link } from 'react-router-dom'

import { LABELS, ROUTES } from '@/shared/constants'

import styles from './NotFoundPage.module.scss'

export function NotFoundPage() {
  return (
    <section className={styles.wrap}>
      <p className={styles.code}>404</p>
      <h1 className={styles.title}>{LABELS.notFoundTitle}</h1>
      <p className={styles.text}>{LABELS.notFoundText}</p>
      <Link className={styles.link} to={ROUTES.home}>
        {LABELS.home}
      </Link>
    </section>
  )
}
