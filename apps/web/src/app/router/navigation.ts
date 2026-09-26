import type { Role } from '@/features/auth'
import { ROUTES } from '@/shared/constants'

export interface NavItem {
  path: string
  label: string
  /** Vai trò được phép mở. Router và menu dùng chung danh sách này. */
  roles: Role[]
  /** Gói chức năng trong kế hoạch Phần 1 — hiện trên trang giữ chỗ. */
  feature: string
}

/**
 * Khu vực theo vai trò.
 *
 * Đây là **nguồn duy nhất** cho cả menu lẫn tuyến được bảo vệ: router sinh
 * `<RequireAuth roles={...}>` từ chính danh sách này. Nhờ vậy không có cảnh
 * menu hiện một mục mà router không có tuyến, hoặc tuyến tồn tại nhưng thiếu
 * người gác.
 *
 * Mỗi mục hiện trỏ vào trang giữ chỗ. Khi màn thật xong thì đổi `element`
 * trong router, không phải sửa ở đây.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  {
    path: ROUTES.svDangKy,
    label: 'Đăng ký học phần',
    roles: ['SINH_VIEN'],
    feature: 'F08',
  },
  {
    path: ROUTES.svBangDiem,
    label: 'Bảng điểm',
    roles: ['SINH_VIEN'],
    feature: 'F07',
  },
  {
    path: ROUTES.svLichHoc,
    label: 'Thời khoá biểu',
    roles: ['SINH_VIEN'],
    feature: 'F09',
  },
  {
    path: ROUTES.gvLopPhuTrach,
    label: 'Lớp phụ trách',
    roles: ['GIANG_VIEN'],
    feature: 'F05',
  },
  {
    path: ROUTES.gvNhapDiem,
    label: 'Nhập điểm',
    roles: ['GIANG_VIEN'],
    feature: 'F06',
  },
  {
    path: ROUTES.qtHoSo,
    label: 'Hồ sơ và tài khoản',
    roles: ['ADMIN_CO_SO'],
    feature: 'F02',
  },
  {
    path: ROUTES.qtLopHocPhan,
    label: 'Lớp học phần',
    roles: ['ADMIN_CO_SO'],
    feature: 'F04',
  },
  {
    path: ROUTES.qtDanhMuc,
    label: 'Danh mục và tiên quyết',
    roles: ['ADMIN_MASTER'],
    feature: 'F03',
  },
]

/** Menu của một vai trò. Chưa đăng nhập thì không có mục nào. */
export function navItemsFor(role: Role | null | undefined): NavItem[] {
  if (!role) return []
  return NAV_ITEMS.filter((item) => item.roles.includes(role))
}
