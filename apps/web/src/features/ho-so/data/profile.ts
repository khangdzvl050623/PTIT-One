import { ROUTES } from '@/shared/constants'

import type { FeatureLink, StudentProfile, StudentSummary, TermResults } from '../types'

/**
 * Dữ liệu tạm để dựng giao diện — khớp seed `B26DCCN001`. Thay bằng API khi
 * backend có endpoint hồ sơ của chính sinh viên (hiện chưa có).
 */
export const DEMO_PROFILE: StudentProfile = {
  maSinhVien: 'B26DCCN001',
  hoTen: 'Nguyễn Văn An',
  ngaySinh: '2008-03-14',
  email: 'b26dccn001@stu.ptit.edu.vn',
  gioiTinh: 'NAM',
  dienThoai: '0901 234 567',
  soCCCD: '079208001234',
  email2: 'nguyenvanan.dev@gmail.com',
  noiSinh: 'TP. Hồ Chí Minh',
  danToc: 'Kinh',
  tonGiao: 'Không',
  hoKhau: '97 Man Thiện, P. Hiệp Phú, TP. Thủ Đức, TP. Hồ Chí Minh',
  maCoSoNha: 'HCM',
  tenCoSo: 'Cơ sở TP. Hồ Chí Minh',
  trangThai: 'DANG_HOC',
  maCTDT: 'CNTT2026',
  tenCTDT: 'Công nghệ thông tin',
  tenKhoa: 'Khoa Công nghệ thông tin 2',
  tongTinChiCTDT: 150,
  soTinChiTichLuy: 42,
}

export const DEMO_SUMMARY: StudentSummary = {
  thongBaoChuaDoc: 3,
  buoiHocTrongTuan: 6,
  tinChiHocKy: { daDangKy: 17, tran: 25 },
}

/** Mới nhất trước — ô chọn học kỳ mặc định lấy phần tử đầu. */
export const DEMO_RESULTS: readonly TermResults[] = [
  {
    maHocKy: '2025-2026-HK2',
    tenHocKy: 'Học kỳ 2 năm học 2025-2026',
    monHoc: [
      { maMonHoc: 'INT1339', tenMonHoc: 'Ngôn ngữ lập trình C++', diemTongKet: 8.4 },
      { maMonHoc: 'INT1358', tenMonHoc: 'Toán rời rạc 1', diemTongKet: 7.1 },
      { maMonHoc: 'BAS1203', tenMonHoc: 'Giải tích 2', diemTongKet: 6.5 },
      { maMonHoc: 'BAS1227', tenMonHoc: 'Vật lý 3 và thí nghiệm', diemTongKet: 3.6 },
      { maMonHoc: 'SKD1102', tenMonHoc: 'Kỹ năng làm việc nhóm', diemTongKet: 9.0 },
      { maMonHoc: 'BAS1107', tenMonHoc: 'Giáo dục thể chất 2', diemTongKet: null },
    ],
  },
  {
    maHocKy: '2025-2026-HK1',
    tenHocKy: 'Học kỳ 1 năm học 2025-2026',
    monHoc: [
      { maMonHoc: 'INT1154', tenMonHoc: 'Tin học cơ sở 1', diemTongKet: 8.8 },
      { maMonHoc: 'BAS1201', tenMonHoc: 'Đại số', diemTongKet: 7.5 },
      { maMonHoc: 'BAS1202', tenMonHoc: 'Giải tích 1', diemTongKet: 6.2 },
      { maMonHoc: 'BAS1224', tenMonHoc: 'Vật lý 1 và thí nghiệm', diemTongKet: 7.0 },
      { maMonHoc: 'BAS1106', tenMonHoc: 'Giáo dục thể chất 1', diemTongKet: 8.0 },
    ],
  },
]

/**
 * Lối tắt cột phải — đủ mọi chức năng backend mở cho `SINH_VIEN` (xem bảng
 * quyền trong `docs/PTIT-One-API-Contract.md`). Không thêm mục của cổng gốc mà
 * backend không có (học phí, hoá đơn, lịch thi, nguyện vọng, góp ý).
 */
export const STUDENT_FEATURES: readonly FeatureLink[] = [
  { label: 'Thông báo từ ban quản trị', href: ROUTES.thongBao }, // /api/me/notifications
  { label: 'Xem chương trình đào tạo', href: ROUTES.svChuongTrinh }, // /api/programs/{maCTDT}
  { label: 'Xem môn học tiên quyết', href: ROUTES.svMonHoc }, // /api/courses
  { label: 'Lịch đợt đăng ký', href: ROUTES.svDotDangKy }, // /api/enrollment-periods
  { label: 'Đăng ký môn học', href: ROUTES.svDangKy }, // /api/me/enrollments
  { label: 'Thời khoá biểu dạng tuần', href: ROUTES.svLichHoc }, // /api/me/timetable?tuan=
  { label: 'Thời khoá biểu dạng học kỳ', href: ROUTES.svLichHocHocKy }, // /api/me/timetable
  { label: 'Xem điểm', href: ROUTES.svBangDiem }, // /api/me/grades
  { label: 'Đổi mật khẩu', href: ROUTES.doiMatKhau }, // /api/auth/change-password
  { label: 'Email và xác minh', href: ROUTES.email }, // /api/auth/email
  { label: 'Phiên đăng nhập', href: ROUTES.account }, // /api/auth/logout-all
]
