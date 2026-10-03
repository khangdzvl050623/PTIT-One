/**
 * Hồ sơ hiển thị ở trang Thông tin.
 *
 * Trường có trong schema: `SinhVien`, `ChuongTrinhDaoTao`, `Khoa`, `CoSo`,
 * email của `TaiKhoan`. Nhóm trường "lý lịch" bên dưới (giới tính → hộ khẩu)
 * **chưa có cột trong DB** — UI dựng trước theo cổng gốc, cần nhóm chốt
 * migration trước khi nối API. Vì vậy tất cả đều có thể `null`.
 */
export interface StudentProfile {
  maSinhVien: string
  hoTen: string
  /** ISO `yyyy-mm-dd`; `null` khi hồ sơ chưa nhập. */
  ngaySinh: string | null
  email: string | null

  // --- Lý lịch: chưa có trong schema ---
  gioiTinh: 'NAM' | 'NU' | null
  dienThoai: string | null
  soCCCD: string | null
  /** Email cá nhân, ngoài email trường cấp. */
  email2: string | null
  noiSinh: string | null
  danToc: string | null
  tonGiao: string | null
  hoKhau: string | null

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
