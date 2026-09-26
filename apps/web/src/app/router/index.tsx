import { Route, Routes } from 'react-router-dom'

import { DefaultLayout } from '@/app/layouts'
import { RequireAuth } from '@/features/auth'
import { AccountPage } from '@/pages/AccountPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ROUTES } from '@/shared/constants'

/**
 * Khai báo tuyến tập trung. Path lấy từ `ROUTES`, không viết chuỗi thẳng ở đây.
 *
 * Tuyến cần đăng nhập nằm trong `<Route element={<RequireAuth />}>`; thêm
 * `roles` khi tuyến chỉ dành cho một số vai trò. Đây là lớp trải nghiệm —
 * quyền thật do backend kiểm ở mỗi request.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path={ROUTES.home} element={<HomePage />} />
        <Route path={ROUTES.login} element={<LoginPage />} />
        <Route path={ROUTES.forbidden} element={<ForbiddenPage />} />

        <Route element={<RequireAuth />}>
          <Route path={ROUTES.account} element={<AccountPage />} />
        </Route>

        <Route path={ROUTES.notFound} element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
