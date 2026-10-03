/** Khớp `Term` của backend (`GET /api/terms`). Ngày dạng ISO `yyyy-mm-dd`. */
export interface Term {
  maHocKy: string
  tenHocKy: string
  ngayBatDau: string
  ngayKetThuc: string
}

/** `TRUC_TIEP` · `TRUC_TUYEN` · `KET_HOP` — CHECK trên `LopHocPhan`. */
export type HinhThucHoc = 'TRUC_TIEP' | 'TRUC_TUYEN' | 'KET_HOP'

/**
 * Một buổi học — khớp `TimetableEntry` của `GET /api/me/timetable`.
 * `thu`: 2 = thứ Hai … 8 = Chủ nhật. Giờ dạng `HH:mm:ss` lấy từ khung giờ tiết.
 */
export interface TimetableEntry {
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  tenGiangVien: string | null
  hinhThucHoc: HinhThucHoc
  thu: number
  tietBatDau: number
  soTiet: number
  phongHoc: string | null
  tuanBatDau: number
  tuanKetThuc: number
  gioBatDau: string
  gioKetThuc: string
  /**
   * Buổi dạy bù (thường chỉ một tuần: `tuanBatDau = tuanKetThuc`).
   * **Backend chưa có** — `LichHoc` không có cờ này; chỉ dữ liệu demo điền.
   */
  laDayBu?: boolean
}

/** Khớp `Timetable`: `tuan = null` nghĩa là cả học kỳ. */
export interface Timetable {
  maHocKy: string
  ngayBatDau: string
  tuan: number | null
  buoiHoc: readonly TimetableEntry[]
}

/** Một dòng `KhungGioTiet`. */
export interface Period {
  soTiet: number
  gioBatDau: string
  gioKetThuc: string
}
