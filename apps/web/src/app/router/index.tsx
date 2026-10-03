import type { ReactNode } from 'react'
import { Route, Routes } from 'react-router-dom'

import { DefaultLayout } from '@/app/layouts'
import { RequireAuth } from '@/features/auth'
import { AccountPage } from '@/pages/AccountPage'
import { AdminClassesPage } from '@/pages/AdminClassesPage'
import { AdminDashboardPage } from '@/pages/AdminDashboardPage'
import { AdminRegistrationPage } from '@/pages/AdminRegistrationPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { GradesPage } from '@/pages/GradesPage'
import { HomePage } from '@/pages/HomePage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PeriodPage } from '@/pages/PeriodPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { PrerequisitePage } from '@/pages/PrerequisitePage'
import { ProgramPage } from '@/pages/ProgramPage'
import { RegistrationPage } from '@/pages/RegistrationPage'
import { SemesterTimetablePage } from '@/pages/SemesterTimetablePage'
import { TimetablePage } from '@/pages/TimetablePage'
import { UserInfoPage } from '@/pages/UserInfoPage'
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
/** Màn đã dựng xong, theo đường dẫn. Mục chưa có ở đây dùng trang giữ chỗ. */
const SCREENS: Partial<Record<string, ReactNode>> = {
  [ROUTES.qtTongQuan]: <AdminDashboardPage />,
  [ROUTES.qtDangKy]: <AdminRegistrationPage />,
  [ROUTES.qtLopHocPhan]: <AdminClassesPage />,
  [ROUTES.svDangKy]: <RegistrationPage />,
  [ROUTES.svDotDangKy]: <PeriodPage />,
  [ROUTES.svMonHoc]: <PrerequisitePage />,
  [ROUTES.svChuongTrinh]: <ProgramPage />,
  [ROUTES.svLichHoc]: <TimetablePage />,
  [ROUTES.svBangDiem]: <GradesPage />,
  [ROUTES.svLichHocHocKy]: <SemesterTimetablePage />,
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path={ROUTES.home} element={<HomePage />} />
        <Route path={ROUTES.login} element={<LoginPage />} />
        <Route path={ROUTES.forbidden} element={<ForbiddenPage />} />

        <Route element={<RequireAuth />}>
          <Route path={ROUTES.account} element={<AccountPage />} />
          <Route path={ROUTES.userInfo} element={<UserInfoPage />} />
        </Route>

        {NAV_ITEMS.map((item) => (
          <Route key={item.path} element={<RequireAuth roles={item.roles} />}>
            <Route
              path={item.path}
              element={
                SCREENS[item.path] ?? <PlaceholderPage title={item.label} feature={item.feature} />
              }
            />
          </Route>
        ))}

        <Route path={ROUTES.notFound} element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
