import { diemChu, diemHe4, downloadCsv } from '@/shared/lib'

import type { TermSummary } from '../types'

const HEADER = [
  'Học kỳ',
  'Mã MH',
  'Lớp',
  'Tên môn học',
  'Số tín chỉ',
  'Chuyên cần',
  'Giữa kỳ',
  'Điểm thi',
  'Điểm TK (10)',
  'Điểm TK (4)',
  'Điểm TK (C)',
  'Kết quả',
]

/** Tải bảng điểm dạng CSV — mở trực tiếp bằng Excel. */
export function downloadGradesCsv(terms: readonly TermSummary[], fileName: string): void {
  const rows = terms.flatMap((t) =>
    t.grades.map((g) => [
      t.tenHocKy,
      g.maMonHoc,
      g.maLopHP,
      g.tenMonHoc,
      g.soTinChi,
      g.diemChuyenCan,
      g.diemGiuaKy,
      g.diemCuoiKy,
      g.diemTongKet,
      g.diemTongKet === null ? null : diemHe4(g.diemTongKet),
      g.diemTongKet === null ? null : diemChu(g.diemTongKet),
      g.ketQua === 'DAT' ? 'Đạt' : g.ketQua === 'KHONG_DAT' ? 'Không đạt' : 'Chưa có điểm',
    ]),
  )
  downloadCsv([HEADER, ...rows], fileName)
}
