/** Khoảng điểm của `phanBoDiem` — đúng 5 khoảng theo API Contract. */
export const GRADE_BUCKETS = ['<4.0', '4.0–5.4', '5.5–6.9', '7.0–8.4', '≥8.5'] as const

/** Một dòng `CourseReport.Row` của `GET /api/reports/courses`. */
export interface CourseRow {
  maMonHoc: string
  tenMonHoc: string
  soLop: number
  luotDangKy: number
  tongSucChua: number
  tongDaDangKy: number
  /** 0–1; `null` khi môn không có lớp. */
  tiLeLapDay: number | null
  soDat: number
  soTruot: number
  /** Lượt chưa có điểm công bố — KHÔNG phải trượt. */
  chuaCoKetQua: number
  /** Số lượt theo từng khoảng của `GRADE_BUCKETS`, chỉ điểm đã công bố. */
  phanBoDiem: readonly number[]
}

/** Khớp `ReportSummary` của `GET /api/reports/summary`. */
export interface ReportSummary {
  phamVi: { maHocKy: string; maCoSo: string | null }
  dangKy: { soLop: number; luotDangKy: number; soSinhVien: number }
  sucChua: {
    tongSucChua: number
    tongDaDangKy: number
    /** 0–1; `null` khi không có lớp. */
    tiLeLapDay: number | null
    soLopDay: number
    soLopConCho: number
  }
  tienDoDiem: { chuaCongBo: number; daCongBo: number; daKhoa: number }
  capNhatLuc: string
}

export interface CourseReport {
  phamVi: { maHocKy: string; maCoSo: string | null }
  monHoc: readonly CourseRow[]
  capNhatLuc: string
}

export interface Campus {
  maCoSo: string
  tenCoSo: string
}

export interface ReportTerm {
  maHocKy: string
  tenHocKy: string
}
