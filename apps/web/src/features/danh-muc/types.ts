/**
 * Kiểu của màn Hồ sơ và tài khoản (F02). Khớp **nguyên văn** record của
 * backend — xem `auth.model.AccountSummary`, `auth.model.ActivationCode`,
 * `student.model.*`, `teacher.model.*` và hai DTO `Create*Request`.
 */

/** Bốn vai trò, trùng enum `Role` của backend. */
export type LoaiNguoiDung = 'SINH_VIEN' | 'GIANG_VIEN' | 'ADMIN_CO_SO' | 'ADMIN_MASTER'

/** `HOAT_DONG` · `NGUNG` — chỉ hai giá trị này đổi được. */
export type TrangThaiTaiKhoan = 'HOAT_DONG' | 'NGUNG'

/**
 * Một dòng danh bạ — `GET /api/accounts?maCoSo=&loaiNguoiDung=`.
 * Không bao giờ mang hash mật khẩu.
 */
export interface AccountSummary {
  tenDangNhap: string
  loaiNguoiDung: LoaiNguoiDung
  /** `null` với Admin Master — Master không thuộc cơ sở nào. */
  maCoSo: string | null
  /** Mã SV/GV; `null` với tài khoản quản trị. */
  maThucThe: string | null
  trangThai: string
  /** Đã đặt mật khẩu bằng mã kích hoạt (hoặc tạo từ seed). */
  daKichHoat: boolean
}

/**
 * Mã kích hoạt vừa cấp. DB chỉ giữ hash nên **xem lại được là không có** —
 * mất thì cấp mã mới.
 */
export interface ActivationCode {
  tenDangNhap: string
  /**
   * Mã gốc dạng `XXXX-XXXX-XXXX-XXXX`, trả cho Admin Master **đúng một lần**.
   * `null` khi mã chỉ đi qua thư tới `guiToiEmail` — Admin không thấy mã, nên
   * kích hoạt được cũng chứng minh người dùng sở hữu email đó.
   */
  maKichHoat: string | null
  /** ISO-8601, hạn 7 ngày. */
  hetHan: string
  /** Địa chỉ nhận thư; `null` khi Admin tự trao mã. */
  guiToiEmail: string | null
}

/** Khớp `StudentProfile`. */
export interface StudentProfile {
  maSinhVien: string
  hoTen: string
  maCoSoNha: string
  maCTDT: string
  /** `DANG_HOC` · `BAO_LUU` · `THOI_HOC` · `TOT_NGHIEP` */
  trangThai: string
}

/** Khớp `Teacher`. */
export interface Teacher {
  maGiangVien: string
  hoTen: string
  maCoSo: string
  maKhoa: string
  hocVi: string | null
}

/** Khớp `ProvisionedStudent` — `POST /api/students`, trả `201`. */
export interface ProvisionedStudent {
  sinhVien: StudentProfile
  kichHoat: ActivationCode
}

/** Khớp `ProvisionedTeacher` — `POST /api/teachers`, trả `201`. */
export interface ProvisionedTeacher {
  giangVien: Teacher
  kichHoat: ActivationCode
}

/**
 * ⚠️ **CHỈ DEMO — API thật KHÔNG có trường này.**
 *
 * `CreateStudentRequest`/`CreateTeacherRequest` của backend chỉ nhận hồ sơ +
 * email; tài khoản sinh ra **cố ý chưa có mật khẩu** và chỉ đặt được qua
 * `POST /api/auth/activate` bằng mã kích hoạt. Không có endpoint nào cho Admin
 * đặt mật khẩu hộ.
 *
 * Nhóm chọn thêm ô mật khẩu ở giao diện để demo cho nhanh. Trước khi bật
 * `VITE_AUTH_MODE=api` phải làm MỘT trong hai: bỏ trường này khỏi form, hoặc
 * bổ sung `matKhauBanDau` vào DTO + service ở `apps/api` (đổi thiết kế F02,
 * cần nhóm quyết).
 *
 * Quy tắc 8–128 ký tự và không chứa tên đăng nhập lấy theo
 * `ActivateAccountRequest` để sau này khỏi lệch.
 */
export interface DemoInitialPassword {
  /** `null` nghĩa là dùng đúng luồng mã kích hoạt của hợp đồng. */
  matKhauBanDau: string | null
}

/** Thân `POST /api/students`. Tên đăng nhập **chính là** `maSinhVien`. */
export interface CreateStudentInput extends DemoInitialPassword {
  maSinhVien: string
  hoTen: string
  /** ISO `yyyy-mm-dd`; tuỳ chọn, phải ở quá khứ. */
  ngaySinh: string | null
  maCoSoNha: string
  maCTDT: string
  /** Tuỳ chọn. Có email thì mã kích hoạt **chỉ** đi qua thư. */
  email: string | null
}

/** Thân `POST /api/teachers`. Tên đăng nhập **chính là** `maGiangVien`. */
export interface CreateTeacherInput extends DemoInitialPassword {
  maGiangVien: string
  hoTen: string
  maCoSo: string
  maKhoa: string
  hocVi: string | null
  email: string | null
}

/**
 * Kết quả cấp hồ sơ **của bản giả**.
 *
 * API thật luôn trả `ProvisionedStudent`/`ProvisionedTeacher` với `kichHoat`
 * có giá trị — nhánh `matKhauBanDau` chỉ tồn tại ở `apps/web` (xem
 * {@link DemoInitialPassword}).
 */
export interface ProvisionResult {
  /** Mã hồ sơ vừa cấp, cũng là tên đăng nhập. */
  ma: string
  hoTen: string
  loai: 'SINH_VIEN' | 'GIANG_VIEN'
  /** Có giá trị khi Admin để trống mật khẩu — đúng luồng hợp đồng. */
  kichHoat: ActivationCode | null
  /** ⚠️ CHỈ DEMO: mật khẩu Admin đặt sẵn. `null` khi dùng mã kích hoạt. */
  matKhauBanDau: string | null
}

/** Khớp `AccountEmail` — email của một tài khoản, sau khi Admin đặt lại. */
export interface AccountEmailState {
  email: string | null
  /** Luôn `false` ngay sau khi Admin đặt: admin không xác minh hộ được. */
  daXacMinh: boolean
}

/** Khớp `Faculty` — `GET /api/faculties`. */
export interface Faculty {
  maKhoa: string
  tenKhoa: string
}

/** Khớp `StudyProgram` — `GET /api/programs`. */
export interface StudyProgram {
  maCTDT: string
  tenCTDT: string
  maKhoa: string
  tongTinChi: number
}

/**
 * Cơ sở. **Chưa có endpoint** `/api/campuses` — bảng `CoSo` hiện chỉ đọc gián
 * tiếp qua các màn khác, nên danh sách này lấy từ seed cho tới khi backend mở
 * đường tra cứu.
 */
export interface Campus {
  maCoSo: string
  tenCoSo: string
}
