import { useEffect, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { safeTarget } from '@/app/router/navigation'
import { LoginPanel, useAuth } from '@/features/auth'
import { FlashBanner, clearLocationFlash, consumePendingFlash } from '@/shared/ui'
import type { Flash, FlashRouteState } from '@/shared/ui'

import styles from './LoginPage.module.scss'

export function LoginPage() {
  const { status, user } = useAuth()
  const location = useLocation()

  const state = location.state as FlashRouteState | null
  const from = state?.from

  /* Tin một lần (đổi MK xong, kích hoạt xong…): ưu tiên state của entry,
     rơi xuống kho dự phòng khi entry bị người gác ghi đè giữa đường. Đọc rồi
     xoá ngay để F5 hay back không hiện lại. */
  const [flash, setFlash] = useState<Flash | null>(null)
  useEffect(() => {
    const incoming = state?.flash ?? consumePendingFlash()
    if (incoming) {
      setFlash(incoming)
      clearLocationFlash()
    }
  }, [state])

  /* Đã đăng nhập thì không mở lại màn đăng nhập. Nơi đến phải hợp vai trò:
     quay lại một tuyến của vai trò khác sẽ rơi vào /khong-du-quyen. */
  if (status === 'authenticated' && user) {
    return <Navigate to={safeTarget(from, user.role)} replace />
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {flash ? (
          <div className={styles.flash}>
            <FlashBanner flash={flash} onClose={() => setFlash(null)} />
          </div>
        ) : null}
        {/* Vai trò chỉ biết sau khi đăng nhập, nên đưa hàm chọn đích xuống. */}
        <LoginPanel redirectTo={(role) => safeTarget(from, role)} flash={flash} />
      </div>
    </div>
  )
}
