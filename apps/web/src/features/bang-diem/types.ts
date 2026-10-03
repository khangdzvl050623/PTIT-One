/**
 * Một dòng bảng điểm — khớp `StudentGrade` của `GET /api/me/grades`.
 *
 * Điểm chưa công bố: mọi cột điểm và `ketQua` là `null`, `daCongBo = false`.
 * **Không bao giờ đổi `null` thành 0** — hiển thị "Chưa có điểm".
 */
export interface StudentGrade {
  maHocKy: string
  tenHocKy: string
  maLopHP: string
  maMonHoc: string
  tenMonHoc: string
  soTinChi: number
  diemChuyenCan: number | null
  diemGiuaKy: number | null
  diemCuoiKy: number | null
  diemTongKet: number | null
  /** `DAT` · `KHONG_DAT` · `null` (chưa có điểm). */
  ketQua: 'DAT' | 'KHONG_DAT' | null
  daCongBo: boolean
  /** ISO-8601; `null` khi chưa công bố. */
  ngayCongBo: string | null
}

/** Số liệu một học kỳ, kèm luỹ kế tính tới hết học kỳ đó. */
export interface TermSummary {
  maHocKy: string
  tenHocKy: string
  grades: StudentGrade[]
  /** `null` khi học kỳ chưa có môn nào được công bố điểm. */
  tbHocKy10: number | null
  tbHocKy4: number | null
  tinChiDatHocKy: number
  tbTichLuy10: number | null
  tbTichLuy4: number | null
  tinChiTichLuy: number
  xepLoaiHocKy: string | null
}
