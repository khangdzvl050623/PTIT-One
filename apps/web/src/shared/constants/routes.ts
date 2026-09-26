/**
 * Đường dẫn của các tuyến — dùng chung giữa router, menu và `<Link>`.
 * Không viết chuỗi đường dẫn thẳng trong component.
 */
export const ROUTES = {
  home: '/',
  login: '/dang-nhap',
  account: '/tai-khoan',

  // Sinh viên — F07, F08, F09
  svDangKy: '/sinh-vien/dang-ky',
  svBangDiem: '/sinh-vien/bang-diem',
  svLichHoc: '/sinh-vien/lich-hoc',

  // Giảng viên — F05, F06
  gvLopPhuTrach: '/giang-vien/lop-phu-trach',
  gvNhapDiem: '/giang-vien/nhap-diem',

  // Quản trị đào tạo (ADMIN_CO_SO) — F02, F04
  qtHoSo: '/quan-tri/ho-so',
  qtLopHocPhan: '/quan-tri/lop-hoc-phan',

  // Quản trị danh mục (ADMIN_MASTER) — F03
  qtDanhMuc: '/quan-tri/danh-muc',

  forbidden: '/khong-du-quyen',
  notFound: '*',
} as const

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES]
