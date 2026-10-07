/**
 * Kiểu của màn nhập điểm (F06). Mọi kiểu ở đây khớp **nguyên văn** record của
 * backend — xem `vn.ptit.one.grade.model` và `dto.SaveGradesRequest`.
 */

/** Khớp `ClassSection` — lớp kèm trong `GradeSheet.lop`. */
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

/**
 * Một lớp giảng viên phụ trách (F05). `GET /api/me/teaching-classes` trả
 * thẳng `List<ClassSection>` — **không** kèm lịch học. Muốn hiện lịch thì lấy
 * từ `GET /api/me/teaching-schedule`, nơi mỗi buổi có `maLopHP`.
 */
export type TeachingClass = ClassSection

/** Khớp `RosterEntry` của `GET /api/classes/{maLopHP}/students`. */
export interface RosterEntry {
  maSinhVien: string
  hoTen: string
  maCoSoNha: string
  ngayDangKy: string
  /** `DANG_XU_LY` · `DA_DANG_KY` · `DANG_HUY` — mọi ghi danh còn giữ chỗ. */
  trangThai: string
}

/**
 * Khớp `ClassRoster`: `lop.soLuongDaDangKy` là BỘ ĐẾM, `sinhVien.length` là số
 * dòng ghi danh — hai số phải bằng nhau, trả cả hai để đối soát.
 */
export interface ClassRoster {
  lop: ClassSection
  sinhVien: readonly RosterEntry[]
}

/**
 * Một buổi dạy — khớp `TimetableEntry`. `GET /api/me/teaching-schedule` có
 * **cùng hình dạng** với `GET /api/me/timetable` của sinh viên, nên màn lịch
 * tuần ở `features/lich-hoc` dùng lại được nguyên vẹn.
 *
 * `thu`: 2 = thứ Hai … 8 = Chủ nhật. Giờ dạng `HH:mm:ss`, server lấy sẵn từ
 * khung giờ tiết.
 */
export interface TeachingScheduleEntry {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  tenGiangVien: string | null
  hinhThucHoc: 'TRUC_TIEP' | 'TRUC_TUYEN' | 'KET_HOP'
  thu: number
  tietBatDau: number
  soTiet: number
  phongHoc: string | null
  tuanBatDau: number
  tuanKetThuc: number
  gioBatDau: string
  gioKetThuc: string
}

/** Khớp `Timetable`: `tuan = null` nghĩa là cả học kỳ. */
export interface TeachingSchedule {
  maHocKy: string
  ngayBatDau: string
  tuan: number | null
  buoiHoc: readonly TeachingScheduleEntry[]
}

/**
 * Khớp `GradeEntry` — một dòng trong bảng điểm của lớp.
 *
 * `diemTongKet` và `ketQua` do **server** tính (`0.1·CC + 0.3·GK + 0.6·CK`),
 * client không gửi lên; thiếu một điểm thành phần thì cả hai là `null`.
 */
export interface GradeEntry {
  maSinhVien: string
  hoTen: string
  diemChuyenCan: number | null
  diemGiuaKy: number | null
  diemCuoiKy: number | null
  diemTongKet: number | null
  ketQua: 'DAT' | 'KHONG_DAT' | null
  /**
   * Gửi lại **nguyên giá trị này** khi lưu. Lệch nghĩa là có người sửa dòng đó
   * sau khi mình tải bảng → `409 GRADE_VERSION_CONFLICT`.
   */
  version: number
  /** ISO-8601. `null` là điểm nháp — sinh viên chưa thấy. */
  ngayCongBo: string | null
}

/** Trạng thái bảng điểm, suy ra ở server (`GradeBookService.sheetOf`). */
export type GradeSheetStatus = 'NHAP' | 'DA_CONG_BO' | 'DA_KHOA'

/** Khớp `GradeSheet` — `GET /api/classes/{maLopHP}/grades`. */
export interface GradeSheet {
  lop: ClassSection
  trangThai: GradeSheetStatus
  diem: readonly GradeEntry[]
}

/**
 * Một dòng trong thân `PUT /api/classes/{maLopHP}/grades`.
 *
 * `null` ở một điểm thành phần là **xoá** điểm đó, không phải 0. Không có
 * `diemTongKet`: server tự tính.
 */
export interface SaveGradeRow {
  maSinhVien: string
  diemChuyenCan: number | null
  diemGiuaKy: number | null
  diemCuoiKy: number | null
  version: number
}

/** Ba điểm thành phần người dùng gõ được, dạng chuỗi trong ô nhập. */
export interface DraftScores {
  diemChuyenCan: string
  diemGiuaKy: string
  diemCuoiKy: string
}
