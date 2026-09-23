import { Route, Routes } from 'react-router-dom'

import { DefaultLayout } from '@/app/layouts'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ROUTES } from '@/shared/constants'

/**
 * Khai báo tuyến tập trung. Path lấy từ `ROUTES`, không viết chuỗi thẳng ở đây.
 * Route guard theo vai trò sẽ bọc quanh `<Route element={...}>` khi có auth.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path={ROUTES.home} element={<HomePage />} />
        <Route path={ROUTES.notFound} element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
