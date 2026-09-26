import { useState } from 'react'

import { ROLE_LABELS, useAuth } from '@/features/auth'
import { LABELS } from '@/shared/constants'
import { Panel } from '@/shared/ui'

import styles from './AccountPage.module.scss'

/**
 * Màn hình tối thiểu cho tuyến cần đăng nhập. Dữ liệu ở đây lấy từ
 * `GET /api/auth/me`, tức là danh tính do **server** xác định — không phải
 * thứ client tự khai.
 */
export function AccountPage() {
  const { user, signOut, signOutEverywhere } = useAuth()
  const [pending, setPending] = useState(false)

  if (!user) return null

  async function run(action: () => Promise<void>) {
    setPending(true)
    try {
      await action()
    } finally {
      setPending(false)
    }
  }

  return (
    <Panel title={LABELS.account} icon="user">
      <dl className={styles.rows}>
        <div className={styles.row}>
          <dt className={styles.term}>Tên đăng nhập</dt>
          <dd className={styles.value}>{user.username}</dd>
        </div>
        <div className={styles.row}>
          <dt className={styles.term}>Vai trò</dt>
          <dd className={styles.value}>{ROLE_LABELS[user.role]}</dd>
        </div>
        {user.entityId ? (
          <div className={styles.row}>
            <dt className={styles.term}>Mã hồ sơ</dt>
            <dd className={styles.value}>{user.entityId}</dd>
          </div>
        ) : null}
        <div className={styles.row}>
          <dt className={styles.term}>Cơ sở</dt>
          {/* ADMIN_MASTER không thuộc cơ sở nào — hiện dấu gạch, không bịa mã. */}
          <dd className={styles.value}>{user.homeCampus ?? '—'}</dd>
        </div>
        <div className={styles.row}>
          <dt className={styles.term}>Phiên hết hạn</dt>
          <dd className={styles.value}>{formatMoment(user.expiresAt)}</dd>
        </div>
      </dl>

      <div className={styles.actions}>
        <button
          className={styles.action}
          type="button"
          disabled={pending}
          onClick={() => void run(signOut)}
        >
          {LABELS.logout}
        </button>
        <button
          className={styles.action}
          type="button"
          disabled={pending}
          onClick={() => void run(signOutEverywhere)}
        >
          {LABELS.logoutEverywhere}
        </button>
      </div>
    </Panel>
  )
}

function formatMoment(iso: string): string {
  const value = Date.parse(iso)
  return Number.isNaN(value) ? '—' : new Date(value).toLocaleString('vi-VN')
}
