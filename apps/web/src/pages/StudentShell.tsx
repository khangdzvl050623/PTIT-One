import type { ReactNode } from 'react'

import { LoginPanel } from '@/features/auth'
import { FeatureLinks, STUDENT_FEATURES } from '@/features/ho-so'

import styles from './StudentShell.module.scss'

export interface StudentShellProps {
  /** Các khung nội dung, xếp dọc ở cột chính. */
  children: ReactNode
}

/**
 * Bố cục chung các màn sinh viên: nội dung bên trái, cột phải (tài khoản +
 * TÍNH NĂNG) dính dưới header. Khi in chỉ còn nội dung.
 */
export function StudentShell({ children }: StudentShellProps) {
  return (
    <div className={styles.columns}>
      <div className={styles.main}>{children}</div>

      <aside className={styles.aside} data-print="hide">
        <LoginPanel />
        <FeatureLinks links={STUDENT_FEATURES} />
      </aside>
    </div>
  )
}
