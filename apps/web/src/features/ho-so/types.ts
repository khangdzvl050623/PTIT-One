/**
 * Phần hồ sơ **giống nhau giữa sinh viên và giảng viên**: lý lịch tự khai và
 * ảnh đại diện.
 *
 * Sinh viên lấy từ `SinhVien` (`V8`), giảng viên từ `GiangVien` (`V9`) — hai
 * bảng khác nhau nhưng đúng một bộ cột, nên hai bên dùng chung một biểu mẫu
 * sửa và một endpoint ảnh cùng hình dạng.
 *
 * Hồ sơ cũ chưa có dữ liệu nên tất cả đều `null` được.
 */
export interface PersonalProfile {
  hoTen: string
  gioiTinh: 'NAM' | 'NU' | null
  dienThoai: string | null
  soCCCD: string | null
  /** Email cá nhân, ngoài email trường cấp. */
  emailCaNhan: string | null
  noiSinh: string | null
  danToc: string | null
  tonGiao: string | null
  hoKhau: string | null
  /** URL ảnh trên Cloudinary; `null` thì hiển thị khung mặc định. */
  anhDaiDien: string | null
}

/**
 * Hồ sơ sinh viên ở trang Thông tin. Khớp `StudentDetail` của
 * `GET /api/me/profile`.
 */
export interface StudentProfile extends PersonalProfile {
  maSinhVien: string
  /** ISO `yyyy-mm-dd`; `null` khi hồ sơ chưa nhập. */
  ngaySinh: string | null
  email: string | null

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

/**
 * Thân của `PUT /api/me/profile` và `PUT /api/me/teacher-profile` — đúng tám ô
 * tự sửa được. Một kiểu cho cả hai vai vì hai endpoint nhận đúng cùng bộ ô.
 *
 * Chỉ có phần lý lịch: họ tên, ngày sinh, cơ sở, chương trình, học vị và trạng
 * thái do Phòng Đào tạo quản, server không nhận chúng ở đây.
 *
 * **Thay toàn bộ, không vá từng ô**: ô nào để `''` hoặc `null` thì giá trị cũ
 * bị XOÁ. Vì vậy biểu mẫu phải gửi lại cả tám ô, kể cả ô người dùng không sửa.
 *
 * Không có `anhDaiDien`: ảnh đi qua `POST /api/me/profile/avatar` (tải file) và
 * `DELETE` cùng đường. Nhận URL ở đây thì sinh viên trỏ ảnh sang địa chỉ bất kỳ
 * trên internet được, và cột `AnhDaiDien` sẽ có hai đường ghi.
 */
export interface UpdateMyProfileInput {
  gioiTinh: 'NAM' | 'NU' | null
  dienThoai: string | null
  soCCCD: string | null
  emailCaNhan: string | null
  noiSinh: string | null
  danToc: string | null
  tonGiao: string | null
  hoKhau: string | null
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

/**
 * Hồ sơ giảng viên ở trang Thông tin. Khớp `TeacherDetail` của
 * `GET /api/me/teacher-profile`.
 *
 * `hocVi`, `maKhoa`, `maCoSo` là dữ liệu hành chính — Phòng Đào tạo quản,
 * giảng viên không tự sửa.
 */
export interface TeacherProfile extends PersonalProfile {
  maGiangVien: string
  hocVi: string | null
  maKhoa: string
  tenKhoa: string
  maCoSo: string
  tenCoSo: string
  email: string | null
}

/** Một lớp GV phụ trách — rút từ `GET /api/me/teaching-classes`. */
/**
 * Một lớp GV phụ trách — đúng những trường `GET /api/me/teaching-classes` trả
 * về (`ClassSection`).
 *
 * KHÔNG có lịch học và KHÔNG có trạng thái bảng điểm: endpoint này không kèm
 * hai thứ đó. Trước đây kiểu này mang `lich` và `trangThaiDiem` theo dữ liệu
 * mẫu, nên màn hình hiện được những con số mà API thật không có.
 */
export interface TeachingClass {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  maHocKy: string
  soLuongDaDangKy: number
  soLuongToiDa: number
  /** `DU_KIEN` · `MO` · `DA_KHOA` · `DA_HUY` */
  trangThai: string
  /** `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP` */
  hinhThucHoc: string
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
