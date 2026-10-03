import { useAuth } from '@/features/auth'
import { Dashboard } from '@/features/bao-cao'

/**
 * Tổng quan cho quản trị. Admin cơ sở bị khoá theo cơ sở trong JWT (server
 * trả 403 nếu xin cơ sở khác); Admin Master xem toàn hệ thống hoặc từng cơ sở.
 */
export function AdminDashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <Dashboard
      campusLock={user.role === 'ADMIN_CO_SO' ? user.homeCampus : null}
      exportName="thong-ke"
    />
  )
}
