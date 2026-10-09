import { ROUTES } from '@/shared/constants'

import type {
  FeatureLink,
  StaffProfile,
  TeacherProfile,
  TeachingClass,
  TermReport,
} from '../types'

/**
 * Dữ liệu tạm cho trang Thông tin của giảng viên và quản trị — khớp tài khoản
 * demo `GVHCM001`, `admin.hcm`, `admin.master`. Lớp của GV trùng màn đăng ký
 * (INT1313, INT14148 do thầy Đặng Quốc Việt dạy).
 */

export const DEMO_TEN_HOC_KY_HIEN_TAI = 'Học kỳ 1 - Năm học 2026 - 2027'

export const DEMO_TEACHER: TeacherProfile = {
  maGiangVien: 'GVHCM001',
  hoTen: 'Đặng Quốc Việt',
  hocVi: 'Thạc sĩ',
  maKhoa: 'CNTT2',
  tenKhoa: 'Khoa Công nghệ thông tin 2',
  maCoSo: 'HCM',
  tenCoSo: 'Cơ sở TP. Hồ Chí Minh',
  email: 'vietdq@ptithcm.edu.vn',

  /* Lý lịch để trống ĐÚNG CHỦ Ý, giống hồ sơ giảng viên thật trong DB: để thấy
     giao diện xử lý ô rỗng và để thử được luồng tự điền. */
  gioiTinh: null,
  dienThoai: null,
  soCCCD: null,
  emailCaNhan: null,
  noiSinh: null,
  danToc: null,
  tonGiao: null,
  hoKhau: null,
  anhDaiDien: null,
}

/**
 * Lớp GVHCM001 phụ trách — PHẢI khớp `DEMO_CLASSES` của feature `dang-ky`,
 * nơi màn "Lớp phụ trách" và màn nhập điểm lấy dữ liệu.
 *
 * Trước đây hai nơi liệt kê hai bộ lớp khác nhau, nên trang Thông tin nói một
 * đằng mà bảng lớp nói một nẻo. Đổi lớp ở `DEMO_CLASSES` thì sửa cả ở đây.
 */
export const DEMO_TEACHING: readonly TeachingClass[] = [
  {
    maLopHP: 'INT1339-2026-1-HCM01',
    maMonHoc: 'INT1339',
    tenMonHoc: 'Ngôn ngữ lập trình C++',
    soTinChi: 3,
    maHocKy: '2026-1',
    soLuongDaDangKy: 57,
    soLuongToiDa: 60,
    trangThai: 'MO',
    hinhThucHoc: 'TRUC_TIEP',
  },
  {
    maLopHP: 'INT1340-2026-1-HCM01',
    maMonHoc: 'INT1340',
    tenMonHoc: 'Nhập môn công nghệ phần mềm',
    soTinChi: 3,
    maHocKy: '2026-1',
    soLuongDaDangKy: 71,
    soLuongToiDa: 80,
    trangThai: 'MO',
    hinhThucHoc: 'TRUC_TUYEN',
  },
]

export const DEMO_TEACHER_SUMMARY = {
  thongBaoChuaDoc: 2,
  /** Mỗi lớp một buổi/tuần — tính từ `GET /api/me/teaching-schedule?tuan=`. */
  buoiDayTrongTuan: DEMO_TEACHING.length,
}

export const DEMO_CAMPUS_ADMIN: StaffProfile = {
  tenDangNhap: 'admin.hcm',
  hoTen: 'Quản trị Phòng Đào tạo (demo)',
  maCoSo: 'HCM',
  tenCoSo: 'Cơ sở TP. Hồ Chí Minh',
  email: 'daotao@ptithcm.edu.vn',
}

export const DEMO_MASTER_ADMIN: StaffProfile = {
  tenDangNhap: 'admin.master',
  hoTen: 'Quản trị danh mục (demo)',
  maCoSo: null,
  tenCoSo: null,
  email: 'master@ptit.edu.vn',
}

/** Thống kê HK1 2026-27 theo cơ sở. HCM khớp 15 lớp demo của màn đăng ký. */
export const DEMO_CAMPUS_REPORTS: readonly TermReport[] = [
  {
    maCoSo: 'HCM',
    tenPhamVi: 'Cơ sở TP. Hồ Chí Minh',
    soLop: 15,
    luotDangKy: 585,
    soSinhVien: 212,
    tongSucChua: 835,
    tongDaDangKy: 585,
    soLopDay: 1,
    chuaCongBo: 15,
    daCongBo: 0,
    daKhoa: 0,
  },
  {
    maCoSo: 'HN',
    tenPhamVi: 'Cơ sở Hà Nội',
    soLop: 22,
    luotDangKy: 910,
    soSinhVien: 338,
    tongSucChua: 1200,
    tongDaDangKy: 910,
    soLopDay: 4,
    chuaCongBo: 22,
    daCongBo: 0,
    daKhoa: 0,
  },
  {
    maCoSo: 'DN',
    tenPhamVi: 'Cơ sở Đà Nẵng',
    soLop: 9,
    luotDangKy: 301,
    soSinhVien: 117,
    tongSucChua: 450,
    tongDaDangKy: 301,
    soLopDay: 0,
    chuaCongBo: 9,
    daCongBo: 0,
    daKhoa: 0,
  },
]

/** Toàn hệ thống — Admin Master xem không lọc cơ sở. */
export const DEMO_SYSTEM_REPORT: TermReport = DEMO_CAMPUS_REPORTS.reduce<TermReport>(
  (sum, r) => ({
    ...sum,
    soLop: sum.soLop + r.soLop,
    luotDangKy: sum.luotDangKy + r.luotDangKy,
    soSinhVien: sum.soSinhVien + r.soSinhVien,
    tongSucChua: sum.tongSucChua + r.tongSucChua,
    tongDaDangKy: sum.tongDaDangKy + r.tongDaDangKy,
    soLopDay: sum.soLopDay + r.soLopDay,
    chuaCongBo: sum.chuaCongBo + r.chuaCongBo,
    daCongBo: sum.daCongBo + r.daCongBo,
    daKhoa: sum.daKhoa + r.daKhoa,
  }),
  {
    maCoSo: null,
    tenPhamVi: 'Toàn hệ thống',
    soLop: 0,
    luotDangKy: 0,
    soSinhVien: 0,
    tongSucChua: 0,
    tongDaDangKy: 0,
    soLopDay: 0,
    chuaCongBo: 0,
    daCongBo: 0,
    daKhoa: 0,
  },
)

/** Quyền theo bảng phân quyền trong `docs/PTIT-One-API-Contract.md`. */
export const CAMPUS_ADMIN_RIGHTS: readonly string[] = [
  'Mở, sửa lớp học phần và phân công giảng viên — chỉ trong cơ sở mình',
  'Xếp lịch học (chặn trùng giảng viên và phòng)',
  'Mở, đóng đợt đăng ký của cơ sở — mỗi học kỳ một đợt mở',
  'Huỷ lớp: trả chỗ, trả tín chỉ và báo sinh viên trong một giao dịch',
  'Khoá bảng điểm sau khi giảng viên công bố',
  'Xem danh sách sinh viên, bảng điểm và thống kê của cơ sở',
]

export const MASTER_ADMIN_RIGHTS: readonly string[] = [
  'Quản lý danh mục môn học, khoa, học kỳ và môn tiên quyết (chặn chu trình)',
  'Cấp hồ sơ sinh viên, giảng viên kèm tài khoản và mã kích hoạt',
  'Khoá, mở và cấp lại mã kích hoạt tài khoản',
  'Xem thống kê toàn hệ thống, lọc theo từng cơ sở',
  'Chỉ ĐỌC lớp học phần và đợt đăng ký — việc mở lớp thuộc admin cơ sở',
]

const ACCOUNT_LINKS: readonly FeatureLink[] = [
  { label: 'Đổi mật khẩu', href: ROUTES.doiMatKhau },
  { label: 'Email và xác minh', href: ROUTES.email },
  { label: 'Phiên đăng nhập', href: ROUTES.account },
]

export const TEACHER_FEATURES: readonly FeatureLink[] = [
  { label: 'Thông báo', href: ROUTES.thongBao }, // /api/me/notifications
  { label: 'Lớp phụ trách', href: ROUTES.gvLopPhuTrach }, // /api/me/teaching-classes
  { label: 'Nhập điểm', href: ROUTES.gvNhapDiem }, // /api/classes/{maLopHP}/grades
  ...ACCOUNT_LINKS,
]

export const CAMPUS_ADMIN_FEATURES: readonly FeatureLink[] = [
  { label: 'Tổng quan thống kê', href: ROUTES.qtTongQuan }, // /api/reports/*
  { label: 'Đợt đăng ký và ghi danh', href: ROUTES.qtDangKy }, // /api/enrollment-periods
  { label: 'Lớp học phần', href: ROUTES.qtLopHocPhan }, // /api/classes
  ...ACCOUNT_LINKS,
]

export const MASTER_ADMIN_FEATURES: readonly FeatureLink[] = [
  { label: 'Tổng quan thống kê', href: ROUTES.qtTongQuan }, // /api/reports/*
  { label: 'Danh mục và tiên quyết', href: ROUTES.qtDanhMuc }, // /api/courses
  { label: 'Hồ sơ và tài khoản', href: ROUTES.qtHoSo }, // /api/students · /api/accounts
  ...ACCOUNT_LINKS,
]
