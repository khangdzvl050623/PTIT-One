import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { ROUTES } from '@/shared/constants'

import { useAuth } from '../model/AuthContext'
import type { Role } from '../model/types'

export interface RequireAuthProps {
  /** Bỏ trống nghĩa là chỉ cần đăng nhập, không phân biệt vai trò. */
  roles?: Role[]
}

/**
 * Chặn tuyến ở phía giao diện.
 *
 * ⚠️ Đây **chỉ là trải nghiệm người dùng**, không phải lớp bảo vệ. Ai cũng sửa
 * được JavaScript trong trình duyệt. Quyền thật do backend kiểm ở mỗi request;
 * ẩn nút hay chặn route không thay thế được điều đó.
 */
export function RequireAuth({ roles }: RequireAuthProps) {
  const { status, user } = useAuth()
  const location = useLocation()

  /* Chưa biết còn phiên hay không. Điều hướng lúc này sẽ đá nhầm người đang
     đăng nhập hợp lệ ra màn hình login mỗi lần F5. */
  if (status === 'loading') {
    return null
  }

  if (status === 'anonymous' || !user) {
    // Nhớ nơi định tới để đăng nhập xong quay lại đúng chỗ.
    return <Navigate to={ROUTES.login} state={{ from: location.pathname }} replace />
  }

  if (roles && roles.length > 0 && !roles.includes(user.role)) {
    return <Navigate to={ROUTES.forbidden} replace />
  }

  return <Outlet />
}
