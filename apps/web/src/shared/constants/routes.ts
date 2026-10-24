/**
 * Đường dẫn của các tuyến — dùng chung giữa router, menu và `<Link>`.
 * Không viết chuỗi đường dẫn thẳng trong component.
 */
export const ROUTES = {
  home: '/',
  login: '/dang-nhap',
  /** Chưa đăng nhập được vẫn phải vào: tài khoản mới cấp chưa có mật khẩu. */
  kichHoat: '/kich-hoat',
  quenMatKhau: '/quen-mat-khau',
  account: '/tai-khoan',
  userInfo: '/thong-tin',
  thongBao: '/thong-bao',
  doiMatKhau: '/tai-khoan/doi-mat-khau',
  email: '/tai-khoan/email',

  // Sinh viên — F07, F08, F09
  svDangKy: '/sinh-vien/dang-ky',
  svBangDiem: '/sinh-vien/bang-diem',
  svLichHoc: '/sinh-vien/lich-hoc',
  svLichHocHocKy: '/sinh-vien/lich-hoc-hoc-ky',
  svChuongTrinh: '/sinh-vien/chuong-trinh-dao-tao',
  svMonHoc: '/sinh-vien/mon-hoc',
  svDotDangKy: '/sinh-vien/dot-dang-ky',

  // Giảng viên — F05, F06
  gvLopPhuTrach: '/giang-vien/lop-phu-trach',
  gvNhapDiem: '/giang-vien/nhap-diem',

  // Quản trị — tổng quan (cả hai), F02 (ADMIN_MASTER), F04 (ADMIN_CO_SO)
  qtTongQuan: '/quan-tri/tong-quan',
  qtHoSo: '/quan-tri/ho-so',
  qtLopHocPhan: '/quan-tri/lop-hoc-phan',
  qtDangKy: '/quan-tri/dang-ky-hoc-phan',

  // Quản trị danh mục (ADMIN_MASTER) — F03
  qtDanhMuc: '/quan-tri/danh-muc',
  /** Soạn tin cho SV/GV trong phạm vi của mình. */
  qtThongBao: '/quan-tri/thong-bao',

  forbidden: '/khong-du-quyen',
  notFound: '*',
} as const

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES]
