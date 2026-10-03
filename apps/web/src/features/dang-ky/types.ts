/** Khớp `ClassSection` của `GET /api/classes?maHocKy=`. */
export interface ClassSection {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  maHocKy: string
  maCoSoHost: string
  maGiangVien: string | null
  tenGiangVien: string | null
  soLuongToiDa: number
  soLuongDaDangKy: number
  /** `DU_KIEN` · `MO` · `DA_KHOA` · `DA_HUY` */
  trangThai: string
  choPhepLienCoSo: boolean
  /** `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP` */
  hinhThucHoc: string
  phienBanLich: number
}

/** Khớp `ScheduleSlot` của `GET /api/classes/{maLopHP}/schedule`. */
export interface ScheduleSlot {
  thu: number
  tietBatDau: number
  soTiet: number
  phongHoc: string | null
  tuanBatDau: number
  tuanKetThuc: number
}

/** Lớp kèm lịch — ghép từ `/api/classes` và `/api/classes/{maLopHP}/schedule`. */
export interface ClassOffer extends ClassSection {
  lich: readonly ScheduleSlot[]
}

/** Khớp `EnrolledCourse`. `ngayDangKy` ISO-8601. */
export interface EnrolledCourse {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  /** Phần 1: `DA_DANG_KY`. `DANG_XU_LY`/`DANG_HUY` dành cho Phần 2. */
  trangThai: string
  ngayDangKy: string | null
}

/** Khớp `StudentEnrollments` của `GET /api/me/enrollments?maHocKy=`. */
export interface StudentEnrollments {
  maHocKy: string
  soTinChiDaDangKy: number
  /** `null` khi sinh viên chưa đăng ký gì trong kỳ. */
  tranTinChi: number | null
  dangKy: readonly EnrolledCourse[]
}

/** Khớp `RegistrationResult` của `POST /api/me/enrollments`. */
export interface RegistrationResult {
  dangKy: EnrolledCourse
  /** `HOC_MOI` · `HOC_LAI` (đã trượt) · `CAI_THIEN` (đã đạt). */
  loaiDangKy: 'HOC_MOI' | 'HOC_LAI' | 'CAI_THIEN'
}

/** Khớp `EnrollmentPeriod` của `GET /api/enrollment-periods`. */
export interface EnrollmentPeriod {
  maDot: string
  maHocKy: string
  maCoSo: string
  thoiGianMo: string
  thoiGianDong: string
  /** `CHUA_MO` · `DANG_MO` · `DA_DONG` */
  trangThai: string
}

/** Khớp `ProgramCourse` của `GET /api/programs/{maCTDT}`. */
export interface ProgramCourse {
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  /** Học kỳ thứ mấy trong lộ trình; `null` nếu không gợi ý. */
  hocKyGoiY: number | null
  batBuoc: boolean
}

/** CTĐT của sinh viên kèm môn — từ `GET /api/programs/{maCTDT}`. */
export interface StudentProgram {
  maCTDT: string
  tenCTDT: string
  /**
   * Sinh viên đang ở học kỳ thứ mấy của lộ trình (kỳ hè không tính).
   * **API chưa trả** — suy từ số học kỳ chính đã học; bản demo gán sẵn.
   */
  hocKyHienTai: number
  monHoc: readonly ProgramCourse[]
}

/** Khớp `CourseSummary` của `GET /api/courses` — có khoa quản lý môn. */
export interface CourseSummary {
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  maKhoa: string
  tenKhoa: string
}

/** Kết quả tốt nhất từng môn, suy từ `GET /api/me/grades` (chỉ điểm đã công bố). */
export type BestResults = Readonly<Record<string, 'DAT' | 'KHONG_DAT'>>

/**
 * Loại quan hệ giữa hai môn. **Backend chỉ có `TIEN_QUYET`** (bảng
 * `MonHocTienQuyet` không có cột loại); `HOC_TRUOC` và `SONG_HANH` để sẵn cho
 * khi nhóm thêm cột — hiện luôn rỗng.
 */
export type RelationKind = 'TIEN_QUYET' | 'HOC_TRUOC' | 'SONG_HANH'

/** Một dòng "môn đăng ký ← môn yêu cầu", phẳng hoá từ `GET /api/courses/{maMonHoc}`. */
export interface CourseRelation {
  loai: RelationKind
  maMonHoc: string
  tenMonHoc: string
  maMonYeuCau: string
  tenMonYeuCau: string
}

/** Một lần học một môn — rút gọn từ `StudentGrade` của `GET /api/me/grades`. */
export interface StudyRecord {
  maHocKy: string
  tenHocKy: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  /** `null` khi chưa công bố (đang học). */
  diemTongKet: number | null
  ketQua: 'DAT' | 'KHONG_DAT' | null
}
