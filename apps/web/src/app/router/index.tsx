import { Route, Routes } from 'react-router-dom'

import { DefaultLayout } from '@/app/layouts'
import { RequireAuth } from '@/features/auth'
import { AccountPage } from '@/pages/AccountPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { ROUTES } from '@/shared/constants'

import { NAV_ITEMS } from './navigation'

/**
 * Khai báo tuyến tập trung. Path lấy từ `ROUTES`, không viết chuỗi thẳng ở đây.
 *
 * Tuyến theo vai trò được sinh từ `NAV_ITEMS` để menu và router không bao giờ
 * lệch nhau. Màn thật xong thì đổi `element` của mục tương ứng.
 *
 * ⚠️ `RequireAuth` chỉ là lớp trải nghiệm. Quyền thật do backend kiểm ở mỗi
 * request — ẩn menu hay chặn tuyến không thay thế được điều đó.
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

        {NAV_ITEMS.map((item) => (
          <Route key={item.path} element={<RequireAuth roles={item.roles} />}>
            <Route
              path={item.path}
              element={<PlaceholderPage title={item.label} feature={item.feature} />}
            />
          </Route>
        ))}

        <Route path={ROUTES.notFound} element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
