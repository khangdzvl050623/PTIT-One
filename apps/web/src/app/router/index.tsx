import type { ReactNode } from 'react'
import { Route, Routes } from 'react-router-dom'

import { DefaultLayout } from '@/app/layouts'
import { RequireAuth } from '@/features/auth'
import { AccountAdminPage } from '@/pages/AccountAdminPage'
import { AccountPage } from '@/pages/AccountPage'
import { AdminClassesPage } from '@/pages/AdminClassesPage'
import { AdminDashboardPage } from '@/pages/AdminDashboardPage'
import { AdminRegistrationPage } from '@/pages/AdminRegistrationPage'
import { CatalogAdminPage } from '@/pages/CatalogAdminPage'
import {
  ActivatePage,
  ChangePasswordPage,
  EmailPage,
  ForgotPasswordPage,
} from '@/pages/CredentialPages'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { GradeEntryPage } from '@/pages/GradeEntryPage'
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
import { TeachingClassesPage } from '@/pages/TeachingClassesPage'
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
  [ROUTES.doiMatKhau]: <ChangePasswordPage />,
  [ROUTES.email]: <EmailPage />,
  [ROUTES.gvLopPhuTrach]: <TeachingClassesPage />,
  [ROUTES.gvNhapDiem]: <GradeEntryPage />,
  [ROUTES.qtTongQuan]: <AdminDashboardPage />,
  [ROUTES.qtHoSo]: <AccountAdminPage />,
  [ROUTES.qtDangKy]: <AdminRegistrationPage />,
  [ROUTES.qtLopHocPhan]: <AdminClassesPage />,
  [ROUTES.qtDanhMuc]: <CatalogAdminPage />,
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
        {/* Hai tuyến này CỐ Ý không gác: tài khoản mới cấp chưa có mật khẩu, và
            người quên mật khẩu thì không đăng nhập trước được. */}
        <Route path={ROUTES.kichHoat} element={<ActivatePage />} />
        <Route path={ROUTES.quenMatKhau} element={<ForgotPasswordPage />} />
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
