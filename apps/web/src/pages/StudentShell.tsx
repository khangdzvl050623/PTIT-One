import type { ReactNode } from 'react'

import { LoginPanel, useAuth } from '@/features/auth'
import { FeatureLinks, STUDENT_FEATURES } from '@/features/ho-so'
import { UnreadBadge, useUnreadCount } from '@/features/thong-bao'
import { ROUTES } from '@/shared/constants'

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
  const { status } = useAuth()
  const unread = useUnreadCount(status === 'authenticated')

  return (
    <div className={styles.columns}>
      <div className={styles.main}>{children}</div>

      <aside className={styles.aside} data-print="hide">
        <LoginPanel />
        <FeatureLinks
          links={STUDENT_FEATURES}
          after={(link) =>
            link.href === ROUTES.thongBao ? <UnreadBadge count={unread} /> : null
          }
        />
      </aside>
    </div>
  )
}
