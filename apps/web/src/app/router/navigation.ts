import { ROLES } from '@/features/auth'
import type { Role } from '@/features/auth'
import { ROUTES } from '@/shared/constants'

export interface NavItem {
  path: string
  label: string
  /** Vai trò được phép mở. Router và menu dùng chung danh sách này. */
  roles: Role[]
  /** Gói chức năng trong kế hoạch Phần 1 — hiện trên trang giữ chỗ. */
  feature: string
  /**
   * `false`: vẫn có tuyến và người gác, nhưng không lên thanh menu trên cùng —
   * chỉ mở từ lối tắt (khung TÍNH NĂNG). Bỏ trống là có trong menu.
   */
  inMenu?: boolean
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
    path: ROUTES.qtTongQuan,
    label: 'Tổng quan',
    roles: ['ADMIN_CO_SO', 'ADMIN_MASTER'],
    feature: 'Thống kê',
  },
  {
    path: ROUTES.qtHoSo,
    label: 'Hồ sơ và tài khoản',
    // Cấp hồ sơ SV/GV và tài khoản chỉ ở Master (chốt 02/10/2026, StudentController).
    roles: ['ADMIN_MASTER'],
    feature: 'F02',
  },
  {
    path: ROUTES.qtDangKy,
    label: 'Đăng ký học phần',
    roles: ['ADMIN_CO_SO'],
    feature: 'F08',
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

  // --- Ngoài menu: lối tắt ở trang Thông tin ---------------------------
  {
    path: ROUTES.thongBao,
    label: 'Thông báo',
    roles: ['SINH_VIEN', 'GIANG_VIEN'],
    feature: 'Thông báo',
    inMenu: false,
  },
  {
    path: ROUTES.svLichHocHocKy,
    label: 'Thời khoá biểu dạng học kỳ',
    roles: ['SINH_VIEN'],
    feature: 'F09',
    inMenu: false,
  },
  {
    path: ROUTES.svChuongTrinh,
    label: 'Chương trình đào tạo',
    roles: ['SINH_VIEN'],
    feature: 'F03',
    inMenu: false,
  },
  {
    path: ROUTES.svMonHoc,
    label: 'Môn học và môn tiên quyết',
    roles: ['SINH_VIEN'],
    feature: 'F03',
    inMenu: false,
  },
  {
    path: ROUTES.svDotDangKy,
    label: 'Lịch đợt đăng ký',
    roles: ['SINH_VIEN'],
    feature: 'F08',
    inMenu: false,
  },
  {
    path: ROUTES.doiMatKhau,
    label: 'Đổi mật khẩu',
    roles: [...ROLES],
    feature: 'A1',
    inMenu: false,
  },
  {
    path: ROUTES.email,
    label: 'Email và xác minh',
    roles: [...ROLES],
    feature: 'A1',
    inMenu: false,
  },
]

/** Menu của một vai trò. Chưa đăng nhập thì không có mục nào. */
export function navItemsFor(role: Role | null | undefined): NavItem[] {
  if (!role) return []
  return NAV_ITEMS.filter((item) => item.inMenu !== false && item.roles.includes(role))
}
