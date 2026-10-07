/**
 * Hồ sơ hiển thị ở trang Thông tin.
 *
 * Khớp `StudentDetail` của `GET /api/me/profile`.
 *
 * Nhóm trường "lý lịch" (giới tính → hộ khẩu) và `anhDaiDien` do migration `V8`
 * thêm vào `SinhVien`. Hồ sơ cũ chưa có dữ liệu nên tất cả đều `null` được.
 */
export interface StudentProfile {
  maSinhVien: string
  hoTen: string
  /** ISO `yyyy-mm-dd`; `null` khi hồ sơ chưa nhập. */
  ngaySinh: string | null
  email: string | null

  // --- Lý lịch (V8) ---
  gioiTinh: 'NAM' | 'NU' | null
  dienThoai: string | null
  soCCCD: string | null
  /** Email cá nhân, ngoài email trường cấp. */
  emailCaNhan: string | null
  noiSinh: string | null
  danToc: string | null
  tonGiao: string | null
  hoKhau: string | null
  /** URL ảnh trên Cloudinary; `null` thì hiển thị chữ cái đầu của họ tên. */
  anhDaiDien: string | null

  maCoSoNha: string
  tenCoSo: string
  /** `DANG_HOC` · `BAO_LUU` · `THOI_HOC` · `TOT_NGHIEP` */
  trangThai: string
  maCTDT: string
  tenCTDT: string
  tenKhoa: string
  tongTinChiCTDT: number
  soTinChiTichLuy: number
}

/** Ba ô số liệu nhanh ở cột giữa. */
export interface StudentSummary {
  /** Số thông báo chưa đọc của toàn hộp thư (`Inbox.soChuaDoc`). */
  thongBaoChuaDoc: number
  /** Số buổi học trong tuần hiện tại, từ thời khoá biểu (F09). */
  buoiHocTrongTuan: number
  /** Tín chỉ học kỳ đang mở đăng ký (`SinhVienHocKy`). */
  tinChiHocKy: { daDangKy: number; tran: number }
}

/** Một môn trong bảng điểm (F07), rút gọn cho biểu đồ. */
export interface CourseResult {
  maMonHoc: string
  tenMonHoc: string
  /** Thang 10. `null` khi chưa công bố — **không bao giờ đổi thành 0**. */
  diemTongKet: number | null
}

export interface TermResults {
  maHocKy: string
  tenHocKy: string
  monHoc: readonly CourseResult[]
}

export interface FeatureLink {
  label: string
  href: string
}

/** Khớp `Teacher` (`GET /api/teachers`) + email/trạng thái của tài khoản. */
export interface TeacherProfile {
  maGiangVien: string
  hoTen: string
  hocVi: string | null
  maKhoa: string
  tenKhoa: string
  maCoSo: string
  tenCoSo: string
  email: string | null
}

/** Một lớp GV phụ trách — rút từ `GET /api/me/teaching-classes`. */
export interface TeachingClass {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  soLuongDaDangKy: number
  soLuongToiDa: number
  /** Lịch gọn, ví dụ `T6 (1–3) · 1A105`. */
  lich: string
  /** Bảng điểm của lớp: `NHAP` · `DA_CONG_BO` · `DA_KHOA`. */
  trangThaiDiem: 'NHAP' | 'DA_CONG_BO' | 'DA_KHOA'
}

/** Tài khoản quản trị — không gắn hồ sơ SV/GV. `maCoSo = null` với Admin Master. */
export interface StaffProfile {
  tenDangNhap: string
  hoTen: string
  maCoSo: string | null
  tenCoSo: string | null
  email: string | null
}

/** Khớp `ReportSummary` (`GET /api/reports/summary`) — rút gọn cho trang Thông tin. */
export interface TermReport {
  /** `null` = toàn hệ thống (Admin Master). */
  maCoSo: string | null
  tenPhamVi: string
  soLop: number
  luotDangKy: number
  soSinhVien: number
  tongSucChua: number
  tongDaDangKy: number
  soLopDay: number
  chuaCongBo: number
  daCongBo: number
  daKhoa: number
}
